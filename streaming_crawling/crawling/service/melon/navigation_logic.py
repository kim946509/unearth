"""
Melon 네비게이션 관련 로직
"""
import os
import logging
import requests
from dotenv import load_dotenv

# .env 파일 로드
load_dotenv()

logger = logging.getLogger(__name__)

class MelonNavigationLogic:
    """Melon 네비게이션 관련 로직을 담당하는 클래스"""
    
    def __init__(self):
        # .env에서 API URL 로드
        self.api_base_url = os.getenv('MELON_API_URL', '')
        if not self.api_base_url:
            logger.error("❌ MELON_API_URL 환경변수가 설정되지 않았습니다.")
            raise ValueError("MELON_API_URL 환경변수가 필요합니다.")
        
        self.session = requests.Session()
        # User-Agent 설정
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Accept': 'application/json, text/plain, */*',
            'Accept-Language': 'ko-KR,ko;q=0.9,en;q=0.8',
            'Referer': 'https://m2.melon.com/',
            'Origin': 'https://m2.melon.com'
        })
    
    def navigate_to_song_api(self, melon_song_id: str) -> dict:
        """
        Melon API로 곡 정보 요청
        
        Args:
            melon_song_id (str): Melon 곡 ID
            
        Returns:
            dict: API 응답 데이터 또는 None
        """
        try:
            # API 호출
            api_url = f"{self.api_base_url}?songId={melon_song_id}"
            response = self.session.get(api_url, timeout=10)
            
            if response.status_code != 200:
                logger.error(f"❌ API 호출 실패: HTTP {response.status_code}")
                return None
            
            # JSON 파싱
            data = response.json()
            
            # 응답 구조 확인
            if 'response' not in data or 'SONGINFO' not in data['response']:
                logger.error("❌ API 응답 구조가 올바르지 않습니다.")
                return None
            
            return data
            
        except requests.exceptions.RequestException as e:
            logger.error(f"❌ API 요청 실패: {e}")
            return None
        except Exception as e:
            logger.error(f"❌ API 네비게이션 실패: {e}")
            return None 