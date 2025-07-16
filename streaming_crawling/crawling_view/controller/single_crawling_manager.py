"""
단일 곡 크롤링/저장 매니저
"""
import logging
from datetime import date
from crawling_view.data.song_service import SongService
from crawling_view.utils.constants import Platforms
from crawling_view.controller.crawling_manager import create_crawler
from crawling_view.data.db_writer import (
    save_genie_to_db, save_youtube_music_to_db, save_youtube_to_db, save_melon_to_db
)
from crawling_view.data.csv_writer import (
    save_genie_csv, save_youtube_music_csv, save_youtube_csv, save_melon_csv
)

logger = logging.getLogger(__name__)

def run_single_song_crawling(song_dict, save_csv=True, save_db=True, platform=None):
    """
    단일 곡 크롤링 및 저장 (여러 곡과 동일한 로직, 곡 리스트만 1개)
    Args:
        song_dict (dict): {'song_id', 'song_title', 'artist_name', 'title_en', 'artist_en'}
        save_csv (bool): CSV 저장 여부
        save_db (bool): DB 저장 여부
        platform (str or None): 특정 플랫폼만 실행 (None이면 전체)
    Returns:
        dict: 결과 요약
    """
    import time
    start_time = time.time()
    
    logger.info("🚀 단일 곡 크롤링 프로세스 시작")
    logger.info(f"🎵 곡 정보: {song_dict['artist_name']} - {song_dict['song_title']} (ID: {song_dict['song_id']})")

    crawling_results = {}
    db_results = {}
    csv_results = {}
    platform_status = {}  # 플랫폼별 성공/실패 상태

    platforms_to_run = [platform] if platform else Platforms.ALL_PLATFORMS

    for plat in platforms_to_run:
        try:
            logger.info(f"🔍 {plat.upper()} 크롤링 시작")
            
            if plat == Platforms.GENIE:
                # Genie용 데이터 형식
                genie_data = [{
                    'song_id': song_dict['song_id'],
                    'song_title': song_dict['song_title'],
                    'artist_name': song_dict['artist_name']
                }]
                
                genie_crawler = create_crawler('genie')
                genie_results = genie_crawler.crawl_songs(genie_data)
                crawling_results['genie'] = genie_results
                
                # 성공 여부 확인
                if genie_results and len(genie_results) > 0 and genie_results[0]:
                    platform_status['genie'] = 'success'
                    if save_db:
                        db_results['genie'] = save_genie_to_db(genie_results)
                    if save_csv:
                        csv_results['genie'] = save_genie_csv(genie_results)
                else:
                    platform_status['genie'] = 'failed'
                    
            elif plat == Platforms.YOUTUBE_MUSIC:
                # YouTube Music용 데이터 형식
                ytmusic_data = [{
                    'song_id': song_dict['song_id'],
                    'song_title': song_dict['song_title'],
                    'artist_name': song_dict['artist_name']
                }]
                
                ytmusic_crawler = create_crawler('youtube_music')
                ytmusic_results = ytmusic_crawler.crawl_songs(ytmusic_data)
                crawling_results['youtube_music'] = ytmusic_results
                
                # 성공 여부 확인
                if ytmusic_results and len(ytmusic_results) > 0 and ytmusic_results[0]:
                    platform_status['youtube_music'] = 'success'
                    if save_db:
                        db_results['youtube_music'] = save_youtube_music_to_db(ytmusic_results)
                    if save_csv:
                        csv_results['youtube_music'] = save_youtube_music_csv(ytmusic_results)
                else:
                    platform_status['youtube_music'] = 'failed'
                    
            elif plat == Platforms.YOUTUBE:
                # YouTube는 song_dict에서 직접 URL 정보 사용
                youtube_url = song_dict.get('youtube_url')
                
                if youtube_url:
                    youtube_data = [(youtube_url, song_dict['artist_name'], song_dict['song_id'])]
                    
                    youtube_crawler = create_crawler('youtube')
                    youtube_results = youtube_crawler.crawl_songs(youtube_data)
                    crawling_results['youtube'] = youtube_results
                    
                    # 성공 여부 확인
                    if youtube_results and len(youtube_results) > 0 and youtube_results[0]:
                        platform_status['youtube'] = 'success'
                        if save_db:
                            db_results['youtube'] = save_youtube_to_db(youtube_results)
                        if save_csv:
                            csv_results['youtube'] = save_youtube_csv(youtube_results)
                    else:
                        platform_status['youtube'] = 'failed'
                else:
                    platform_status['youtube'] = 'skipped'  # URL 없음
                    logger.warning(f"⚠️ YouTube URL이 비어있어 건너뜀")
                    
            elif plat == Platforms.MELON:
                # Melon은 song_dict에서 직접 melon_song_id 사용
                melon_song_id = song_dict.get('melon_song_id')
                
                if melon_song_id:
                    melon_data = [{
                        'song_id': song_dict['song_id'],
                        'melon_song_id': melon_song_id
                    }]
                    
                    melon_crawler = create_crawler('melon')
                    melon_results = melon_crawler.crawl_songs(melon_data)
                    crawling_results['melon'] = melon_results
                    
                    # 성공 여부 확인
                    if melon_results and len(melon_results) > 0 and melon_results[0]:
                        platform_status['melon'] = 'success'
                        if save_db:
                            db_results['melon'] = save_melon_to_db(melon_results)
                        if save_csv:
                            csv_results['melon'] = save_melon_csv(melon_results)
                    else:
                        platform_status['melon'] = 'failed'
                else:
                    platform_status['melon'] = 'skipped'  # song_id 없음
                    logger.warning(f"⚠️ Melon song_id가 비어있어 건너뜀")
                    
        except Exception as e:
            logger.error(f"❌ {plat.upper()} 크롤링 중 오류: {str(e)}")
            platform_status[plat] = 'error'
            crawling_results[plat] = None

    # 실행 시간 계산
    end_time = time.time()
    execution_time = end_time - start_time
    
    # 성공/실패 통계
    success_count = sum(1 for status in platform_status.values() if status == 'success')
    failed_count = sum(1 for status in platform_status.values() if status == 'failed')
    error_count = sum(1 for status in platform_status.values() if status == 'error')
    skipped_count = sum(1 for status in platform_status.values() if status == 'skipped')
    
    # 요약 정보 생성
    summary = {
        'status': 'success' if success_count > 0 else 'failed',
        'execution_time': f"{execution_time:.2f}초",
        'platform_status': platform_status,
        'statistics': {
            'total_platforms': len(platforms_to_run),
            'success': success_count,
            'failed': failed_count,
            'error': error_count,
            'skipped': skipped_count
        },
        'crawling_results': crawling_results,
        'db_results': db_results,
        'csv_results': csv_results
    }
    
    # 깔끔한 요약 출력
    logger.info("=" * 60)
    logger.info("📊 단일 곡 크롤링 결과 요약")
    logger.info("=" * 60)
    logger.info(f"🎵 곡: {song_dict['artist_name']} - {song_dict['song_title']}")
    logger.info(f"⏱️  실행 시간: {execution_time:.2f}초")
    logger.info(f"📈 성공: {success_count}개, 실패: {failed_count}개, 오류: {error_count}개, 건너뜀: {skipped_count}개")
    
    # 플랫폼별 결과
    for plat, status in platform_status.items():
        status_emoji = "✅" if status == 'success' else "❌" if status == 'failed' else "⚠️" if status == 'skipped' else "💥"
        logger.info(f"{status_emoji} {plat.upper()}: {status}")
    
    if success_count > 0:
        logger.info("✅ 크롤링 완료 (일부 성공)")
    else:
        logger.info("❌ 크롤링 실패 (모든 플랫폼 실패)")
    
    logger.info("=" * 60)
    
    return summary 