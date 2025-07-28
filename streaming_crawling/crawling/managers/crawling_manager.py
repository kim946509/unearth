"""
크롤링 매니저 - 전체 크롤링 프로세스 조율
"""
import logging
from datetime import date
from typing import Dict, List, Any, Optional

from crawling.repository.song_service import SongService
from crawling.utils.batch_crawling_logger import BatchCrawlingLogger
from crawling.repository.failure_service import FailureService
from crawling.repository.db_writer import save_all_platforms_for_songs

# 플랫폼 전략들 import (새로운 구조)
from crawling.service.genie import GenieCrawlingStrategy
from crawling.service.youtube import YouTubeCrawlingStrategy
from crawling.service.youtube_music import YouTubeMusicCrawlingStrategy
from crawling.service.melon import MelonCrawlingStrategy

# YouTube 조회수 수집 서비스 import
from crawling.service.youtube.youtube_api_service import update_youtube_viewcounts_for_period

logger = logging.getLogger(__name__)


def songinfo_to_dict(song):
    return {
        'song_id': song.id,
        'title_ko': song.title_ko,
        'title_en': getattr(song, 'title_en', ''),
        'artist_ko': song.artist_ko,
        'artist_en': getattr(song, 'artist_en', ''),
        'youtube_url': getattr(song, 'youtube_url', ''),
        'melon_song_id': getattr(song, 'melon_song_id', ''),
        # 필요에 따라 추가 필드 작성
    }

class CrawlingManager:
    """크롤링 전체 프로세스를 조율하는 매니저 클래스"""
    
    def __init__(self):
        self.log_writer = BatchCrawlingLogger()
        # 각 플랫폼 전략 인스턴스 생성
        self.platform_strategies = {
            'genie': GenieCrawlingStrategy(),
            'youtube': YouTubeCrawlingStrategy(),
            'youtube_music': YouTubeMusicCrawlingStrategy(),
            'melon': MelonCrawlingStrategy()
        }
    
    def run_full_crawling(self, target_date: Optional[date] = None) -> Dict[str, Any]:
        """
        전체 크롤링 프로세스를 실행합니다.
        
        Args:
            target_date: 크롤링 대상 날짜. None이면 오늘 날짜
            
        Returns:
            크롤링 결과 요약
        """
        try:
            logger.info("🚀 전체 크롤링 프로세스 시작")
            
            # 1단계: 크롤링 대상 노래 조회
            active_songs = SongService.get_active_songs(target_date)
            
            if not active_songs:
                logger.warning("⚠️ 크롤링 대상 노래가 없습니다.")
                return {'status': 'no_songs', 'message': '크롤링 대상 노래가 없습니다.'}
            
            logger.info(f"📋 크롤링 대상: {len(active_songs)}개 곡")
            
            # 로그 라이터 시작
            self.log_writer.start_crawling(target_date or date.today(), len(active_songs))
            
            # 2단계: 플랫폼별 크롤링 실행
            crawling_results = {}
            
            for platform_name, strategy in self.platform_strategies.items():
                platform_songs = SongService.get_songs_by_platform(active_songs, platform_name)
                # SongInfo 객체 리스트를 dict 리스트로 변환
                platform_songs_dict = [songinfo_to_dict(song) for song in platform_songs]
                
                if platform_songs_dict:
                    logger.info(f"🎯 {platform_name} 크롤링 시작: {len(platform_songs_dict)}개 곡")
                    results = strategy.crawl_platform(platform_songs_dict, self.log_writer)
                    crawling_results[platform_name] = results
                else:
                    logger.info(f"⚠️ {platform_name} 크롤링 대상 곡이 없습니다.")
            
            # 3단계: DB 저장
            logger.info("💾 크롤링 결과를 데이터베이스에 저장 중...")
            all_song_ids = [song.id for song in active_songs]
            db_results = save_all_platforms_for_songs(
                song_ids=all_song_ids,
                genie_results=crawling_results.get('genie'),
                youtube_music_results=crawling_results.get('youtube_music'),
                youtube_results=crawling_results.get('youtube'),
                melon_results=crawling_results.get('melon')
            )
            
            # 4단계: 실패 처리
            logger.info("🔍 실패 처리 중...")
            for song in active_songs:
                FailureService.check_and_handle_failures(song.id, target_date)
            
            # 로그 라이터 종료
            self.log_writer.end_crawling()
            
            # YouTube 조회수 수집 (후처리)
            logger.info("🎥 YouTube 조회수 수집 시작 (후처리)")
            try:
                update_youtube_viewcounts_for_period(
                    start_date=target_date or date.today(),
                    end_date=target_date or date.today(),
                    target_date=target_date or date.today()
                )
                logger.info("✅ YouTube 조회수 수집 완료")
            except Exception as e:
                logger.error(f"❌ YouTube 조회수 수집 실패: {e}")
                # YouTube 조회수 수집 실패는 전체 크롤링 실패로 처리하지 않음
            
            # 결과 요약
            summary = self._create_summary(target_date, active_songs, crawling_results, db_results)
            
            logger.info("✅ 전체 크롤링 프로세스 완료")
            return summary
            
        except Exception as e:
            logger.error(f"❌ 크롤링 프로세스 실패: {e}", exc_info=True)
            return {'status': 'error', 'message': str(e)}
    
    def _create_summary(self, target_date: Optional[date], active_songs: List, crawling_results: Dict, db_results: Dict) -> Dict[str, Any]:
        """전체 크롤링 결과 요약을 생성합니다."""
        return {
            'status': 'success',
            'target_date': target_date or date.today(),
            'total_songs': len(active_songs),
            'crawling_results': crawling_results,
            'db_results': db_results,
            'log_summary': self.log_writer.get_summary_dict()
        }


# 싱글톤 인스턴스
_crawling_manager = None

def get_crawling_manager() -> CrawlingManager:
    """크롤링 매니저 싱글톤 인스턴스를 반환합니다."""
    global _crawling_manager
    if _crawling_manager is None:
        _crawling_manager = CrawlingManager()
    return _crawling_manager


# 기존 함수 (호환성을 위해 유지)
def run_crawling(target_date=None):
    """크롤링 전체 프로세스 실행 (기존 호환성)"""
    manager = get_crawling_manager()
    return manager.run_full_crawling(target_date) 