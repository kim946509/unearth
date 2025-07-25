"""
단일 곡 크롤링/저장 매니저
"""
import logging
from crawling.utils.constants import Platforms
from crawling.managers.crawling_manager import create_crawler
from crawling.repository.db_writer import (
    save_all_platforms_for_songs
)
from crawling.utils.single_crawling_logger import create_summary_logger
from crawling.repository.failure_service import FailureService
from crawling.view.melon.melon_song_id_logic import MelonSongIdFinder
from crawling.repository.melon_song_id_db_writer import save_melon_song_id_to_db, get_song_info_for_melon_search
from crawling.utils.driver import setup_driver

logger = logging.getLogger(__name__)

def run_single_song_crawling(song_dict, save_csv=True, save_db=True, platform=None):
    """
    단일 곡 크롤링 및 저장 (여러 곡과 동일한 로직, 곡 리스트만 1개)
    Args:
        song_dict (dict): {'song_id', 'title_ko', 'artist_ko', 'title_en', 'artist_en'}
        save_csv (bool): CSV 저장 여부
        save_db (bool): DB 저장 여부
        platform (str or None): 특정 플랫폼만 실행 (None이면 전체)
    Returns:
        dict: 결과 요약
    """
    logger.info("🚀 단일 곡 크롤링 프로세스 시작")
    logger.info(f"🎵 곡 정보: {song_dict['artist_ko']} - {song_dict['title_ko']} (ID: {song_dict['song_id']})")

    # 결과 요약 로거 생성
    summary_logger = create_summary_logger(song_dict)
    platforms_to_run = [platform] if platform else Platforms.ALL_PLATFORMS

    # 크롤링 결과 저장용 변수들 (초기값은 빈 컨테이너)
    genie_results = []
    youtube_music_results = []
    youtube_results = {}
    melon_results = []

    for plat in platforms_to_run:
        try:
            logger.info(f"🔍 {plat.upper()} 크롤링 시작")
            
            if plat == Platforms.GENIE:
                # Genie용 데이터 형식 (한글/영문 제목과 아티스트명 모두 포함)
                genie_data = [{
                    'song_id': song_dict['song_id'],
                    'title_ko': song_dict['title_ko'],
                    'title_en': song_dict.get('title_en', ''),
                    'artist_ko': song_dict['artist_ko'],
                    'artist_en': song_dict['artist_en']
                }]
                
                genie_crawler = create_crawler('genie')
                genie_results = genie_crawler.crawl_songs(genie_data)
                
                # 성공 여부 확인 및 결과 추가
                if genie_results and len(genie_results) > 0:
                    summary_logger.add_platform_result('genie', 'success', genie_results)
                else:
                    summary_logger.add_platform_result('genie', 'failed')
                    genie_results = None  # 실패 시 None으로 설정 (DB에서 -999로 처리)
                    
            elif plat == Platforms.YOUTUBE_MUSIC:
                # YouTube Music용 데이터 형식 (한글/영문 제목과 아티스트명 모두 포함)
                ytmusic_data = [{
                    'song_id': song_dict['song_id'],
                    'title_ko': song_dict['title_ko'],
                    'title_en': song_dict.get('title_en', ''),
                    'artist_ko': song_dict['artist_ko'],
                    'artist_en': song_dict['artist_en']
                }]
                
                ytmusic_crawler = create_crawler('youtube_music')
                ytmusic_results = ytmusic_crawler.crawl_songs(ytmusic_data)
                
                # 성공 여부 확인 및 결과 추가
                if ytmusic_results and len(ytmusic_results) > 0:
                    summary_logger.add_platform_result('youtube_music', 'success', ytmusic_results)
                    youtube_music_results = ytmusic_results  # 성공 시 결과 할당
                else:
                    summary_logger.add_platform_result('youtube_music', 'failed')
                    youtube_music_results = None  # 실패 시 None으로 설정 (DB에서 -999로 처리)
                    
            elif plat == Platforms.YOUTUBE:
                # YouTube는 song_dict에서 직접 URL 정보 사용
                youtube_url = song_dict.get('youtube_url')
                
                if youtube_url:
                    youtube_data = [(youtube_url, song_dict['artist_ko'], song_dict['song_id'])]
                    
                    youtube_crawler = create_crawler('youtube')
                    youtube_results = youtube_crawler.crawl_songs(youtube_data)
                    
                    # 성공 여부 확인 및 결과 추가 (딕셔너리 구조에 맞게 판정)
                    if youtube_results and any(
                        v and isinstance(v, dict) and v.get('song_name') not in (None, '', '제목 없음')
                        for v in youtube_results.values()
                    ):
                        summary_logger.add_platform_result('youtube', 'success', youtube_results)
                    else:
                        summary_logger.add_platform_result('youtube', 'failed')
                        youtube_results = None  # 실패 시 None으로 설정 (DB에서 -999로 처리)
                else:
                    summary_logger.add_platform_result('youtube', 'skipped')
                    logger.warning(f"⚠️ YouTube URL이 비어있어 건너뜀")
                    
            elif plat == Platforms.MELON:
                # Melon은 song_dict에서 직접 melon_song_id 사용
                melon_song_id = song_dict.get('melon_song_id')
                
                # melon_song_id가 없으면 자동으로 찾아서 저장 (null, 빈 문자열, 공백 모두 체크)
                if not melon_song_id or melon_song_id.strip() == "":
                    logger.info(f"🍈 Melon song_id가 없어 자동 검색 시작: {song_dict['artist_ko']} - {song_dict['title_ko']}")
                    
                    try:
                        # 드라이버 설정하여 멜론 곡 ID 검색
                        with setup_driver() as driver:
                            melon_finder = MelonSongIdFinder(driver)
                            found_melon_song_id = melon_finder.find_melon_song_id(song_dict)
                            
                            if found_melon_song_id:
                                # DB에 저장
                                if save_melon_song_id_to_db(song_dict['song_id'], found_melon_song_id):
                                    melon_song_id = found_melon_song_id
                                    song_dict['melon_song_id'] = found_melon_song_id  # song_dict도 업데이트
                                    logger.info(f"✅ Melon song_id 자동 검색 및 저장 완료: {found_melon_song_id}")
                                else:
                                    logger.error(f"❌ Melon song_id DB 저장 실패: {found_melon_song_id}")
                            else:
                                logger.warning(f"❌ Melon song_id 자동 검색 실패")
                                
                    except Exception as e:
                        logger.error(f"❌ Melon song_id 자동 검색 중 오류: {e}", exc_info=True)
                
                # melon_song_id가 있으면 크롤링 진행
                if melon_song_id:
                    melon_data = [{
                        'song_id': song_dict['song_id'],
                        'melon_song_id': melon_song_id
                    }]
                    
                    melon_crawler = create_crawler('melon')
                    melon_results = melon_crawler.crawl_songs(melon_data)
                    
                    # 성공 여부 확인 및 결과 추가
                    if melon_results and len(melon_results) > 0:
                        summary_logger.add_platform_result('melon', 'success', melon_results)
                    else:
                        summary_logger.add_platform_result('melon', 'failed')
                        melon_results = None  # 실패 시 None으로 설정 (DB에서 -999로 처리)
                else:
                    # 멜론 song_id를 찾지 못한 경우는 'failed'로 처리 (크롤링 시도했지만 실패)
                    summary_logger.add_platform_result('melon', 'failed')
                    logger.warning(f"❌ Melon song_id를 찾을 수 없어 크롤링 실패")
                    melon_results = None  # 실패 시 None으로 설정 (DB에서 -999로 처리)
                    
        except Exception as e:
            logger.error(f"❌ {plat.upper()} 크롤링 중 오류: {str(e)}")
            summary_logger.add_platform_result(plat, 'error')

    # 모든 크롤링 완료 후 DB 저장 (무조건 저장)
    if save_db:
        logger.info("💾 모든 플랫폼 데이터 DB 저장 시작")
        db_result = save_all_platforms_for_songs(
            song_ids=[song_dict['song_id']],
            genie_results=genie_results,
            youtube_music_results=youtube_music_results,
            youtube_results=youtube_results,
            melon_results=melon_results
        )
        
        # DB 저장 결과 로깅
        for platform in ['genie', 'youtube_music', 'youtube', 'melon']:
            if platform in db_result:
                platform_result = db_result[platform]
                summary_logger.add_db_result(platform, platform_result)
    
    # 요약 정보 생성 및 출력
    summary = summary_logger.generate_summary()
    summary_logger.print_summary()
    
    # 실패 처리 - DB에 저장된 -999 값을 확인하여 실패 처리
    if save_db:
        logger.info(f"🔍 실패 곡 목록 확인 및 업데이트: {song_dict['song_id']}")
        FailureService.check_and_handle_failures(song_dict['song_id'])
        
        # 추가 실패 로깅 (디버깅용)
        failed_platforms = []
        for platform, status in summary_logger.platform_status.items():
            if status in ['failed', 'error']:
                failed_platforms.append(platform.upper())
        
        if failed_platforms:
            logger.warning(f"❌ 실패한 플랫폼들: {', '.join(failed_platforms)}")
        else:
            logger.info(f"✅ 모든 플랫폼 성공")
    
    return summary 