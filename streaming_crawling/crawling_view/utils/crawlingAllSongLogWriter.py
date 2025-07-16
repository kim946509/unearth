"""
크롤링 전체 과정 로그 관리자
실패한 곡을 추적하고 최종 요약 로그를 생성
"""
import logging
from datetime import datetime
from typing import Dict, List, Any
from collections import defaultdict

logger = logging.getLogger(__name__)

class CrawlingAllSongLogWriter:
    """
    크롤링 전체 과정을 추적하고 최종 요약 로그를 생성하는 클래스
    """
    
    def __init__(self):
        self.start_time = None
        self.end_time = None
        self.target_date = None
        self.total_songs = 0
        
        # 실패한 곡 추적: {노래명: [실패위치1, 실패위치2, ...]}
        self.failed_songs = defaultdict(list)
        
        # 플랫폼별 성공/실패 카운트
        self.platform_stats = defaultdict(lambda: {
            'success': 0,
            'failed': 0,
            'db_saved': 0,
            'db_failed': 0,
            'csv_saved': 0,
            'csv_failed': 0
        })
        
        # 전체 통계
        self.total_stats = {
            'success': 0,
            'failed': 0,
            'db_saved': 0,
            'db_failed': 0,
            'csv_saved': 0,
            'csv_failed': 0
        }
    
    def start_crawling(self, target_date, total_songs):
        """크롤링 시작"""
        self.start_time = datetime.now()
        self.target_date = target_date
        self.total_songs = total_songs
        
        logger.info("🚀 크롤링 프로세스 시작")
        logger.info(f"📅 대상 날짜: {target_date}")
        logger.info(f"🎵 총 크롤링 대상: {total_songs}곡")
    
    def add_crawling_failure(self, song_name: str, platform: str, error_message: str = ""):
        """크롤링 실패 기록"""
        failure_info = f"{platform}_크롤링"
        if error_message:
            failure_info += f"({error_message})"
        
        self.failed_songs[song_name].append(failure_info)
        self.platform_stats[platform]['failed'] += 1
        self.total_stats['failed'] += 1
    
    def add_crawling_success(self, song_name: str, platform: str):
        """크롤링 성공 기록"""
        self.platform_stats[platform]['success'] += 1
        self.total_stats['success'] += 1
    
    def add_db_failure(self, song_name: str, platform: str, error_message: str = ""):
        """DB 저장 실패 기록"""
        failure_info = f"{platform}_DB저장"
        if error_message:
            failure_info += f"({error_message})"
        
        self.failed_songs[song_name].append(failure_info)
        self.platform_stats[platform]['db_failed'] += 1
        self.total_stats['db_failed'] += 1
    
    def add_db_success(self, song_name: str, platform: str):
        """DB 저장 성공 기록"""
        self.platform_stats[platform]['db_saved'] += 1
        self.total_stats['db_saved'] += 1
    
    def add_csv_failure(self, song_name: str, platform: str, error_message: str = ""):
        """CSV 저장 실패 기록"""
        failure_info = f"{platform}_CSV저장"
        if error_message:
            failure_info += f"({error_message})"
        
        self.failed_songs[song_name].append(failure_info)
        self.platform_stats[platform]['csv_failed'] += 1
        self.total_stats['csv_failed'] += 1
    
    def add_csv_success(self, song_name: str, platform: str):
        """CSV 저장 성공 기록"""
        self.platform_stats[platform]['csv_saved'] += 1
        self.total_stats['csv_saved'] += 1
    
    def end_crawling(self):
        """크롤링 종료 및 최종 요약 로그 생성"""
        self.end_time = datetime.now()
        self._generate_final_summary()
    
    def _generate_final_summary(self):
        """최종 요약 로그 생성"""
        elapsed_time = (self.end_time - self.start_time).total_seconds()
        
        logger.info("=" * 80)
        logger.info("📊 크롤링 최종 결과 요약")
        logger.info("=" * 80)
        
        # 기본 정보
        logger.info(f"📅 크롤링 날짜: {self.target_date}")
        logger.info(f"⏰ 시작 시간: {self.start_time.strftime('%Y-%m-%d %H:%M:%S')}")
        logger.info(f"⏰ 종료 시간: {self.end_time.strftime('%Y-%m-%d %H:%M:%S')}")
        logger.info(f"⏱️  총 소요 시간: {elapsed_time:.2f}초 ({elapsed_time/60:.2f}분)")
        logger.info("")
        
        # 전체 통계 (성공 = 전체 - 실패)
        total_success = self.total_songs - len(self.failed_songs)
        logger.info("📈 전체 통계")
        logger.info(f"   총 곡 수: {self.total_songs}곡")
        logger.info(f"   성공한 곡 수: {total_success}곡")
        logger.info(f"   실패한 곡 수: {len(self.failed_songs)}곡")
        logger.info("")
        
        # 플랫폼별 통계 (성공 = 전체 - 실패)
        logger.info("🎵 플랫폼별 통계")
        for platform, stats in self.platform_stats.items():
            if stats['failed'] > 0 or stats['db_failed'] > 0 or stats['csv_failed'] > 0:
                platform_success = self.total_songs - stats['failed']
                logger.info(f"   {platform.upper()}:")
                logger.info(f"     크롤링 성공: {platform_success}곡")
                logger.info(f"     크롤링 실패: {stats['failed']}곡")
                logger.info(f"     DB 저장 실패: {stats['db_failed']}곡")
                logger.info(f"     CSV 저장 실패: {stats['csv_failed']}곡")
        logger.info("")
        
        # 실패 분석
        if self.failed_songs:
            logger.info("❌ 실패한 곡 상세 분석")
            
            # 실패 유형별 카운트
            failure_types = defaultdict(int)
            for song_name, failures in self.failed_songs.items():
                for failure in failures:
                    failure_types[failure] += 1
            
            logger.info("   실패 유형별:")
            for failure_type, count in sorted(failure_types.items()):
                logger.info(f"     {failure_type}: {count}곡")
            
            logger.info("")
            logger.info("   실패한 곡 목록:")
            for song_name, failures in sorted(self.failed_songs.items()):
                failure_str = ", ".join(failures)
                logger.info(f"     {song_name}: {failure_str}")
        else:
            logger.info("✅ 모든 곡이 성공적으로 처리되었습니다!")
        
        logger.info("=" * 80)
    
    def get_summary_dict(self) -> Dict[str, Any]:
        """요약 정보를 딕셔너리로 반환"""
        elapsed_time = (self.end_time - self.start_time).total_seconds() if self.end_time else 0
        total_success = self.total_songs - len(self.failed_songs)
        
        return {
            'target_date': self.target_date,
            'start_time': self.start_time,
            'end_time': self.end_time,
            'elapsed_time': elapsed_time,
            'total_songs': self.total_songs,
            'success_count': total_success,
            'failed_count': len(self.failed_songs),
            'failed_songs': dict(self.failed_songs),
            'platform_stats': dict(self.platform_stats),
            'total_stats': self.total_stats
        } 