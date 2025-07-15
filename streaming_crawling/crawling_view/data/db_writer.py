"""
DB 저장 관련 함수들
"""
from django.db import transaction
from crawling_view.models import SongInfo, CrawlingData, PlatformType
from datetime import datetime
from crawling_view.utils.constants import CommonSettings, Platforms
import logging

logger = logging.getLogger(__name__)

def _validate_and_clean_data(data, platform):
    """
    크롤링 데이터 검증 및 정리
    
    Args:
        data (dict): 크롤링 결과 데이터
        platform (str): 플랫폼명 (genie, youtube, youtube_music)
        
    Returns:
        dict: 정리된 데이터 또는 None (데이터가 유효하지 않은 경우)
    """
    if not data:
        logger.error(f"❌ {platform} 데이터가 비어있음")
        return None
        
    # song_id 필수 검증 (절대 누락되면 안 됨)
    song_id = data.get('song_id')
    if not song_id:
        logger.error(f"❌ {platform} song_id 누락 - 저장 차단: {data}")
        return None
    
    # views 처리 (필수 데이터)
    views = _process_numeric_field(data.get('views'), '조회수', platform, song_id)
    
    # listeners 처리 (필수 데이터)
    listeners = _process_numeric_field(data.get('listeners'), '청취자 수', platform, song_id)
    
    return {
        'song_id': song_id,
        'views': views,
        'listeners': listeners
    }

def _process_numeric_field(value, field_name, platform, song_id):
    """
    숫자 필드 처리 (views, listeners)
    
    Args:
        value: 원본 값
        field_name (str): 필드명 (조회수, 청취자 수)
        platform (str): 플랫폼명
        song_id (str): 곡 ID
        
    Returns:
        int: 처리된 값
            - 정상값: 양수
            - 0: 실제로 0인 경우
            - -1: 해당 플랫폼에서 제공하지 않는 데이터
            - -999: 크롤링 실패/오류
    """
    if value is None or value == 'None':
        logger.error(f"❌ {platform} {field_name} 데이터 누락: song_id={song_id}")
        return -999
    
    try:
        # 이미 정수인 경우 그대로 반환
        if isinstance(value, int):
            return value
        
        # 문자열인 경우 변환
        if isinstance(value, str):
            # 빈 문자열이나 'None' 문자열
            if not value or value.lower() == 'none':
                return -999
            
            # 쉼표 제거 후 변환
            clean_value = value.replace(',', '')
            return int(clean_value)
        
        # 기타 타입은 0으로 처리
        return int(value) if value else -999
        
    except (ValueError, TypeError):
        logger.error(f"❌ {platform} {field_name} 변환 실패: song_id={song_id}, 원래값={value} (type: {type(value)})")
        return -999

def get_song_info_id(platform, **kwargs):
    """
    SongInfo 테이블에서 플랫폼별 정보로 id 조회
    
    Args:
        platform (str): 플랫폼명 ('genie', 'youtube', 'youtube_music')
        **kwargs: 플랫폼별 조회 조건
            - genie: artist_name, song_name
            - youtube: url
            - youtube_music: artist_name, song_name
        
    Returns:
        str: SongInfo의 id 또는 None
    """
    try:
        if platform == Platforms.GENIE:
            # Genie는 artist와 title로 조회
            artist_name = kwargs.get('artist_name')
            song_name = kwargs.get('song_name')
            if not artist_name or not song_name:
                logger.warning(f"❌ Genie artist_name 또는 song_name 누락")
                return None
            
            song_info = SongInfo.objects.get(artist_ko=artist_name, title_ko=song_name)
            # SongInfo 조회 성공은 디버그 레벨로 변경
            pass
            
        elif platform == Platforms.YOUTUBE:
            # YouTube는 URL로만 조회
            url = kwargs.get('url')
            if not url:
                logger.warning(f"❌ YouTube URL 누락")
                return None
            
            song_info = SongInfo.objects.get(youtube_url=url)
            # SongInfo 조회 성공은 디버그 레벨로 변경
            pass
                
        elif platform == Platforms.YOUTUBE_MUSIC:
            # YouTube Music은 artist와 title로 조회
            artist_name = kwargs.get('artist_name')
            song_name = kwargs.get('song_name')
            if not artist_name or not song_name:
                logger.warning(f"❌ YouTube Music artist_name 또는 song_name 누락")
                return None
            
            song_info = SongInfo.objects.get(artist_ko=artist_name, title_ko=song_name)
            # SongInfo 조회 성공은 디버그 레벨로 변경
            pass
            
        else:
            logger.warning(f"❌ 지원하지 않는 플랫폼: {platform}")
            return None
        
        return song_info.id
        
    except SongInfo.DoesNotExist:
        logger.warning(f"❌ SongInfo 찾을 수 없음: {platform} - {kwargs}")
        return None
    except Exception as e:
        logger.error(f"❌ SongInfo 조회 실패: {platform} - {kwargs} - {e}")
        return None

