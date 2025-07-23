"""
크롤링 결과 요약 유틸리티
"""
import logging
from datetime import datetime
from typing import Dict, Any, List
from .slack_notifier import send_slack_message

logger = logging.getLogger(__name__)

class CrawlingResultSummary:
    """크롤링 결과 요약 클래스"""
    
    def __init__(self, song_info: Dict[str, Any], start_time: float):
        """
        Args:
            song_info (dict): 곡 정보 {'song_id', 'artist_ko', 'title_ko'}
            start_time (float): 크롤링 시작 시간 (time.time())
        """
        self.song_info = song_info
        self.start_time = start_time
        self.platform_status = {}
        self.crawling_results = {}
        self.db_results = {}
        self.csv_results = {}
        
    def add_platform_result(self, platform: str, status: str, result: Any = None):
        """플랫폼별 결과 추가"""
        self.platform_status[platform] = status
        if result is not None:
            self.crawling_results[platform] = result
            
    def add_db_result(self, platform: str, result: Any):
        """DB 저장 결과 추가"""
        self.db_results[platform] = result
        
    def add_csv_result(self, platform: str, result: Any):
        """CSV 저장 결과 추가"""
        self.csv_results[platform] = result
        
    def generate_summary(self) -> Dict[str, Any]:
        """요약 정보 생성"""
        import time
        end_time = time.time()
        execution_time = end_time - self.start_time
        
        # 통계 계산
        success_count = sum(1 for status in self.platform_status.values() if status == 'success')
        failed_count = sum(1 for status in self.platform_status.values() if status == 'failed')
        error_count = sum(1 for status in self.platform_status.values() if status == 'error')
        skipped_count = sum(1 for status in self.platform_status.values() if status == 'skipped')
        
        return {
            'status': 'success' if success_count > 0 else 'failed',
            'execution_time': f"{execution_time:.2f}초",
            'execution_time_seconds': execution_time,
            'platform_status': self.platform_status,
            'statistics': {
                'total_platforms': len(self.platform_status),
                'success': success_count,
                'failed': failed_count,
                'error': error_count,
                'skipped': skipped_count
            },
            'crawling_results': self.crawling_results,
            'db_results': self.db_results,
            'csv_results': self.csv_results
        }
        
    def print_summary(self):
        """콘솔에 요약 정보 출력"""
        summary = self.generate_summary()
        
        # 요약 메시지 생성
        summary_message = self._generate_summary_message(summary)
        
        # 로그로 출력
        logger.info("=" * 60)
        logger.info("📊 크롤링 결과 요약")
        logger.info("=" * 60)
        for line in summary_message.split('\n'):
            if line.strip():
                logger.info(line)
        logger.info("=" * 60)
        
        # 실패한 경우에만 Slack 메시지 전송
        stats = summary['statistics']
        has_failures = stats['failed'] > 0 or stats['error'] > 0
        
        if has_failures:
            logger.info("⚠️ 실패가 발생하여 Slack 알림을 전송합니다.")
            slack_message = self.generate_slack_message()
            send_slack_message(slack_message)
        else:
            logger.info("✅ 모든 플랫폼이 성공하여 Slack 알림을 생략합니다.")
        
    def generate_slack_message(self) -> str:
        """Slack 메시지용 텍스트 생성"""
        summary = self.generate_summary()
        
        # 기본 정보
        message = f"🎵 *{self.song_info['artist_ko']} - {self.song_info['title_ko']}*\n"
        message += f"⏱️ 실행 시간: {summary['execution_time']}\n\n"
        
        # 통계
        stats = summary['statistics']
        message += f"📊 *통계*\n"
        message += f"• 성공: {stats['success']}개\n"
        message += f"• 실패: {stats['failed']}개\n"
        message += f"• 오류: {stats['error']}개\n"
        message += f"• 건너뜀: {stats['skipped']}개\n\n"
        
        # 플랫폼별 결과
        message += f"🔍 *플랫폼별 결과*\n"
        for plat, status in self.platform_status.items():
            status_emoji = "✅" if status == 'success' else "❌" if status == 'failed' else "⚠️" if status == 'skipped' else "💥"
            message += f"{status_emoji} {plat.upper()}: {status}\n"
        
        # 최종 상태
        if stats['success'] > 0:
            message += f"\n✅ 크롤링 완료"
        else:
            message += f"\n❌ 크롤링 실패 (모든 플랫폼 실패)"
            
        return message
        
    def generate_detailed_report(self) -> Dict[str, Any]:
        """상세 리포트 생성 (Slack 첨부파일용)"""
        summary = self.generate_summary()
        
        report = {
            'timestamp': datetime.now().isoformat(),
            'song_info': self.song_info,
            'summary': summary,
            'platform_details': {}
        }
        
        # 플랫폼별 상세 정보
        for platform, status in self.platform_status.items():
            report['platform_details'][platform] = {
                'status': status,
                'crawling_result': self.crawling_results.get(platform),
                'db_result': self.db_results.get(platform),
                'csv_result': self.csv_results.get(platform)
            }
            
        return report
    
    def _generate_summary_message(self, summary: Dict[str, Any]) -> str:
        """요약 메시지 생성 (로그용)"""
        lines = []
        
        lines.append(f"🎵 곡: {self.song_info['artist_ko']} - {self.song_info['title_ko']}")
        lines.append(f"⏱️  실행 시간: {summary['execution_time']}")
        lines.append(f"📈 성공: {summary['statistics']['success']}개 플랫폼, 실패: {summary['statistics']['failed']}개 플랫폼, "
                    f"오류: {summary['statistics']['error']}개 플랫폼, 건너뜀: {summary['statistics']['skipped']}개 플랫폼")
        
        # 플랫폼별 결과
        for plat, status in self.platform_status.items():
            status_emoji = "✅" if status == 'success' else "❌" if status == 'failed' else "⚠️" if status == 'skipped' else "💥"
            lines.append(f"{status_emoji} {plat.upper()}: {status}")
        
        if summary['statistics']['success'] > 0:
            lines.append("✅ 크롤링 완료")
        else:
            lines.append("❌ 크롤링 실패 (모든 플랫폼 실패)")
            
        return '\n'.join(lines)


def create_summary_logger(song_info: Dict[str, Any]) -> CrawlingResultSummary:
    """크롤링 결과 요약 로거 생성"""
    import time
    return CrawlingResultSummary(song_info, time.time())


def format_execution_time(seconds: float) -> str:
    """실행 시간을 읽기 쉬운 형태로 포맷"""
    if seconds < 60:
        return f"{seconds:.2f}초"
    elif seconds < 3600:
        minutes = int(seconds // 60)
        remaining_seconds = seconds % 60
        return f"{minutes}분 {remaining_seconds:.1f}초"
    else:
        hours = int(seconds // 3600)
        remaining_minutes = int((seconds % 3600) // 60)
        remaining_seconds = seconds % 60
        return f"{hours}시간 {remaining_minutes}분 {remaining_seconds:.1f}초" 