"""
YouTube 크롤링 메인 실행 파일
"""
import logging
from crawling.utils.driver import setup_driver
from .youtube_logic import YouTubeCrawler

logger = logging.getLogger(__name__)

def run_youtube_crawling(url_artist_song_id_list):
    """
    YouTube 크롤링 실행
    
    Args:
        url_artist_song_id_list (list): 크롤링할 URL, 아티스트, song_id 리스트 [('url1', 'artist1', 'song_id1'), ('url2', 'artist2', 'song_id2'), ...]
    
    Returns:
        dict: 크롤링된 데이터 딕셔너리
    """
    logger.info(f"🖤 YouTube 크롤링 시작 - 총 {len(url_artist_song_id_list)}개 URL")
    
    try:
        # Chrome 드라이버 설정 및 실행
        with setup_driver() as driver:
            crawler = YouTubeCrawler(driver)
            
            # 크롤링 실행
            results = crawler.crawl_multiple(url_artist_song_id_list)
            
            logger.info(f"🖤 YouTube 크롤링 완료 - 성공: {len(results)}개")
            
            return results
            
    except Exception as e:
        logger.error(f"❌ YouTube 크롤링 실행 중 오류 발생: {e}", exc_info=True)
        return {}

if __name__ == "__main__":
    # 테스트용 실행
    test_urls = [
        ("https://www.youtube.com/watch?v=Sv2mIvMwrSY", "Jaerium", "test_song_id_1"),
        ("https://www.youtube.com/watch?v=R1CZTJ8hW0s", "Jaerium", "test_song_id_2"),
    ]
    
    results = run_youtube_crawling(test_urls)
    print(f"크롤링 결과: {len(results)}개")
    for song_id, result in results.items():
        print(f"[YouTube] 곡명: {result['song_name']}, 아티스트: {result['artist_name']}, "
              f"조회수: {result['views']}, 업로드일: {result['upload_date']}") 