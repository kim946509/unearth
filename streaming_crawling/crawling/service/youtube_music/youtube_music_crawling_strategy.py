"""
YouTube Music 크롤링 전략 구현
"""
import logging
from typing import List, Dict

from crawling.utils.batch_crawling_logger import BatchCrawlingLogger
from crawling.service.platform_crawling_strategy import PlatformCrawlingStrategy
from .youtube_music_main import run_youtube_music_crawling

logger = logging.getLogger(__name__)


class YouTubeMusicCrawlingStrategy(PlatformCrawlingStrategy):
    """YouTube Music 플랫폼 크롤링 전략"""
    
    def get_platform_name(self) -> str:
        return "youtube_music"
    
    def crawl_platform(self, song_list: List[Dict], log_writer: BatchCrawlingLogger) -> List[Dict]:
        """YouTube Music 크롤링 실행"""
        if not song_list:
            logger.warning("⚠️ YouTube Music 크롤링 대상 곡이 없습니다.")
            return []
        
        logger.info(f"🎯 YouTube Music 크롤링 시작 - 총 {len(song_list)}곡")
        
        try:
            results = run_youtube_music_crawling(song_list)
            
            # 실패한 곡들 로그에 기록
            successful_song_ids = {result.get('song_id') for result in results}
            self._log_failures(song_list, successful_song_ids, log_writer)
            
            logger.info(f"✅ YouTube Music 크롤링 완료 - 성공: {len(results)}곡")
            return results
            
        except Exception as e:
            logger.error(f"❌ YouTube Music 크롤링 실행 중 오류 발생: {e}", exc_info=True)
            # 모든 곡을 실패로 기록
            for song_info in song_list:
                song_title = song_info.get('title_ko', '')
                artist_name = song_info.get('artist_ko', '')
                log_writer.add_crawling_failure(f"{artist_name} - {song_title}", "youtube_music")
            return []
    
    def _log_failures(self, song_list: List[Dict], successful_song_ids: set, log_writer: BatchCrawlingLogger):
        """크롤링 실패한 곡들을 로그에 기록합니다."""
        for song_info in song_list:
            song_id = song_info.get('song_id')
            if song_id not in successful_song_ids:
                song_title = song_info.get('title_ko', '')
                artist_name = song_info.get('artist_ko', '')
                log_writer.add_crawling_failure(f"{artist_name} - {song_title}", "youtube_music") 