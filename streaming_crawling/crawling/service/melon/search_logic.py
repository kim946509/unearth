"""
Melon 검색 관련 로직
"""
import logging

logger = logging.getLogger(__name__)

class MelonSearchLogic:
    """Melon 검색 관련 로직을 담당하는 클래스"""
    
    def __init__(self):
        pass
    
    def validate_melon_song_id(self, melon_song_id: str) -> bool:
        """
        Melon 곡 ID 유효성 검사
        
        Args:
            melon_song_id (str): Melon 곡 ID
            
        Returns:
            bool: 유효한 ID 여부
        """
        if not melon_song_id:
            return False
        
        # Melon 곡 ID는 숫자로만 구성됨
        try:
            int(melon_song_id)
            return True
        except ValueError:
            return False
    
    def prepare_search_query(self, melon_song_id: str) -> str:
        """
        검색 쿼리 준비 (API URL 생성용)
        
        Args:
            melon_song_id (str): Melon 곡 ID
            
        Returns:
            str: 준비된 쿼리
        """
        return melon_song_id.strip() 