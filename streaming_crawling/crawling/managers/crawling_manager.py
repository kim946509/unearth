"""
크롤링 매니저 - 객체지향 설계로 리팩토링
크롤링 전체 흐름을 4단계로 단순화:
1. 크롤링 대상 노래 조회
2. 크롤링 실행
3. DB 저장
4. CSV 저장
"""
import logging
from datetime import date
from typing import Dict, List, Any, Optional
from abc import ABC, abstractmethod

from crawling.repository.song_service import SongService
from crawling.managers.platform_crawlers import create_crawler
from crawling.utils.batch_crawling_logger import BatchCrawlingLogger
from crawling.repository.failure_service import FailureService
from crawling.repository.db_writer import save_all_platforms_for_songs

logger = logging.getLogger(__name__)


class PlatformCrawlingStrategy(ABC):
    """플랫폼별 크롤링 전략을 정의하는 추상 클래스"""
    
    def __init__(self, platform_name: str):
        self.platform_name = platform_name
    
    @abstractmethod
    def crawl_platform(self, songs: List, log_writer: BatchCrawlingLogger) -> Optional[Any]:
        """플랫폼별 크롤링을 실행합니다."""
        pass
    
    def _log_failures(self, songs: List, successful_song_ids: set, log_writer: BatchCrawlingLogger):
        """크롤링 실패한 곡들을 로그에 기록합니다."""
        for song in songs:
            if song.id not in successful_song_ids:
                song_name = f"{song.artist_ko} - {song.title_ko}"
                log_writer.add_crawling_failure(song_name, self.platform_name)


class GenieCrawlingStrategy(PlatformCrawlingStrategy):
    """Genie 플랫폼 크롤링 전략"""
    
    def __init__(self):
        super().__init__('genie')
    
    def crawl_platform(self, songs: List, log_writer: BatchCrawlingLogger) -> Optional[List[Dict]]:
        if not songs:
            return None
            
        crawler = create_crawler('genie')
        crawling_data = SongService.convert_to_crawling_format(songs, 'genie')
        results = crawler.crawl_songs(crawling_data)
        
        # 실패한 곡 기록
        successful_song_ids = {result.get('song_id') for result in results}
        self._log_failures(songs, successful_song_ids, log_writer)
        
        return results


class YouTubeMusicCrawlingStrategy(PlatformCrawlingStrategy):
    """YouTube Music 플랫폼 크롤링 전략"""
    
    def __init__(self):
        super().__init__('youtube_music')
    
    def crawl_platform(self, songs: List, log_writer: BatchCrawlingLogger) -> Optional[List[Dict]]:
        if not songs:
            return None
            
        crawler = create_crawler('youtube_music')
        crawling_data = SongService.convert_to_crawling_format(songs, 'youtube_music')
        results = crawler.crawl_songs(crawling_data)
        
        # 실패한 곡 기록
        successful_song_ids = {result.get('song_id') for result in results}
        self._log_failures(songs, successful_song_ids, log_writer)
        
        return results


class YouTubeCrawlingStrategy(PlatformCrawlingStrategy):
    """YouTube 플랫폼 크롤링 전략"""
    
    def __init__(self):
        super().__init__('youtube')
    
    def crawl_platform(self, songs: List, log_writer: BatchCrawlingLogger) -> Optional[Dict]:
        if not songs:
            return None
            
        crawler = create_crawler('youtube')
        crawling_data = SongService.convert_to_crawling_format(songs, 'youtube')
        results = crawler.crawl_songs(crawling_data)
        
        # 실패한 곡 기록 (YouTube는 딕셔너리 형태)
        successful_song_ids = set(results.keys())
        self._log_failures(songs, successful_song_ids, log_writer)
        
        return results


