"""
YouTube 데이터 추출 관련 로직
"""
import re
import logging
from bs4 import BeautifulSoup
from typing import Optional

from crawling.utils.constants import YouTubeSelectors
from crawling.utils.utils import make_soup, get_current_timestamp, convert_view_count

logger = logging.getLogger(__name__)


class YouTubeDataExtractionLogic:
    """YouTube 데이터 추출 관련 로직"""
    
    def __init__(self):
        pass
    
    def extract_video_data(self, html: str) -> Optional[dict]:
        """
        HTML에서 비디오 데이터 추출
        
        Args:
            html (str): 페이지 HTML
            
        Returns:
            dict: 추출된 데이터 또는 None
        """
        try:
            soup = make_soup(html)
            if not soup:
                return None
            
            # 동영상 제목 추출
            song_name = self.extract_title(soup)
            if not song_name:
                song_name = "제목 없음"
                logger.warning("동영상 제목을 찾지 못했습니다.")
            
            # 조회수 추출
            view_count = self.extract_view_count(soup)
            
            # 업로드 날짜 추출
            upload_date = self.extract_upload_date(soup)
            
            return {
                'song_name': song_name,
                'views': view_count,
                'upload_date': upload_date
            }
            
        except Exception as e:
            logger.error(f"❌ 데이터 추출 실패: {e}")
            return None
    
    def extract_title(self, soup: BeautifulSoup) -> Optional[str]:
        """
        동영상 제목 추출
        
        Args:
            soup (BeautifulSoup): 파싱된 HTML
            
        Returns:
            str: 동영상 제목 또는 None
        """
        title_selectors = [
            {'type': 'css', 'value': selector} for selector in YouTubeSelectors.TITLE_SELECTORS
        ]
        
        return self._find_with_selectors(soup, title_selectors)
    
    def extract_view_count(self, soup: BeautifulSoup) -> Optional[int]:
        """
        조회수 추출
        
        Args:
            soup (BeautifulSoup): 파싱된 HTML
            
        Returns:
            int: 조회수 또는 None
        """
        view_count_selectors = [
            {'type': 'css', 'value': selector} for selector in YouTubeSelectors.VIEW_COUNT_SELECTORS
        ]
        
        view_count_text = self._find_with_selectors(soup, view_count_selectors)
        return convert_view_count(view_count_text)
    
    def extract_upload_date(self, soup: BeautifulSoup) -> Optional[str]:
        """
        업로드 날짜 추출
        
        Args:
            soup (BeautifulSoup): 파싱된 HTML
            
        Returns:
            str: 업로드 날짜 또는 None
        """
        upload_date_selectors = [
            {'type': 'css', 'value': selector} for selector in YouTubeSelectors.UPLOAD_DATE_SELECTORS
        ]
        
        date_text = self._find_with_selectors(soup, upload_date_selectors)
        if date_text:
            # "YYYY. MM. DD." 또는 "YYYY.MM.DD" 형식을 "YYYY.MM.DD" 형식으로 변환
            date_match = re.search(r'(\d{4})[.\-\/\s]*(\d{1,2})[.\-\/\s]*(\d{1,2})', date_text)
            if date_match:
                year, month, day = date_match.groups()
                return f"{year}.{int(month):02d}.{int(day):02d}"
            else:
                return date_text.strip()
        
        return None
    
    def _find_with_selectors(self, soup: BeautifulSoup, selectors: list, get_text: bool = True) -> Optional[str]:
        """
        여러 selector를 순차적으로 시도하여 첫 번째로 찾은 element(또는 text)를 반환
        
        Args:
            soup (BeautifulSoup): 파싱된 HTML
            selectors (list): 셀렉터 리스트
            get_text (bool): 텍스트 반환 여부
            
        Returns:
            str: 찾은 텍스트 또는 None
        """
        for selector in selectors:
            if selector.get('type') == 'css':
                el = soup.select_one(selector['value'])
            elif selector.get('type') == 'tag_class':
                el = soup.find(selector['tag'], class_=selector['class'])
            elif selector.get('type') == 'tag_id':
                el = soup.find(selector['tag'], id=selector['id'])
            else:
                continue
            
            if el:
                return el.text.strip() if get_text else el
        
        return None 