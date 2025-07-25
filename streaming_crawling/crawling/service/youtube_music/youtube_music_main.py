"""
YouTube Music 크롤링 메인 실행 파일
"""
import logging
from crawling.utils.driver import setup_driver 
from .youtube_music_crawler import YouTubeMusicCrawler

logger = logging.getLogger(__name__)

def run_youtube_music_crawling(song_list):
    """
    YouTube Music 크롤링 실행
    
    Args:
        song_list (list): 크롤링할 곡 리스트 [{'title_ko': '곡명', 'artist_ko': '가수명', 'song_id': 'id'}, ...]
    
    Returns:
        list: 크롤링된 데이터 리스트
    """
    logger.info(f"🎵 YouTube Music 크롤링 시작 - 총 {len(song_list)}곡")
    
    crawled_data = []
    
    try:
        # Chrome 드라이버 설정 및 실행
        with setup_driver() as driver:
            crawler = YouTubeMusicCrawler(driver)
            
            # 로그인 수행
            if not crawler.login():
                logger.error("❌ YouTube Music 로그인 실패")
                return []
            
            # 각 곡에 대해 크롤링 실행
            for song_info in song_list:
                song_title = song_info.get('title_ko', '')
                artist_name = song_info.get('artist_ko', '')
                song_id = song_info.get('song_id')
                
                logger.info(f"🔍 검색 중: {song_title} - {artist_name} (ID: {song_id})")
                
                # 새로운 구조로 곡 정보 전달
                song_data = {
                    'title_ko': song_title,
                    'title_en': song_info.get('title_en', ''),  # 영문 제목이 있으면 사용
                    'artist_ko': artist_name, 
                    'artist_en': song_info.get('artist_en', ''),  # 영문 아티스트가 있으면 사용
                    'song_id': song_id  # song_id 반드시 포함
                }
                
                # 크롤링 실행
                result = crawler.crawl_song(song_data)
                
                if result:
                    crawled_data.append(result)
                    logger.info(f"✅ 크롤링 완료: {result['song_title']} - {result['artist_name']} (조회수: {result['views']})")
                else:
                    logger.warning(f"❌ 크롤링 실패: {song_title} - {artist_name}")
        
        logger.info(f"🎵 YouTube Music 크롤링 완료 - 성공: {len(crawled_data)}곡")
        
        return crawled_data
        
    except Exception as e:
        logger.error(f"❌ YouTube Music 크롤링 실행 중 오류 발생: {e}", exc_info=True)
        return []

if __name__ == "__main__":
    # 테스트용 실행
    test_songs = [
        {'song_title': 'Supernova', 'artist_name': 'aespa'},
        {'song_title': 'How Sweet', 'artist_name': 'NewJeans'},
    ]
    
    results = run_youtube_music_crawling(test_songs)
    print(f"크롤링 결과: {len(results)}곡") 