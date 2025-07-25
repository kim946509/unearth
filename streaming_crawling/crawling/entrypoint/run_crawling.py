"""
전체 크롤링 프로세스 실행 스크립트 (운영용)
"""
import sys
import os
import django
import logging
from datetime import datetime, date
from typing import Optional, Dict, Any


class CrawlingApplication:
    """크롤링 애플리케이션의 메인 클래스"""
    
    def __init__(self):
        self.logger = None
        self.command_parser = CommandLineParser()
        self.crawling_executor = None
    
    def initialize(self):
        """애플리케이션을 초기화합니다."""
        DjangoInitializer.initialize()
        self.logger = LoggingConfigurator.configure()
        self.crawling_executor = CrawlingExecutor(self.logger)
    
    def run(self):
        """크롤링 애플리케이션을 실행합니다."""
        try:
            # 명령행 인수 파싱
            args = self.command_parser.parse_arguments()
            
            # 날짜 파싱
            target_date = self.command_parser.parse_date(args.date)
            
            # 크롤링 실행
            result = self.crawling_executor.execute_full_crawling(target_date)
            
            # 종료 코드 설정
            ExitCodeManager.exit_with_result(result)
            
        except ValueError as e:
            self.logger.error(f"❌ {e}")
            sys.exit(1)
        except Exception as e:
            self.logger.error(f"❌ 예상치 못한 오류 발생: {e}", exc_info=True)
            sys.exit(1)


# Django 설정
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')
django.setup()

from crawling.managers.crawling_manager import run_crawling
from crawling.utils.constants import Platforms


class DjangoInitializer:
    """Django 설정 및 초기화를 담당하는 클래스"""
    
    @staticmethod
    def initialize():
        """Django 환경을 초기화합니다."""
        sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
        os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')
        django.setup()


class LoggingConfigurator:
    """로깅 설정을 담당하는 클래스"""
    
    @staticmethod
    def configure():
        """로깅 설정을 초기화합니다."""
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler(f'logs/crawling_{datetime.now().strftime("%Y%m%d_%H%M%S")}.log', encoding='utf-8'),
                logging.StreamHandler()
            ]
        )
        return logging.getLogger(__name__)


class CommandLineParser:
    """명령행 인수 처리를 담당하는 클래스"""
    
    def __init__(self):
        self.parser = self._create_parser()
    
    def _create_parser(self):
        """ArgumentParser를 생성합니다."""
        import argparse
        parser = argparse.ArgumentParser(description='크롤링 프로세스 실행')
        parser.add_argument('--date', type=str, help='크롤링 대상 날짜 (YYYY-MM-DD 형식)')
        return parser
    
    def parse_arguments(self):
        """명령행 인수를 파싱합니다."""
        return self.parser.parse_args()
    
    def parse_date(self, date_str: Optional[str]) -> Optional[date]:
        """날짜 문자열을 파싱합니다."""
        if not date_str:
            return None
            
        try:
            return datetime.strptime(date_str, '%Y-%m-%d').date()
        except ValueError:
            raise ValueError("날짜 형식이 올바르지 않습니다. YYYY-MM-DD 형식을 사용하세요.")


class CrawlingExecutor:
    """크롤링 실행을 담당하는 클래스"""
    
    def __init__(self, logger):
        self.logger = logger
    
    def execute_full_crawling(self, target_date: Optional[date] = None) -> Dict[str, Any]:
        """
        전체 크롤링 프로세스를 실행합니다.
        
        Args:
            target_date: 크롤링 대상 날짜. None이면 오늘 날짜
            
        Returns:
            크롤링 결과 딕셔너리
        """
        try:
            result = run_crawling(target_date)
            
            if result.get('status') == 'success' and 'log_summary' in result:
                return result['log_summary']
            else:
                return result
                
        except Exception as e:
            self.logger.error(f"❌ 크롤링 프로세스 실행 중 오류 발생: {e}", exc_info=True)
            return self._create_error_result(str(e), target_date)
    
    def _create_error_result(self, error_message: str, target_date: Optional[date] = None) -> Dict[str, Any]:
        """에러 결과를 생성합니다."""
        error_result = {
            'status': 'error',
            'error_message': error_message,
            'start_time': datetime.now(),
            'end_time': datetime.now(),
            'elapsed_time': 0,
            'target_date': target_date or date.today()
        }
        
        return error_result


class ExitCodeManager:
    """종료 코드 관리를 담당하는 클래스"""
    
    @staticmethod
    def exit_with_result(result: Dict[str, Any]):
        """크롤링 결과에 따라 적절한 종료 코드로 종료합니다."""
        if result.get('status') == 'success':
            sys.exit(0)
        else:
            sys.exit(1)



def run_full_crawling(target_date=None):
    """
    전체 크롤링 프로세스 실행 (운영용) - 기존 호환성을 위한 함수
    
    Args:
        target_date (date, optional): 크롤링 대상 날짜. None이면 오늘 날짜
        
    Returns:
        dict: 상세한 크롤링 결과
    """
    app = CrawlingApplication()
    app.initialize()
    return app.crawling_executor.execute_full_crawling(target_date)


if __name__ == "__main__":
    app = CrawlingApplication()
    app.initialize()
    app.run() 