"""
단순화된 크롤링 매니저
크롤링 전체 흐름을 4단계로 단순화:
1. 크롤링 대상 노래 조회
2. 크롤링 실행
3. DB 저장
4. CSV 저장
"""
import logging
from datetime import date
from crawling_view.data.song_service import SongService
from crawling_view.data.db_writer import save_genie_to_db, save_youtube_to_db, save_youtube_music_to_db, save_melon_to_db
from crawling_view.data.csv_writer import save_genie_csv, save_youtube_csv, save_youtube_music_csv, save_melon_csv
from crawling_view.controller.platform_crawlers import create_crawler
from crawling_view.utils.constants import Platforms
from crawling_view.utils.batch_crawling_logger import BatchCrawlingLogger

logger = logging.getLogger(__name__)

def run_crawling(target_date=None):
    """
    크롤링 전체 프로세스 실행
        
    Args:
        target_date (date, optional): 크롤링 대상 날짜. None이면 오늘 날짜
            
    Returns:
        dict: 크롤링 결과 요약
    """
    # 로그 라이터 초기화
    log_writer = BatchCrawlingLogger()
    
    try:
        # 1단계: 크롤링 대상 노래 조회
        active_songs = SongService.get_active_songs(target_date)
        
        if not active_songs:
            logger.warning("⚠️ 크롤링 대상 노래가 없습니다.")
            return {'status': 'no_songs', 'message': '크롤링 대상 노래가 없습니다.'}
        
        # 로그 라이터 시작
        log_writer.start_crawling(target_date or date.today(), len(active_songs))
        
        # 2단계: 플랫폼별 크롤링 실행
        crawling_results = {}
        
        # Genie 크롤링
        genie_songs = SongService.get_songs_by_platform(active_songs, 'genie')
        if genie_songs:
            genie_crawler = create_crawler('genie')
            genie_data = SongService.convert_to_crawling_format(genie_songs, 'genie')
            genie_results = genie_crawler.crawl_songs(genie_data)
            crawling_results['genie'] = genie_results
            
            # 실패한 곡만 기록 (Genie는 List[Dict] 형태)
            successful_song_ids = {result.get('song_id') for result in genie_results}
            for song in genie_songs:
                if song.id not in successful_song_ids:
                    song_name = f"{song.artist_ko} - {song.title_ko}"
                    log_writer.add_crawling_failure(song_name, 'genie')
        
        # YouTube Music 크롤링
        ytmusic_songs = SongService.get_songs_by_platform(active_songs, 'youtube_music')
        if ytmusic_songs:
            ytmusic_crawler = create_crawler('youtube_music')
            ytmusic_data = SongService.convert_to_crawling_format(ytmusic_songs, 'youtube_music')
            ytmusic_results = ytmusic_crawler.crawl_songs(ytmusic_data)
            crawling_results['youtube_music'] = ytmusic_results
            
            # 실패한 곡만 기록 (YouTube Music은 List[Dict] 형태)
            successful_song_ids = {result.get('song_id') for result in ytmusic_results}
            for song in ytmusic_songs:
                if song.id not in successful_song_ids:
                    song_name = f"{song.artist_ko} - {song.title_ko}"
                    log_writer.add_crawling_failure(song_name, 'youtube_music')
        
        # YouTube 크롤링
        youtube_songs = SongService.get_songs_by_platform(active_songs, 'youtube')
        if youtube_songs:
            youtube_crawler = create_crawler('youtube')
            youtube_data = SongService.convert_to_crawling_format(youtube_songs, 'youtube')
            youtube_results = youtube_crawler.crawl_songs(youtube_data)
            crawling_results['youtube'] = youtube_results
            
            # 실패한 곡만 기록 (YouTube는 딕셔너리 형태로 반환)
            successful_song_ids = set(youtube_results.keys())
            for song in youtube_songs:
                if song.id not in successful_song_ids:
                    song_name = f"{song.artist_ko} - {song.title_ko}"
                    log_writer.add_crawling_failure(song_name, 'youtube')
        
        # Melon 크롤링
        melon_songs = SongService.get_songs_by_platform(active_songs, 'melon')
        if melon_songs:
            melon_crawler = create_crawler('melon')
            melon_data = SongService.convert_to_crawling_format(melon_songs, 'melon')
            melon_results = melon_crawler.crawl_songs(melon_data)
            crawling_results['melon'] = melon_results
            
            # 실패한 곡만 기록 (Melon은 List[Dict] 형태)
            successful_song_ids = {result.get('song_id') for result in melon_results}
            for song in melon_songs:
                if song.id not in successful_song_ids:
                    song_name = f"{song.artist_ko} - {song.title_ko}"
                    log_writer.add_crawling_failure(song_name, 'melon')
        
        # 3단계: DB 저장
        db_results = {}
        
        if 'genie' in crawling_results:
            db_results['genie'] = save_genie_to_db(crawling_results['genie'])
            # DB 저장 실패만 기록
            if db_results['genie'].get('saved_count', 0) == 0 and db_results['genie'].get('updated_count', 0) == 0:
                for song in genie_songs:
                    song_name = f"{song.artist_ko} - {song.title_ko}"
                    log_writer.add_db_failure(song_name, 'genie')
        
        if 'youtube_music' in crawling_results:
            db_results['youtube_music'] = save_youtube_music_to_db(crawling_results['youtube_music'])
            # DB 저장 실패만 기록
            if db_results['youtube_music'].get('saved_count', 0) == 0 and db_results['youtube_music'].get('updated_count', 0) == 0:
                for song in ytmusic_songs:
                    song_name = f"{song.artist_ko} - {song.title_ko}"
                    log_writer.add_db_failure(song_name, 'youtube_music')
        
        if 'youtube' in crawling_results:
            db_results['youtube'] = save_youtube_to_db(crawling_results['youtube'])
            # DB 저장 실패만 기록
            if db_results['youtube'].get('saved_count', 0) == 0 and db_results['youtube'].get('updated_count', 0) == 0:
                for song in youtube_songs:
                    song_name = f"{song.artist_ko} - {song.title_ko}"
                    log_writer.add_db_failure(song_name, 'youtube')
        
        if 'melon' in crawling_results:
            db_results['melon'] = save_melon_to_db(crawling_results['melon'])
            # DB 저장 실패만 기록
            if db_results['melon'].get('saved_count', 0) == 0 and db_results['melon'].get('updated_count', 0) == 0:
                for song in melon_songs:
                    song_name = f"{song.artist_ko} - {song.title_ko}"
                    log_writer.add_db_failure(song_name, 'melon')
        
        # 4단계: CSV 저장
        csv_results = {}
        
        if 'genie' in crawling_results:
            csv_results['genie'] = save_genie_csv(crawling_results['genie'])
            # CSV 저장 실패만 기록
            if not csv_results['genie']:
                for song in genie_songs:
                    song_name = f"{song.artist_ko} - {song.title_ko}"
                    log_writer.add_csv_failure(song_name, 'genie')
                
        if 'youtube_music' in crawling_results:
            csv_results['youtube_music'] = save_youtube_music_csv(crawling_results['youtube_music'])
            # CSV 저장 실패만 기록
            if not csv_results['youtube_music']:
                for song in ytmusic_songs:
                    song_name = f"{song.artist_ko} - {song.title_ko}"
                    log_writer.add_csv_failure(song_name, 'youtube_music')
        
        if 'youtube' in crawling_results:
            csv_results['youtube'] = save_youtube_csv(crawling_results['youtube'])
            # CSV 저장 실패만 기록
            if not csv_results['youtube']:
                for song in youtube_songs:
                    song_name = f"{song.artist_ko} - {song.title_ko}"
                    log_writer.add_csv_failure(song_name, 'youtube')
        
        if 'melon' in crawling_results:
            csv_results['melon'] = save_melon_csv(crawling_results['melon'])
            # CSV 저장 실패만 기록
            if not csv_results['melon']:
                for song in melon_songs:
                    song_name = f"{song.artist_ko} - {song.title_ko}"
                    log_writer.add_csv_failure(song_name, 'melon')
        
        # 로그 라이터 종료 및 최종 요약 생성
        log_writer.end_crawling()
        
        # 결과 요약
        summary = {
            'status': 'success',
            'target_date': target_date or date.today(),
            'total_songs': len(active_songs),
            'crawling_results': crawling_results,
            'db_results': db_results,
            'csv_results': csv_results,
            'log_summary': log_writer.get_summary_dict()
        }
        
        return summary
        
    except Exception as e:
        logger.error(f"❌ 크롤링 프로세스 실패: {e}", exc_info=True)
        return {'status': 'error', 'message': str(e)}