class MelonCrawlingStrategy(PlatformCrawlingStrategy):
    """Melon 플랫폼 크롤링 전략"""
    
    def __init__(self):
        super().__init__('melon')
    
    def crawl_platform(self, songs: List, log_writer: BatchCrawlingLogger) -> Optional[List[Dict]]:
        if not songs:
            return None
            
        crawler = create_crawler('melon')
        crawling_data = SongService.convert_to_crawling_format(songs, 'melon')
        results = crawler.crawl_songs(crawling_data)
        
        # 실패한 곡 기록
        successful_song_ids = {result.get('song_id') for result in results}
        self._log_failures(songs, successful_song_ids, log_writer)
        
        return results


class CrawlingDataProcessor:
    """크롤링 데이터 처리 및 저장을 담당하는 클래스"""
    
    def __init__(self, log_writer: BatchCrawlingLogger):
        self.log_writer = log_writer
    
    def save_to_database(self, all_song_ids: List[int], crawling_results: Dict[str, Any]) -> Dict[str, Any]:
        """크롤링 결과를 데이터베이스에 저장합니다."""
        db_results = save_all_platforms_for_songs(
            song_ids=all_song_ids,
            genie_results=crawling_results.get('genie'),
            youtube_music_results=crawling_results.get('youtube_music'),
            youtube_results=crawling_results.get('youtube'),
            melon_results=crawling_results.get('melon')
        )
        
        # DB 저장 실패 기록
        if db_results.get('total_saved', 0) == 0 and db_results.get('total_updated', 0) == 0:
            self._log_db_failures(all_song_ids)
        
        return db_results
    
    def _log_db_failures(self, song_ids: List[int]):
        """DB 저장 실패를 로그에 기록합니다."""
        for song_id in song_ids:
            # SongService에서 곡 정보를 가져와서 로그에 기록
            songs = SongService.get_songs_by_ids([song_id])
            if songs:
                song = songs[0]
                song_name = f"{song.artist_ko} - {song.title_ko}"
                self.log_writer.add_db_failure(song_name, 'all_platforms')
    
    def _log_failures(self, songs: List, successful_song_ids: set, log_writer: BatchCrawlingLogger):
        """크롤링 실패한 곡들을 로그에 기록합니다."""
        for song in songs:
            if song.id not in successful_song_ids:
                song_name = f"{song.artist_ko} - {song.title_ko}"
                log_writer.add_crawling_failure(song_name, 'all_platforms')


