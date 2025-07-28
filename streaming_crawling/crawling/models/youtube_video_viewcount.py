"""
YouTube 동영상 조회수 관리 모델
"""
from django.db import models
from .base import BaseModel
from .crawling_period import CrawlingPeriod

class YoutubeVideoViewCount(BaseModel):
    """
    YouTube 동영상 조회수 관리 모델
    CrawlingPeriod와 1:N 관계
    """
    crawling_period = models.ForeignKey(
        CrawlingPeriod, 
        on_delete=models.CASCADE, 
        db_column='crawling_period_id',
        help_text="크롤링 기간 (crawling_period.id 참조)"
    )
    date = models.DateField(help_text="조회수 수집 날짜")
    view_count = models.IntegerField(default=-999, help_text="조회수 (-999: 실패/에러)")

    class Meta:
        db_table = 'youtube_video_viewcount'
        ordering = ['-date', '-created_at']
        unique_together = ('crawling_period', 'date')  # 한 기간/날짜에 하나만 저장
        
    def __str__(self):
        return f"Period {self.crawling_period_id}: {self.date} - {self.view_count} views" 