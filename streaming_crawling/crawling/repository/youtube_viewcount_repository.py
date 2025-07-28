"""
YouTube 조회수 DB 저장 레포지토리
"""
import logging
from datetime import date
from typing import Dict
from django.db import transaction
from crawling.models.youtube_video_viewcount import YoutubeVideoViewCount
from crawling.models.crawling_period import CrawlingPeriod

logger = logging.getLogger(__name__)

def save_youtube_viewcount_batch(viewcount_results: Dict[str, int], target_date: date):
    """
    YouTube 조회수를 일괄 저장/업데이트
    
    Args:
        viewcount_results (dict): {crawling_period_id: view_count} 매핑
        target_date (date): 조회수 수집 날짜
    """
    if not viewcount_results:
        logger.warning("⚠️ 저장할 조회수 데이터가 없습니다.")
        return
    
    logger.info(f"💾 YouTube 조회수 일괄 저장 시작: {len(viewcount_results)}개")
    
    try:
        with transaction.atomic():
            success_count = 0
            error_count = 0
            
            for crawling_period_id, view_count in viewcount_results.items():
                try:
                    # crawling_period_id로 CrawlingPeriod 객체 조회
                    crawling_period = CrawlingPeriod.objects.get(id=crawling_period_id)
                    
                    # update_or_create로 중복 방지 및 업데이트
                    obj, created = YoutubeVideoViewCount.objects.update_or_create(
                        crawling_period=crawling_period,
                        date=target_date,
                        defaults={'view_count': view_count}
                    )
                    
                    if created:
                        logger.debug(f"✅ 신규 저장: {crawling_period_id} - {view_count}")
                    else:
                        logger.debug(f"🔄 업데이트: {crawling_period_id} - {view_count}")
                    
                    success_count += 1
                    
                except CrawlingPeriod.DoesNotExist:
                    logger.error(f"❌ CrawlingPeriod를 찾을 수 없음: {crawling_period_id}")
                    error_count += 1
                except Exception as e:
                    logger.error(f"❌ 개별 저장 실패: {crawling_period_id} - {e}")
                    error_count += 1
            
            logger.info(f"✅ YouTube 조회수 일괄 저장 완료: 성공 {success_count}개, 실패 {error_count}개")
            
    except Exception as e:
        logger.error(f"❌ YouTube 조회수 일괄 저장 실패: {e}", exc_info=True) 