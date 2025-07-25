"""
YouTube Music 통합 크롤러 - 분리된 로직들을 조율
"""
import logging
from .search_logic import YouTubeMusicSearchLogic
from .navigation_logic import YouTubeMusicNavigationLogic
from .data_extraction_logic import YouTubeMusicDataExtractionLogic

logger = logging.getLogger(__name__)

class YouTubeMusicCrawler:
    """YouTube Music 크롤링을 담당하는 통합 클래스"""
    
    def __init__(self, driver):
        self.driver = driver
        self.search_logic = YouTubeMusicSearchLogic(driver)
        self.navigation_logic = YouTubeMusicNavigationLogic(driver)
        self.data_extraction_logic = YouTubeMusicDataExtractionLogic()
    
    def login(self):
        """
        YouTube Music 로그인
        
        Returns:
            bool: 로그인 성공 여부
        """
        return self.navigation_logic.login()
    
    def crawl_song(self, song_info: dict) -> dict:
        """
        단일 곡 크롤링
        
        Args:
            song_info (dict): 곡 정보 (title_ko, title_en, artist_ko, artist_en)
            
        Returns:
            dict: 크롤링 결과 또는 None
        """
        try:
            if not self.navigation_logic.is_logged_in:
                logger.error("❌ 로그인이 필요합니다.")
                return None
            
            # 먼저 국문으로 검색
            logger.info("🔍 국문으로 검색 시도")
            html = self.search_logic.search_song(song_info['title_ko'], song_info['artist_ko'])
            if html:
                result = self.data_extraction_logic.parse_song_info(html, song_info)
                if result:
                    return result
            
            # 국문 검색 실패시 영문으로 검색
            if song_info.get('title_en') and song_info.get('artist_en'):
                logger.info("🔍 영문으로 검색 시도")
                html = self.search_logic.search_song(song_info['title_en'], song_info['artist_en'])
                if html:
                    result = self.data_extraction_logic.parse_song_info(html, song_info)
                    if result:
                        return result
            
            logger.warning(f"❌ 모든 검색 시도 실패: {song_info}")
            return None
            
        except Exception as e:
            logger.error(f"❌ 곡 크롤링 실패: {e}", exc_info=True)
            return None 