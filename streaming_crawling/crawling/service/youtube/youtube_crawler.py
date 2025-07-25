"""
YouTube 크롤러 조율자
"""
import logging
from typing import Dict, List, Optional

from crawling.utils.utils import get_current_timestamp
from .search_logic import YouTubeSearchLogic
from .navigation_logic import YouTubeNavigationLogic
from .data_extraction_logic import YouTubeDataExtractionLogic

logger = logging.getLogger(__name__)


class YouTubeCrawler:
    """YouTube 크롤러 조율자"""
    
    def __init__(self, driver):
        self.driver = driver
        self.search_logic = YouTubeSearchLogic(driver)
        self.navigation_logic = YouTubeNavigationLogic(driver)
        self.data_extraction_logic = YouTubeDataExtractionLogic()
    
    def crawl_song(self, song_info: Dict) -> Optional[Dict]:
        """
        단일 곡 크롤링
        
        Args:
            song_info (dict): 곡 정보 {'youtube_url': url, 'artist_ko': artist, 'song_id': id}
            
        Returns:
            dict: 크롤링 결과 또는 None
        """
        try:
            url = song_info.get('youtube_url')
            artist_name = song_info.get('artist_ko', '')
            song_id = song_info.get('song_id')
            
            if not url:
                logger.warning(f"⚠️ YouTube URL이 없음: {artist_name}")
                return None
            
            # URL 유효성 검사
            if not self.search_logic.validate_video_url(url):
                logger.warning(f"⚠️ 유효하지 않은 YouTube URL: {url}")
                return None
            
            # 비디오 페이지로 이동
            if not self.search_logic.navigate_to_video(url):
                logger.error(f"❌ YouTube 페이지 이동 실패: {url}")
                return None
            
            # 페이지 로딩 대기
            if not self.navigation_logic.wait_for_page_load():
                logger.error(f"❌ YouTube 페이지 로딩 실패: {url}")
                return None
            
            # HTML 파싱
            html = self.driver.page_source
            
            # 데이터 추출
            extracted_data = self.data_extraction_logic.extract_video_data(html)
            if not extracted_data:
                logger.error(f"❌ YouTube 데이터 추출 실패: {url}")
                return None
            
            # 결과 조합
            result = {
                'song_id': song_id,
                'song_name': extracted_data['song_name'],
                'artist_name': artist_name,
                'views': extracted_data['views'],
                'listeners': -1,  # YouTube는 청취자 수 제공 안함
                'youtube_url': url,
                'upload_date': extracted_data['upload_date'],
                'extracted_date': get_current_timestamp()
            }
            
            logger.info(f"✅ YouTube 크롤링 성공: {artist_name} - {result['song_name']} (조회수: {result['views']})")
            return result
            
        except Exception as e:
            logger.error(f"❌ YouTube 크롤링 실패: {e}", exc_info=True)
            return None
    
    def crawl_multiple(self, url_artist_song_id_list: List[tuple]) -> Dict:
        """
        여러 YouTube URL을 크롤링 (기존 인터페이스 호환성)
        
        Args:
            url_artist_song_id_list (list): [('url', 'artist_name', 'song_id'), ...] 형태의 리스트
            
        Returns:
            dict: 크롤링 결과 딕셔너리 {song_id: data}
        """
        results = {}
        
        for url, artist_name, song_id in url_artist_song_id_list:
            try:
                # 딕셔너리 형태로 변환
                song_info = {
                    'youtube_url': url,
                    'artist_ko': artist_name,
                    'song_id': song_id
                }
                
                result = self.crawl_song(song_info)
                if result:
                    results[song_id] = result
                else:
                    # 실패 시 기본 구조로 결과 생성
                    results[song_id] = {
                        'song_id': song_id,
                        'song_name': None,
                        'artist_name': artist_name,
                        'views': None,
                        'listeners': -1,
                        'youtube_url': url,
                        'upload_date': None,
                        'extracted_date': get_current_timestamp(),
                    }
                    
            except Exception as e:
                logger.error(f"❌ {artist_name} 크롤링 실패: {e}", exc_info=True)
                results[song_id] = {
                    'song_id': song_id,
                    'song_name': None,
                    'artist_name': artist_name,
                    'views': None,
                    'listeners': -1,
                    'youtube_url': url,
                    'upload_date': None,
                    'extracted_date': get_current_timestamp(),
                }
        
        return results 