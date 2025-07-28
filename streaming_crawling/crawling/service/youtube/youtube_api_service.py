"""
YouTube Data API 서비스
"""
import os
import logging
import requests
from typing import List, Dict, Optional
from datetime import date
from dotenv import load_dotenv

from .id_extractor import extract_youtube_id, validate_youtube_id
from crawling.repository.youtube_viewcount_repository import save_youtube_viewcount_batch
from crawling.models import CrawlingPeriod

# .env 파일 로드
load_dotenv()

logger = logging.getLogger(__name__)

class YouTubeApiService:
    """YouTube Data API를 사용한 조회수 수집 서비스"""
    
    def __init__(self):
        self.api_key = os.getenv('YOUTUBE_API_KEY', '')
        if not self.api_key:
            logger.error("❌ YOUTUBE_API_KEY 환경변수가 설정되지 않았습니다.")
            raise ValueError("YOUTUBE_API_KEY 환경변수가 필요합니다.")
        
        self.api_base_url = "https://www.googleapis.com/youtube/v3/videos"
        self.batch_size = 50  # YouTube API 최대 50개 ID까지 한 번에 조회 가능
    
    def _get_video_statistics_batch(self, video_ids: List[str]) -> Dict[str, Dict]:
        """
        YouTube API를 사용하여 비디오 통계 정보를 배치로 가져오기
        
        Args:
            video_ids (list): YouTube 비디오 ID 리스트
            
        Returns:
            dict: {video_id: {'title': str, 'view_count': int, 'published_at': str}} 형태
        """
        results = {}
        
        # batch 단위로 나누어 처리
        for i in range(0, len(video_ids), self.batch_size):
            batch_video_ids = video_ids[i:i + self.batch_size]
            
            logger.info(f"🔄 Batch {i//self.batch_size + 1}: {len(batch_video_ids)}개 동영상 조회")
            
            # API 호출
            batch_results = self._call_youtube_api_with_details(batch_video_ids)
            results.update(batch_results)
        
        return results
    
    def _call_youtube_api_with_details(self, video_ids: List[str]) -> Dict[str, Dict]:
        """
        YouTube Data API 호출 (제목, 조회수, 업로드 날짜 포함)
        
        Args:
            video_ids (list): 동영상 ID 리스트 (최대 50개)
            
        Returns:
            dict: {video_id: {'title': str, 'view_count': int, 'published_at': str}} 형태
        """
        try:
            # API 요청 파라미터 (제목, 통계, 날짜 정보 포함)
            params = {
                'part': 'snippet,statistics',
                'id': ','.join(video_ids),
                'key': self.api_key
            }
            
            # API 호출
            response = requests.get(self.api_base_url, params=params, timeout=30)
            
            if response.status_code != 200:
                logger.error(f"❌ YouTube API 호출 실패: HTTP {response.status_code}")
                return {video_id: {'title': '제목 없음', 'view_count': -999, 'published_at': None} for video_id in video_ids}
            
            # JSON 파싱
            data = response.json()
            
            # 결과 추출
            results = {}
            found_video_ids = set()
            
            for item in data.get('items', []):
                video_id = item.get('id')
                snippet = item.get('snippet', {})
                statistics = item.get('statistics', {})
                
                # 제목 추출
                title = snippet.get('title', '제목 없음')
                
                # 조회수 추출
                view_count_str = statistics.get('viewCount', '0')
                try:
                    view_count = int(view_count_str)
                except ValueError:
                    logger.warning(f"⚠️ 조회수 변환 실패: {video_id} - {view_count_str}")
                    view_count = -999
                
                # 업로드 날짜 추출
                published_at = snippet.get('publishedAt')
                
                results[video_id] = {
                    'title': title,
                    'view_count': view_count,
                    'published_at': published_at
                }
                found_video_ids.add(video_id)
                logger.debug(f"✅ {video_id}: {title} - {view_count:,} views")
            
            # 응답에 없는 동영상은 -999로 처리 (삭제된 동영상 등)
            for video_id in video_ids:
                if video_id not in found_video_ids:
                    results[video_id] = {
                        'title': '제목 없음',
                        'view_count': -999,
                        'published_at': None
                    }
                    logger.warning(f"⚠️ API 응답에 없는 동영상: {video_id}")
            
            logger.info(f"✅ API 호출 성공: {len(found_video_ids)}개 성공 / {len(video_ids)}개 중")
            return results
            
        except requests.exceptions.RequestException as e:
            logger.error(f"❌ YouTube API 네트워크 오류: {e}")
            return {video_id: {'title': '제목 없음', 'view_count': -999, 'published_at': None} for video_id in video_ids}
        except Exception as e:
            logger.error(f"❌ YouTube API 호출 중 오류: {e}")
            return {video_id: {'title': '제목 없음', 'view_count': -999, 'published_at': None} for video_id in video_ids}
    
    def update_youtube_viewcounts_for_period(self, start_date: date, end_date: date, target_date: date = None):
        """
        특정 기간의 CrawlingPeriod에서 YouTube 조회수를 수집하여 저장
        
        Args:
            start_date (date): 크롤링 기간 시작일
            end_date (date): 크롤링 기간 종료일
            target_date (date, optional): 조회수 수집 날짜 (None이면 오늘 날짜)
        """
        if target_date is None:
            target_date = date.today()
        
        logger.info(f"🎥 YouTube 조회수 수집 시작: {start_date} ~ {end_date} (수집일: {target_date})")
        
        try:
            # 1. 기간 내 CrawlingPeriod에서 YouTube URL이 있는 것만 조회
            periods_with_urls = self._get_periods_with_youtube_urls(start_date, end_date)
            
            if not periods_with_urls:
                logger.warning("⚠️ YouTube URL이 있는 CrawlingPeriod가 없습니다.")
                return
            
            logger.info(f"📋 YouTube URL이 있는 곡: {len(periods_with_urls)}개")
            
            # 2. URL에서 동영상 ID 추출
            period_id_to_video_id = {}
            for period in periods_with_urls:
                video_id = extract_youtube_id(period.youtube_url)
                if video_id and validate_youtube_id(video_id):
                    period_id_to_video_id[period.id] = video_id
                else:
                    logger.warning(f"❌ 유효하지 않은 YouTube URL: {period.youtube_url}")
            
            if not period_id_to_video_id:
                logger.warning("⚠️ 유효한 YouTube 동영상 ID가 없습니다.")
                return
            
            logger.info(f"🔍 유효한 동영상 ID: {len(period_id_to_video_id)}개")
            
            # 3. batch로 조회수 수집
            viewcount_results = self._collect_viewcounts_batch(period_id_to_video_id)
            
            # 4. DB에 저장
            save_youtube_viewcount_batch(viewcount_results, target_date)
            
            logger.info(f"✅ YouTube 조회수 수집 완료: {len(viewcount_results)}개 저장")
            
        except Exception as e:
            logger.error(f"❌ YouTube 조회수 수집 실패: {e}", exc_info=True)
    
    def _get_periods_with_youtube_urls(self, start_date: date, end_date: date) -> List:
        """기간 내 YouTube URL이 있는 CrawlingPeriod 조회"""
        try:
            periods = CrawlingPeriod.objects.filter(
                start_date__lte=end_date,
                end_date__gte=start_date,
                is_active=True,
                youtube_url__isnull=False
            ).exclude(youtube_url='')
            
            return list(periods)
            
        except Exception as e:
            logger.error(f"❌ CrawlingPeriod 조회 실패: {e}")
            return []
    
    def _collect_viewcounts_batch(self, period_id_to_video_id: Dict[str, str]) -> Dict[str, int]:
        """
        batch로 YouTube 조회수 수집
        
        Args:
            period_id_to_video_id (dict): {period_id: video_id} 매핑
            
        Returns:
            dict: {period_id: view_count} 매핑 (실패 시 -999)
        """
        results = {}
        video_ids = list(period_id_to_video_id.values())
        period_ids = list(period_id_to_video_id.keys())
        
        # batch 단위로 나누어 처리
        for i in range(0, len(video_ids), self.batch_size):
            batch_video_ids = video_ids[i:i + self.batch_size]
            batch_period_ids = period_ids[i:i + self.batch_size]
            
            logger.info(f"🔄 Batch {i//self.batch_size + 1}: {len(batch_video_ids)}개 동영상 조회")
            
            # API 호출
            api_results = self._call_youtube_api(batch_video_ids)
            
            # 결과 매핑
            for j, period_id in enumerate(batch_period_ids):
                video_id = batch_video_ids[j]
                view_count = api_results.get(video_id, -999)
                results[period_id] = view_count
        
        return results
    
    def _call_youtube_api(self, video_ids: List[str]) -> Dict[str, int]:
        """
        YouTube Data API 호출 (조회수만)
        
        Args:
            video_ids (list): 동영상 ID 리스트 (최대 50개)
            
        Returns:
            dict: {video_id: view_count} 매핑 (실패 시 -999)
        """
        try:
            # API 요청 파라미터
            params = {
                'part': 'statistics',
                'id': ','.join(video_ids),
                'key': self.api_key
            }
            
            # API 호출
            response = requests.get(self.api_base_url, params=params, timeout=30)
            
            if response.status_code != 200:
                logger.error(f"❌ YouTube API 호출 실패: HTTP {response.status_code}")
                return {video_id: -999 for video_id in video_ids}
            
            # JSON 파싱
            data = response.json()
            
            # 결과 추출
            results = {}
            found_video_ids = set()
            
            for item in data.get('items', []):
                video_id = item.get('id')
                statistics = item.get('statistics', {})
                view_count_str = statistics.get('viewCount', '0')
                
                try:
                    view_count = int(view_count_str)
                    results[video_id] = view_count
                    found_video_ids.add(video_id)
                    logger.debug(f"✅ {video_id}: {view_count:,} views")
                except ValueError:
                    logger.warning(f"⚠️ 조회수 변환 실패: {video_id} - {view_count_str}")
                    results[video_id] = -999
            
            # 응답에 없는 동영상은 -999로 처리 (삭제된 동영상 등)
            for video_id in video_ids:
                if video_id not in found_video_ids:
                    results[video_id] = -999
                    logger.warning(f"⚠️ API 응답에 없는 동영상: {video_id}")
            
            logger.info(f"✅ API 호출 성공: {len(found_video_ids)}개 성공 / {len(video_ids)}개 중")
            return results
            
        except requests.exceptions.RequestException as e:
            logger.error(f"❌ YouTube API 네트워크 오류: {e}")
            return {video_id: -999 for video_id in video_ids}
        except Exception as e:
            logger.error(f"❌ YouTube API 호출 중 오류: {e}")
            return {video_id: -999 for video_id in video_ids}

# 편의 함수
def update_youtube_viewcounts_for_period(start_date: date, end_date: date, target_date: date = None):
    """
    YouTube 조회수 수집 편의 함수
    
    Args:
        start_date (date): 크롤링 기간 시작일
        end_date (date): 크롤링 기간 종료일
        target_date (date, optional): 조회수 수집 날짜 (None이면 오늘 날짜)
    """
    service = YouTubeApiService()
    service.update_youtube_viewcounts_for_period(start_date, end_date, target_date) 