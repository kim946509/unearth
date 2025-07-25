"""
YouTube 검색 관련 로직
"""
import logging
from typing import Optional

logger = logging.getLogger(__name__)


class YouTubeSearchLogic:
    """YouTube 검색 관련 로직"""
    
    def __init__(self, driver):
        self.driver = driver
    
    def navigate_to_video(self, url: str) -> bool:
        """
        YouTube 비디오 페이지로 이동
        
        Args:
            url (str): YouTube 비디오 URL
            
        Returns:
            bool: 이동 성공 여부
        """
        try:
            logger.info(f"🔗 YouTube 비디오 페이지로 이동: {url}")
            self.driver.get(url)
            return True
        except Exception as e:
            logger.error(f"❌ YouTube 비디오 페이지 이동 실패: {e}")
            return False
    
    def validate_video_url(self, url: str) -> bool:
        """
        YouTube 비디오 URL 유효성 검사
        
        Args:
            url (str): YouTube URL
            
        Returns:
            bool: 유효한 URL 여부
        """
        if not url:
            return False
        
        # YouTube URL 패턴 확인
        youtube_patterns = [
            'youtube.com/watch',
            'youtu.be/',
            'youtube.com/embed/'
        ]
        
        return any(pattern in url for pattern in youtube_patterns) 