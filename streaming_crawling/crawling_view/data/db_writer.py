"""
DB 저장 관련 함수들
"""
from django.db import transaction
from crawling_view.models import SongInfo, CrawlingData, PlatformType
from datetime import datetime
from crawling_view.utils.constants import CommonSettings, Platforms
import logging

logger = logging.getLogger(__name__)

def _validate_and_clean_data(data, platform, song_id):
    """
    크롤링 데이터 검증 및 정리 (무조건 저장)
    
    Args:
        data (dict): 크롤링 결과 데이터 (None이면 크롤링 실패로 간주)
        platform (str): 플랫폼명 (genie, youtube, youtube_music, melon)
        song_id (str): 곡 ID
        
    Returns:
        dict: 정리된 데이터 (오류 시에도 기본값으로 저장)
    """
    # song_id는 외부에서 전달받음 (무조건 있음)
    
    # data가 None이면 크롤링 실패로 간주하여 -999로 처리
    if data is None:
        logger.warning(f"❌ {platform} 크롤링 실패로 인한 -999 저장: song_id={song_id}")
        return {
            'song_id': song_id,
            'views': -999,
            'listeners': -999
        }
    
    # views 처리 (오류 시 -999, 제공 안 하면 -1)
    views = _process_numeric_field(data.get('views') if data else None, '조회수', platform, song_id)
    
    # listeners 처리 (오류 시 -999, 제공 안 하면 -1)
    listeners = _process_numeric_field(data.get('listeners') if data else None, '청취자 수', platform, song_id)
    
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
            - 0: 실제로 0인 경우 또는 멜론의 빈 값
            - -1: 해당 플랫폼에서 제공하지 않는 데이터
            - -999: 크롤링 실패/오류
    """
    # None이거나 'None' 문자열인 경우 -1 (제공하지 않는 데이터)
    if value is None or value == 'None':
        logger.info(f"ℹ️ {platform} {field_name} 제공하지 않음: song_id={song_id}")
        return -1
    
    try:
        # 이미 정수인 경우 그대로 반환
        if isinstance(value, int):
            return value
        
        # 문자열인 경우 변환
        if isinstance(value, str):
            # 멜론의 경우 빈 문자열을 0으로 처리
            if platform == 'melon' and (not value or value == ""):
                logger.info(f"ℹ️ {platform} {field_name} 빈 값 발견, 0으로 처리: song_id={song_id}")
                return 0
            
            # 다른 플랫폼의 빈 문자열이나 'None' 문자열
            if not value or value.lower() == 'none':
                logger.info(f"ℹ️ {platform} {field_name} 제공하지 않음: song_id={song_id}")
                return -1
            
            # 쉼표 제거 후 변환
            clean_value = value.replace(',', '')
            return int(clean_value)
        
        # 기타 타입은 0으로 처리
        return int(value) if value else -1
        
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
            artist_name = kwargs.get('artist_ko')
            song_name = kwargs.get('title_ko')
            if not artist_name or not song_name:
                logger.warning(f"❌ Genie artist_ko 또는 title_ko 누락")
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
            artist_name = kwargs.get('artist_ko')
            song_name = kwargs.get('title_ko')
            if not artist_name or not song_name:
                logger.warning(f"❌ YouTube Music artist_ko 또는 title_ko 누락")
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

def _save_crawling_data(results, platform, platform_type, song_ids=None):
    """
    크롤링 데이터 저장 공통 함수 (오늘 날짜 데이터 업데이트)
    
    Args:
        results (list/dict): 크롤링 결과
        platform (str): 플랫폼명 (로그용)
        platform_type: PlatformType enum 값
        song_ids (list): 처리할 곡 ID 리스트 (None이면 results에서 추출)
        
    Returns:
        dict: 저장 결과 (saved_count, failed_count, skipped_count, updated_count)
    """
    saved_count = 0
    failed_count = 0
    skipped_count = 0
    updated_count = 0
    
    # 처리할 song_id 목록 결정
    target_song_ids = set()
    
    if song_ids:
        # 외부에서 전달받은 song_ids 사용
        target_song_ids = set(song_ids)
    elif results:
        # results에서 song_id 추출
        if isinstance(results, dict):
            target_song_ids = set(results.keys())
        else:
            # list 형태인 경우 song_id 추출
            for result in results:
                if isinstance(result, dict) and result.get('song_id'):
                    target_song_ids.add(result.get('song_id'))
    
    if not target_song_ids:
        logger.warning(f"⚠️ {platform} 처리할 song_id가 없음")
        return {'saved_count': 0, 'failed_count': 0, 'skipped_count': 0, 'updated_count': 0}
    
    # 각 song_id에 대해 데이터 저장 (무조건 저장)
    for song_id in target_song_ids:
        try:
            # 해당 song_id의 크롤링 결과 찾기
            result_data = None
            if results:
                if isinstance(results, dict):
                    result_data = results.get(song_id)
                else:
                    # list에서 해당 song_id 찾기
                    for result in results:
                        if isinstance(result, dict) and result.get('song_id') == song_id:
                            result_data = result
                            break
            
            # 데이터 검증 및 정리 (무조건 저장)
            # results가 None이거나 빈 컨테이너이면 크롤링 실패로 간주하여 -999로 처리
            if results is None or (isinstance(results, (list, dict)) and len(results) == 0):
                logger.warning(f"⚠️ {platform} 크롤링 결과 없음, -999로 저장: song_id={song_id}")
                clean_data = _validate_and_clean_data(None, platform, song_id)
            else:
                logger.info(f"ℹ️ {platform} 크롤링 결과 있음, 정상 저장: song_id={song_id}")
                clean_data = _validate_and_clean_data(result_data, platform, song_id)
            
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
                crawling_data = CrawlingData.objects.create(
                    song_id=clean_data['song_id'],
                    views=clean_data['views'],
                    listeners=clean_data['listeners'],
                    platform=platform_type
                )
                
                logger.info(f"💾 {platform} DB 저장 완료: song_id={song_id}, views={clean_data['views']}, listeners={clean_data['listeners']}")
                
                if deleted_count > 0:
                    updated_count += 1
                    logger.info(f"✅ {platform} 데이터 교체(업데이트): song_id={song_id} (기존 {deleted_count}개 삭제 후 새로 저장)")
                else:
                    saved_count += 1
                    logger.info(f"✅ {platform} 새 데이터 생성: song_id={song_id}")
                
            except Exception as e:
                failed_count += 1
                logger.error(f"❌ {platform} DB 저장/업데이트 실패: song_id={song_id} - {e}")
            
        except Exception as e:
            failed_count += 1
            logger.error(f"❌ {platform} DB 저장 실패: song_id={song_id} - {e}")
    
    logger.info(f"✅ {platform} DB 저장 완료: {saved_count}개 생성, {updated_count}개 교체, {failed_count}개 실패, {skipped_count}개 스킵")
    return {
        'saved_count': saved_count, 
        'failed_count': failed_count, 
        'skipped_count': skipped_count, 
        'updated_count': updated_count
    }

def save_genie_to_db(results, song_ids=None):
    """
    Genie 크롤링 결과를 DB에 저장
    
    Args:
        results (list): 크롤링 결과 리스트
        song_ids (list): 처리할 곡 ID 리스트 (None이면 results에서 추출)
        
    Returns:
        dict: 저장 결과 (saved_count, failed_count, skipped_count, updated_count)
    """
    return _save_crawling_data(results, 'genie', PlatformType.GENIE, song_ids)

def save_youtube_music_to_db(results, song_ids=None):
    """
    YouTube Music 크롤링 결과를 DB에 저장
    
    Args:
        results (list): 크롤링 결과 리스트
        song_ids (list): 처리할 곡 ID 리스트 (None이면 results에서 추출)
        
    Returns:
        dict: 저장 결과 (saved_count, failed_count, skipped_count, updated_count)
    """
    return _save_crawling_data(results, 'youtube_music', PlatformType.YOUTUBE_MUSIC, song_ids)

def save_youtube_to_db(results, song_ids=None):
    """
    YouTube 크롤링 결과를 DB에 저장
    
    Args:
        results (dict): 크롤링 결과 딕셔너리
        song_ids (list): 처리할 곡 ID 리스트 (None이면 results에서 추출)
        
    Returns:
        dict: 저장 결과 (saved_count, failed_count, skipped_count, updated_count)
    """
    return _save_crawling_data(results, 'youtube', PlatformType.YOUTUBE, song_ids)

def save_melon_to_db(results, song_ids=None):
    """
    Melon 크롤링 결과를 DB에 저장
    
    Args:
        results (list): 크롤링 결과 리스트
        song_ids (list): 처리할 곡 ID 리스트 (None이면 results에서 추출)
        
    Returns:
        dict: 저장 결과 (saved_count, failed_count, skipped_count, updated_count)
    """
    return _save_crawling_data(results, 'melon', PlatformType.MELON, song_ids)

def save_all_platforms_for_songs(song_ids, genie_results=None, youtube_music_results=None, youtube_results=None, melon_results=None):
    """
    모든 곡에 대해 4개 플랫폼의 데이터를 무조건 저장
    
    Args:
        song_ids (list): 처리할 곡 ID 리스트
        genie_results (list): Genie 크롤링 결과 (None이면 빈 데이터로 저장)
        youtube_music_results (list): YouTube Music 크롤링 결과 (None이면 빈 데이터로 저장)
        youtube_results (dict): YouTube 크롤링 결과 (None이면 빈 데이터로 저장)
        melon_results (list): Melon 크롤링 결과 (None이면 빈 데이터로 저장)
        
    Returns:
        dict: 전체 저장 결과
    """
    if not song_ids:
        logger.warning("⚠️ 처리할 song_ids가 없음")
        return {}
    
    logger.info(f"🎯 {len(song_ids)}개 곡에 대해 4개 플랫폼 데이터 저장 시작")
    
    # 각 플랫폼별 저장 (결과가 없어도 무조건 저장)
    genie_result = save_genie_to_db(genie_results, song_ids)
    youtube_music_result = save_youtube_music_to_db(youtube_music_results, song_ids)
    youtube_result = save_youtube_to_db(youtube_results, song_ids)
    melon_result = save_melon_to_db(melon_results, song_ids)
    
    # 전체 결과 집계
    total_result = {
        'total_songs': len(song_ids),
        'genie': genie_result,
        'youtube_music': youtube_music_result,
        'youtube': youtube_result,
        'melon': melon_result,
        'total_saved': genie_result['saved_count'] + youtube_music_result['saved_count'] + youtube_result['saved_count'] + melon_result['saved_count'],
        'total_updated': genie_result['updated_count'] + youtube_music_result['updated_count'] + youtube_result['updated_count'] + melon_result['updated_count'],
        'total_failed': genie_result['failed_count'] + youtube_music_result['failed_count'] + youtube_result['failed_count'] + melon_result['failed_count']
    }
    
    logger.info(f"✅ 전체 플랫폼 저장 완료: {total_result['total_saved']}개 생성, {total_result['total_updated']}개 교체, {total_result['total_failed']}개 실패")
    return total_result 