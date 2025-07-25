"""
Melon 통합 크롤러 - 분리된 로직들을 조율
"""
import logging
from .search_logic import MelonSearchLogic
from .navigation_logic import MelonNavigationLogic
from .data_extraction_logic import MelonDataExtractionLogic

logger = logging.getLogger(__name__)

class MelonCrawler:
    """Melon 크롤링을 담당하는 통합 클래스"""
    
    def __init__(self):
        self.search_logic = MelonSearchLogic()
        self.navigation_logic = MelonNavigationLogic()
        self.data_extraction_logic = MelonDataExtractionLogic()
    
    def crawl_song(self, song_info: dict) -> dict:
        """
        단일 곡 크롤링
        
        Args:
            song_info (dict): 곡 정보 (melon_song_id, song_id)
            
        Returns:
            dict: 크롤링 결과 또는 None
        """
        try:
            melon_song_id = song_info.get('melon_song_id')
            song_id = song_info.get('song_id')
            
            if not melon_song_id:
                logger.error("❌ melon_song_id가 필요합니다.")
                return None
            
            logger.debug(f"🎵 Melon API 호출: songId={melon_song_id}")
            
            # API를 통한 데이터 추출
            result = self.data_extraction_logic.crawl_song(melon_song_id, song_id)
            
            if result:
                logger.debug(f"✅ Melon 크롤링 성공: {result['song_title']} - {result['artist_name']} (조회수: {result['views']}, 청취자: {result['listeners']})")
                return result
            else:
                logger.warning(f"❌ 크롤링 실패: melon_song_id={melon_song_id}")
                return None
            
        except Exception as e:
            logger.error(f"❌ 곡 크롤링 실패: {e}", exc_info=True)
            return None 