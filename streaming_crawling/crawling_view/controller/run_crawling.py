"""
전체 크롤링 프로세스 실행 스크립트 (운영용)
"""
import sys
import os
import django
import logging
from datetime import datetime, date

# Django 설정
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')
django.setup()

from crawling_view.controller.crawling_manager import run_crawling, run_platform_crawling
from crawling_view.utils.constants import Platforms

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(f'logs/crawling_{datetime.now().strftime("%Y%m%d_%H%M%S")}.log', encoding='utf-8'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

def run_full_crawling(target_date=None):
    """
    전체 크롤링 프로세스 실행 (운영용)
    
    Args:
        target_date (date, optional): 크롤링 대상 날짜. None이면 오늘 날짜
        
    Returns:
        dict: 상세한 크롤링 결과
    """
    try:
        # 크롤링 실행 (새로운 로그 시스템 사용)
        result = run_crawling(target_date)
        
        # 새로운 로그 시스템의 요약 정보가 있으면 사용
        if result.get('status') == 'success' and 'log_summary' in result:
            return result['log_summary']
        else:
            return result
        
    except Exception as e:
        logger.error(f"❌ 크롤링 프로세스 실행 중 오류 발생: {e}", exc_info=True)
        
        error_result = {
            'status': 'error',
            'error_message': str(e),
            'start_time': datetime.now(),
            'end_time': datetime.now(),
            'elapsed_time': 0,
            'target_date': target_date or date.today()
        }
        
        return error_result



def run_single_platform_crawling(platform, target_date=None):
    """
    단일 플랫폼 크롤링 실행
    
    Args:
        platform (str): 플랫폼명 ('genie', 'youtube', 'youtube_music', 'melon')
        target_date (date, optional): 크롤링 대상 날짜
        
    Returns:
        dict: 크롤링 결과
    """
    try:
        # 크롤링 실행 (새로운 로그 시스템 사용)
        result = run_platform_crawling(platform, target_date)
        
        # 새로운 로그 시스템의 요약 정보가 있으면 사용
        if result.get('status') == 'success' and 'log_summary' in result:
            return result['log_summary']
        else:
            return result
        
    except Exception as e:
        logger.error(f"❌ {platform.upper()} 크롤링 실패: {e}", exc_info=True)
        
        return {'status': 'error', 'platform': platform, 'error_message': str(e)}

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description='크롤링 프로세스 실행')
    parser.add_argument('--platform', choices=Platforms.ALL_PLATFORMS, 
                       help='특정 플랫폼만 크롤링')
    parser.add_argument('--date', type=str, help='크롤링 대상 날짜 (YYYY-MM-DD 형식)')
    
    args = parser.parse_args()
    
    # 날짜 파싱
    target_date = None
    if args.date:
        try:
            target_date = datetime.strptime(args.date, '%Y-%m-%d').date()
        except ValueError:
            logger.error("❌ 날짜 형식이 올바르지 않습니다. YYYY-MM-DD 형식을 사용하세요.")
            sys.exit(1)
    
    # 크롤링 실행
    if args.platform:
        # 단일 플랫폼 크롤링
        result = run_single_platform_crawling(args.platform, target_date)
    else:
        # 전체 크롤링
        result = run_full_crawling(target_date)
    
    # 종료 코드 설정
    if result.get('status') == 'success':
        sys.exit(0)
    else:
        sys.exit(1) 