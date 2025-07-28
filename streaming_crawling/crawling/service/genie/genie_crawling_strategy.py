"""
Genie 크롤링 전략 구현
"""
import logging
from typing import List, Dict, Optional

from crawling.utils.driver import setup_driver
from crawling.utils.batch_crawling_logger import BatchCrawlingLogger
from crawling.service.platform_crawling_strategy import PlatformCrawlingStrategy
from .genie_crawler import GenieCrawler

logger = logging.getLogger(__name__)


class GenieCrawlingStrategy(PlatformCrawlingStrategy):
    """Genie 플랫폼 크롤링 전략"""
    
    def get_platform_name(self) -> str:
        return "genie"
    
    def crawl_platform(self, song_list: List[Dict], log_writer: BatchCrawlingLogger) -> List[Dict]:
        """
        Genie 크롤링 실행
        
        Args:
            song_list: 크롤링할 곡 리스트 [{'title_ko': '곡명', 'artist_ko': '가수명', 'song_id': 'id'}, ...]
            log_writer: 로그 작성기
        
        Returns:
            크롤링된 데이터 리스트
        """
        if not song_list:
            logger.warning("⚠️ Genie 크롤링 대상 곡이 없습니다.")
            return []
        
        logger.info(f"🎯 Genie 크롤링 시작 - 총 {len(song_list)}곡")
        
        crawled_data = []
        successful_song_ids = set()
        
        try:
            # Chrome 드라이버 설정 및 실행
            with setup_driver() as driver:
                crawler = GenieCrawler(driver)
                
                # 각 곡에 대해 크롤링 실행
                for song_info in song_list:
                    song_title = song_info.get('title_ko', '')
                    artist_name = song_info.get('artist_ko', '')
                    song_id = song_info.get('song_id')
                    
                    logger.info(f"🔍 검색 중: {song_title} - {artist_name} (ID: {song_id})")
                    
                    # 크롤링 실행
                    result = crawler.crawl_song(song_info)
                    
                    if result:
                        # song_id가 None인 경우 원본 song_id로 설정
                        if result.get('song_id') is None:
                            result['song_id'] = song_id
                        crawled_data.append(result)
                        successful_song_ids.add(song_id)
                        logger.info(f"✅ 크롤링 완료: {result['song_title']} - {result['artist_name']} (조회수: {result['views']})")
                    else:
                        logger.warning(f"❌ 크롤링 실패: {song_title} - {artist_name}")
                        log_writer.add_crawling_failure(f"{artist_name} - {song_title}", "genie")
            
            # 실패한 곡들 로그에 기록
            self._log_failures(song_list, successful_song_ids, log_writer)
            
            logger.info(f"✅ Genie 크롤링 완료 - 성공: {len(crawled_data)}곡")
            return crawled_data
            
        except Exception as e:
            logger.error(f"❌ Genie 크롤링 실행 중 오류 발생: {e}", exc_info=True)
            # 모든 곡을 실패로 기록
            for song_info in song_list:
                song_title = song_info.get('title_ko', '')
                artist_name = song_info.get('artist_ko', '')
                log_writer.add_crawling_failure(f"{artist_name} - {song_title}", "genie")
            return []
    
    def _log_failures(self, song_list: List[Dict], successful_song_ids: set, log_writer: BatchCrawlingLogger):
        """크롤링 실패한 곡들을 로그에 기록합니다."""
        for song_info in song_list:
            song_id = song_info.get('song_id')
            if song_id not in successful_song_ids:
                song_title = song_info.get('title_ko', '')
                artist_name = song_info.get('artist_ko', '')
                log_writer.add_crawling_failure(f"{artist_name} - {song_title}", "genie") 