class CrawlingManager:
    """크롤링 전체 프로세스를 관리하는 매니저 클래스"""
    
    def __init__(self):
        self.log_writer = BatchCrawlingLogger()
        self.data_processor = CrawlingDataProcessor(self.log_writer)
        self.platform_strategies = {
            'genie': GenieCrawlingStrategy(),
            'youtube_music': YouTubeMusicCrawlingStrategy(),
            'youtube': YouTubeCrawlingStrategy(),
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
            # 1단계: 크롤링 대상 노래 조회
            active_songs = SongService.get_active_songs(target_date)
            
            if not active_songs:
                logger.warning("⚠️ 크롤링 대상 노래가 없습니다.")
                return {'status': 'no_songs', 'message': '크롤링 대상 노래가 없습니다.'}
            
            # 로그 라이터 시작
            self.log_writer.start_crawling(target_date or date.today(), len(active_songs))
            
            # 2단계: 플랫폼별 크롤링 실행
            crawling_results = {}
            platform_songs = {}
            
            for platform_name, strategy in self.platform_strategies.items():
                platform_songs_list = SongService.get_songs_by_platform(active_songs, platform_name)
                platform_songs[platform_name] = platform_songs_list
                
                if platform_songs_list:
                    results = strategy.crawl_platform(platform_songs_list, self.log_writer)
                    crawling_results[platform_name] = results
            
            # 3단계: DB 저장
            all_song_ids = [song.id for song in active_songs]
            db_results = self.data_processor.save_to_database(all_song_ids, crawling_results)
            
            # 실패 처리
            self._handle_failures(active_songs, target_date)
            
            # 로그 라이터 종료
            self.log_writer.end_crawling()
            
            # 결과 요약
            return self._create_summary(target_date, active_songs, crawling_results, db_results)
            
        except Exception as e:
            logger.error(f"❌ 크롤링 프로세스 실패: {e}", exc_info=True)
            return {'status': 'error', 'message': str(e)}
    
    def run_single_platform_crawling(self, platform: str, target_date: Optional[date] = None) -> Dict[str, Any]:
        """
        특정 플랫폼만 크롤링을 실행합니다.
        
        Args:
            platform: 크롤링할 플랫폼
            target_date: 크롤링 대상 날짜
            
        Returns:
            크롤링 결과
        """
        try:
            # 1단계: 크롤링 대상 노래 조회
            active_songs = SongService.get_active_songs(target_date)
            platform_songs = SongService.get_songs_by_platform(active_songs, platform)
            
            if not platform_songs:
                logger.warning(f"⚠️ {platform} 크롤링 대상 노래가 없습니다.")
                return {'status': 'no_songs', 'platform': platform}
            
            # 로그 라이터 시작
            self.log_writer.start_crawling(target_date or date.today(), len(platform_songs))
            
            # 2단계: 크롤링 실행
            strategy = self.platform_strategies.get(platform)
            if not strategy:
                return {'status': 'error', 'platform': platform, 'message': '지원하지 않는 플랫폼'}
            
            crawling_results = strategy.crawl_platform(platform_songs, self.log_writer)
            
            # 3단계: DB 저장
            all_song_ids = [song.id for song in platform_songs]
            db_results = self._save_single_platform_to_db(platform, all_song_ids, crawling_results)
            
            # 실패 처리
            self._handle_failures(platform_songs, target_date)
            
            # 로그 라이터 종료
            self.log_writer.end_crawling()
            
            # 결과 요약
            return self._create_single_platform_summary(platform, target_date, platform_songs, crawling_results, db_results)
            
        except Exception as e:
            logger.error(f"❌ {platform} 크롤링 실패: {e}", exc_info=True)
            return {'status': 'error', 'platform': platform, 'message': str(e)}
    
    def _save_single_platform_to_db(self, platform: str, song_ids: List[int], crawling_results: Any) -> Dict[str, Any]:
        """단일 플랫폼 결과를 DB에 저장합니다."""
        if platform == 'genie':
            return save_all_platforms_for_songs(song_ids=song_ids, genie_results=crawling_results)
        elif platform == 'youtube_music':
            return save_all_platforms_for_songs(song_ids=song_ids, youtube_music_results=crawling_results)
        elif platform == 'youtube':
            return save_all_platforms_for_songs(song_ids=song_ids, youtube_results=crawling_results)
        elif platform == 'melon':
            return save_all_platforms_for_songs(song_ids=song_ids, melon_results=crawling_results)
        else:
            return {'error': '지원하지 않는 플랫폼'}
    
    def _handle_failures(self, songs: List, target_date: Optional[date]):
        """실패 처리를 수행합니다."""
        for song in songs:
            FailureService.check_and_handle_failures(song.id, target_date)
    
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
    
    def _create_single_platform_summary(self, platform: str, target_date: Optional[date], platform_songs: List, crawling_results: Any, db_results: Dict) -> Dict[str, Any]:
        """단일 플랫폼 크롤링 결과 요약을 생성합니다."""
        # DB 결과에서 해당 플랫폼 결과만 추출
        platform_db_result = {}
        platform_key = platform.lower()
        if platform_key in db_results:
            platform_db_result = db_results[platform_key]
        else:
            platform_db_result = db_results
        
        return {
            'status': 'success',
            'platform': platform,
            'target_date': target_date or date.today(),
            'total_songs': len(platform_songs),
            'crawling_results': crawling_results,
            'db_results': platform_db_result,
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


# 기존 함수들 (호환성을 위해 유지)
def run_crawling(target_date=None):
    """크롤링 전체 프로세스 실행 (기존 호환성)"""
    manager = get_crawling_manager()
    return manager.run_full_crawling(target_date)


def run_platform_crawling(platform, target_date=None):
    """특정 플랫폼만 크롤링 실행 (기존 호환성)"""
    manager = get_crawling_manager()
    return manager.run_single_platform_crawling(platform, target_date) 