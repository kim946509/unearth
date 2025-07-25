"""
Melon 데이터 추출 관련 로직
"""
import logging
import json
from crawling.utils.utils import get_current_timestamp
from .navigation_logic import MelonNavigationLogic
from .search_logic import MelonSearchLogic

logger = logging.getLogger(__name__)

class MelonDataExtractionLogic:
    """Melon 데이터 추출 관련 로직을 담당하는 클래스"""
    
    def __init__(self):
        self.navigation_logic = MelonNavigationLogic()
        self.search_logic = MelonSearchLogic()
    
    def crawl_song(self, melon_song_id: str, song_id: str = None) -> dict:
        """
        단일 곡 크롤링 (API 호출)
        
        Args:
            melon_song_id (str): 멜론 곡 ID
            song_id (str, optional): SongInfo의 pk값
            
        Returns:
            dict: 크롤링 결과 또는 None
        """
        try:
            if not self.search_logic.validate_melon_song_id(melon_song_id):
                logger.error("❌ 유효하지 않은 melon_song_id입니다.")
                return None
            
            logger.debug(f"🎵 Melon API 호출: songId={melon_song_id}")
            
            # API 호출
            data = self.navigation_logic.navigate_to_song_api(melon_song_id)
            if not data:
                return None
            
            # 데이터 추출
            song_info = data['response']['SONGINFO']
            stream_info = data['response'].get('STREAMREPORTINFO', {})
            
            # 곡 정보 추출
            song_name = song_info.get('SONGNAME', '')
            artist_list = song_info.get('ARTISTLIST', [])
            artist_name = artist_list[0].get('ARTISTNAME', '') if artist_list else ''
            
            # 조회수 및 청취자수 추출
            total_listen_count = stream_info.get('TOTALLISTENCNT', '0')
            total_listener_count = stream_info.get('TOTALLISTENERCNT', '0')
            
            # 숫자 변환 (쉼표 제거)
            views = self._convert_to_number(total_listen_count)
            listeners = self._convert_to_number(total_listener_count)
            
            result = {
                'song_title': song_name,
                'artist_name': artist_name,
                'views': views,
                'listeners': listeners,
                'crawl_date': get_current_timestamp(),
                'song_id': song_id,
                'melon_song_id': melon_song_id
            }
            
            logger.debug(f"✅ Melon 크롤링 성공: {song_name} - {artist_name} (조회수: {views}, 청취자: {listeners})")
            return result
            
        except json.JSONDecodeError as e:
            logger.error(f"❌ JSON 파싱 실패: {e}")
            return None
        except Exception as e:
            logger.error(f"❌ Melon 크롤링 실패: {e}", exc_info=True)
            return None
    
    def _convert_to_number(self, value):
        """
        문자열을 숫자로 변환 (쉼표 제거)
        
        Args:
            value (str): 변환할 문자열
            
        Returns:
            int: 변환된 숫자
                - 정상값: 양수
                - 0: 실제로 0인 경우 또는 빈 값
                - -1: 해당 플랫폼에서 제공하지 않는 데이터
                - -999: 크롤링 실패/오류
        """
        try:
            # 빈 값이면 0으로 처리
            if not value or value == "":
                logger.info(f"ℹ️ 빈 값 발견, 0으로 처리: {value}")
                return 0
            
            # 쉼표 제거 후 숫자 변환
            clean_value = str(value).replace(',', '')
            result = int(clean_value)
            
            # 0인 경우 실제 0으로 처리 (멜론에서는 0이 나올 수 있음)
            return result
            
        except (ValueError, TypeError):
            logger.warning(f"숫자 변환 실패: {value}")
            return -999  # 변환 오류 