def run_platform_crawling(platform, target_date=None):
    """
    특정 플랫폼만 크롤링 실행
    
    Args:
        platform (str): 플랫폼명 ('genie', 'youtube', 'youtube_music', 'melon')
        target_date (date, optional): 크롤링 대상 날짜
        
    Returns:
        dict: 크롤링 결과
    """
    # 로그 라이터 초기화
    log_writer = BatchCrawlingLogger()
    
    try:
        # 1단계: 크롤링 대상 노래 조회
        active_songs = SongService.get_active_songs(target_date)
        platform_songs = SongService.get_songs_by_platform(active_songs, platform)
        
        if not platform_songs:
            logger.warning(f"⚠️ {platform} 크롤링 대상 노래가 없습니다.")
            return {'status': 'no_songs', 'platform': platform}
        
        # 로그 라이터 시작
        log_writer.start_crawling(target_date or date.today(), len(platform_songs))
        
        # 2단계: 크롤링 실행
        crawler = create_crawler(platform)
        crawling_data = SongService.convert_to_crawling_format(platform_songs, platform)
        crawling_results = crawler.crawl_songs(crawling_data)
        
        # 실패한 곡만 기록 (플랫폼별 결과 형태에 따라 처리)
        if platform == Platforms.YOUTUBE:
            # YouTube는 Dict[str, Dict] 형태로 반환
            successful_song_ids = set(crawling_results.keys())
        else:
            # Genie, YouTube Music, Melon은 List[Dict] 형태로 반환
            successful_song_ids = {result.get('song_id') for result in crawling_results}
        
        for song in platform_songs:
            if song.id not in successful_song_ids:
                song_name = f"{song.artist_ko} - {song.title_ko}"
                log_writer.add_crawling_failure(song_name, platform)
        
        # 3단계: DB 저장
        if platform == Platforms.GENIE:
            db_results = save_genie_to_db(crawling_results)
        elif platform == Platforms.YOUTUBE_MUSIC:
            db_results = save_youtube_music_to_db(crawling_results)
        elif platform == Platforms.YOUTUBE:
            db_results = save_youtube_to_db(crawling_results)
        elif platform == Platforms.MELON:
            db_results = save_melon_to_db(crawling_results)
        else:
            db_results = {'error': '지원하지 않는 플랫폼'}
        
        # DB 저장 실패만 기록
        if db_results.get('saved_count', 0) == 0 and db_results.get('updated_count', 0) == 0:
            for song in platform_songs:
                song_name = f"{song.artist_ko} - {song.title_ko}"
                log_writer.add_db_failure(song_name, platform)
        
        # 4단계: CSV 저장
        if platform == Platforms.GENIE:
            csv_results = save_genie_csv(crawling_results)
        elif platform == Platforms.YOUTUBE_MUSIC:
            csv_results = save_youtube_music_csv(crawling_results)
        elif platform == Platforms.YOUTUBE:
            csv_results = save_youtube_csv(crawling_results)
        elif platform == Platforms.MELON:
            csv_results = save_melon_csv(crawling_results)
        else:
            csv_results = {'error': '지원하지 않는 플랫폼'}
        
        # CSV 저장 실패만 기록
        if not csv_results:
            for song in platform_songs:
                song_name = f"{song.artist_ko} - {song.title_ko}"
                log_writer.add_csv_failure(song_name, platform)
        
        # 로그 라이터 종료 및 최종 요약 생성
        log_writer.end_crawling()
        
        summary = {
            'status': 'success',
            'platform': platform,
            'target_date': target_date or date.today(),
            'total_songs': len(platform_songs),
            'crawling_results': crawling_results,
            'db_results': db_results,
            'csv_results': csv_results,
            'log_summary': log_writer.get_summary_dict()
        }
        
        return summary
        
    except Exception as e:
        logger.error(f"❌ {platform} 크롤링 실패: {e}", exc_info=True)
        return {'status': 'error', 'platform': platform, 'message': str(e)} 