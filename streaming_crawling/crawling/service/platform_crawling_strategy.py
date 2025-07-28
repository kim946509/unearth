"""
플랫폼별 크롤링 전략 인터페이스
"""
from abc import ABC, abstractmethod
from typing import List, Dict

from crawling.utils.batch_crawling_logger import BatchCrawlingLogger


class PlatformCrawlingStrategy(ABC):
    """플랫폼별 크롤링 전략을 정의하는 추상 클래스"""
    
    @abstractmethod
    def crawl_platform(self, song_list: List[Dict], log_writer: BatchCrawlingLogger) -> List[Dict]:
        """플랫폼별 크롤링을 실행합니다."""
        pass
    
    @abstractmethod
    def get_platform_name(self) -> str:
        """플랫폼 이름을 반환합니다."""
        pass 