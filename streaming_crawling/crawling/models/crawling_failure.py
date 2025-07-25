from django.db import models
from .base import BaseModel


class CrawlingFailure(BaseModel):
    """크롤링 실패 곡 정보"""
    
    song_id = models.CharField(max_length=255, unique=True, verbose_name="음원 ID")
    failed_at = models.DateTimeField(auto_now=True, verbose_name="최근 실패 시점")
    failed_platforms = models.TextField(verbose_name="실패한 플랫폼 목록")  # 쉼표로 구분된 문자열
    
    class Meta:
        db_table = 'crawling_failure'
        verbose_name = '크롤링 실패 곡'
        verbose_name_plural = '크롤링 실패 곡들'
        
    def __str__(self):
        return f"실패 곡: {self.song_id} - {self.failed_platforms}"
    
    def get_failed_platforms_list(self):
        """실패한 플랫폼 목록을 리스트로 반환"""
        if not self.failed_platforms:
            return []
        return [platform.strip() for platform in self.failed_platforms.split(',')]
    
    def set_failed_platforms_list(self, platforms):
        """플랫폼 리스트를 문자열로 저장"""
        if not platforms:
            self.failed_platforms = ""
        else:
            # 중복 제거 및 정렬
            unique_platforms = sorted(set(platforms))
            self.failed_platforms = ','.join(unique_platforms) 