def _save_crawling_data(results, platform, platform_type):
    """
    크롤링 데이터 저장 공통 함수 (오늘 날짜 데이터 업데이트)
    
    Args:
        results (list/dict): 크롤링 결과
        platform (str): 플랫폼명 (로그용)
        platform_type: PlatformType enum 값
        
    Returns:
        dict: 저장 결과 (saved_count, failed_count, skipped_count, updated_count)
    """
    if not results:
        logger.warning(f"⚠️ {platform} 크롤링 결과가 없음")
        return {'saved_count': 0, 'failed_count': 0, 'skipped_count': 0, 'updated_count': 0}
    
    saved_count = 0
    failed_count = 0
    skipped_count = 0
    updated_count = 0
    
    # YouTube는 dict 형태, 나머지는 list 형태
    items = results.items() if isinstance(results, dict) else enumerate(results)
    
    for key, result in items:
        try:
            # song_id 추출 및 검증 (절대 누락되면 안 됨)
            if isinstance(result, dict):
                song_id = result.get('song_id')
            else:
                song_id = key
            
            if not song_id:
                logger.error(f"❌ {platform} song_id 누락 - 저장 차단: {result}")
                skipped_count += 1
                continue
            
            # 데이터 검증 및 정리
            clean_data = _validate_and_clean_data(result, platform)
            if not clean_data:
                skipped_count += 1
                continue
            
            # 오늘 날짜의 기존 데이터 확인
            from datetime import date
            today = date.today()
            
            try:
                # 같은 song_id, platform, 오늘 날짜(일 단위)의 기존 데이터 삭제
                deleted_count = CrawlingData.objects.filter(
                    song_id=clean_data['song_id'],
                    platform=platform_type,
                    created_at__date=today  # 반드시 date만 비교
                ).delete()[0]
                
                # 새 데이터 생성
                CrawlingData.objects.create(
                    song_id=clean_data['song_id'],
                    views=clean_data['views'],
                    listeners=clean_data['listeners'],
                    platform=platform_type
                )
                
                if deleted_count > 0:
                    updated_count += 1
                    logger.info(f"✅ {platform} 데이터 교체(업데이트): song_id={song_id} (기존 {deleted_count}개 삭제 후 새로 저장)")
                else:
                    saved_count += 1
                    logger.info(f"✅ {platform} 새 데이터 생성: song_id={song_id}")
                
            except Exception as e:
                failed_count += 1
                logger.error(f"❌ {platform} DB 저장/업데이트 실패: {result} - {e}")
            
        except Exception as e:
            failed_count += 1
            logger.error(f"❌ {platform} DB 저장 실패: {result} - {e}")
    
    logger.info(f"✅ {platform} DB 저장 완료: {saved_count}개 생성, {updated_count}개 교체, {failed_count}개 실패, {skipped_count}개 스킵")
    return {'saved_count': saved_count, 'failed_count': failed_count, 'skipped_count': skipped_count, 'updated_count': updated_count}

def save_genie_to_db(results):
    """
    Genie 크롤링 결과를 DB에 저장
    
    Args:
        results (list): 크롤링 결과 리스트
        
    Returns:
        dict: 저장 결과 (saved_count, failed_count, skipped_count)
    """
    return _save_crawling_data(results, 'genie', PlatformType.GENIE)

def save_youtube_music_to_db(results):
    """
    YouTube Music 크롤링 결과를 DB에 저장
    
    Args:
        results (list): 크롤링 결과 리스트
        
    Returns:
        dict: 저장 결과 (saved_count, failed_count, skipped_count)
    """
    return _save_crawling_data(results, 'youtube_music', PlatformType.YOUTUBE_MUSIC)

def save_youtube_to_db(results):
    """
    YouTube 크롤링 결과를 DB에 저장
    
    Args:
        results (dict): 크롤링 결과 딕셔너리
        
    Returns:
        dict: 저장 결과 (saved_count, failed_count, skipped_count)
    """
    return _save_crawling_data(results, 'youtube', PlatformType.YOUTUBE)

def save_melon_to_db(results):
    """
    Melon 크롤링 결과를 DB에 저장
    
    Args:
        results (list): 크롤링 결과 리스트
        
    Returns:
        dict: 저장 결과 (saved_count, failed_count, skipped_count)
    """
    return _save_crawling_data(results, 'melon', PlatformType.MELON) 