"""
크롤링 실패 관리 서비스
"""
from django.utils import timezone
from crawling_view.models import CrawlingFailure
from crawling_view.utils.single_crawling_logger import logger


class FailureService:
    """크롤링 실패 관리 서비스"""
    
    @staticmethod
    def add_failure(song_id: str, failed_platforms: list):
        """
        크롤링 실패 곡을 추가하거나 업데이트
        
        Args:
            song_id: 음원 ID
            failed_platforms: 실패한 플랫폼 목록
        """
        if not song_id or not failed_platforms:
            return
            
        try:
            # 기존 실패 기록이 있는지 확인
            failure, created = CrawlingFailure.objects.get_or_create(
                song_id=song_id,
                defaults={'failed_platforms': ''}
            )
            
            if created:
                logger.info(f"새로운 실패 곡 추가: {song_id}")
            else:
                logger.info(f"기존 실패 곡 업데이트: {song_id}")
            
            # 실패한 플랫폼 목록 업데이트
            failure.set_failed_platforms_list(failed_platforms)
            failure.failed_at = timezone.now()
            failure.save()
            
            logger.info(f"실패 곡 저장 완료: {song_id} - {failure.failed_platforms}")
            
        except Exception as e:
            logger.error(f"실패 곡 저장 중 오류 발생: {song_id} - {e}")
    
    @staticmethod
    def remove_success(song_id: str):
        """
        크롤링 성공 시 실패 목록에서 제거
        
        Args:
            song_id: 음원 ID
        """
        if not song_id:
            return
            
        try:
            failure = CrawlingFailure.objects.filter(song_id=song_id).first()
            if failure:
                failure.delete()
                logger.info(f"성공으로 인한 실패 곡 제거: {song_id}")
            else:
                logger.debug(f"실패 목록에 없는 곡: {song_id}")
                
        except Exception as e:
            if "doesn't exist" in str(e) or "Unknown column" in str(e) or "no such table" in str(e):
                logger.error(f"❌ crawling_failure 테이블이 존재하지 않습니다! 다음 SQL을 실행하세요:")
                logger.error(f"CREATE TABLE crawling_failure (id VARCHAR(36) PRIMARY KEY, song_id VARCHAR(255) UNIQUE, failed_at DATETIME, failed_platforms TEXT, created_at DATETIME, updated_at DATETIME);")
            else:
                logger.error(f"실패 곡 제거 중 오류 발생: {song_id} - {e}")
    

    
    @staticmethod
    def check_and_handle_failures(song_id: str, target_date=None):
        """
        DB에서 직접 -999 값을 조회하여 실패 처리
        
        Args:
            song_id: 음원 ID
            target_date: 크롤링 날짜 (None이면 오늘)
        """
        if not song_id:
            return
            
        try:
            from datetime import date
            from crawling_view.models import CrawlingData, PlatformType
            
            check_date = target_date or date.today()
            failed_platforms = []
            
            logger.info(f"🔍 실패 검사 시작: song_id={song_id}, 날짜={check_date}")
            
            # 각 플랫폼별로 -999 값 확인
            platforms = [
                ('GENIE', PlatformType.GENIE),
                ('YOUTUBE_MUSIC', PlatformType.YOUTUBE_MUSIC),
                ('YOUTUBE', PlatformType.YOUTUBE),
                ('MELON', PlatformType.MELON)
            ]
            
            for platform_name, platform_type in platforms:
                try:
                    # 해당 날짜의 데이터 조회
                    crawling_data = CrawlingData.objects.filter(
                        song_id=song_id,
                        platform=platform_type,
                        created_at__date=check_date
                    ).first()
                    
                    if crawling_data:
                        # views나 listeners 중 하나라도 -999이면 실패로 간주
                        has_failure = (crawling_data.views == -999 or crawling_data.listeners == -999)
                        
                        logger.info(f"  {platform_name}: views={crawling_data.views}, listeners={crawling_data.listeners}, 실패={has_failure}")
                        
                        if has_failure:
                            failed_platforms.append(platform_name)
                    else:
                        logger.warning(f"  {platform_name}: 해당 날짜 데이터 없음")
                        
                except Exception as e:
                    logger.error(f"  {platform_name} 검사 중 오류: {e}")
                    failed_platforms.append(platform_name)
            
            logger.info(f"📊 실패 검사 결과: {song_id} - 실패 플랫폼: {failed_platforms}")
            
            if failed_platforms:
                # 실패가 있으면 실패 목록에 추가
                FailureService.add_failure(song_id, failed_platforms)
                logger.info(f"❌ 실패 감지 및 저장: {song_id} - {failed_platforms} (날짜: {check_date})")
            else:
                # 모든 플랫폼 성공이면 실패 목록에서 제거
                FailureService.remove_success(song_id)
                logger.info(f"✅ 성공으로 실패 목록에서 제거: {song_id} (날짜: {check_date})")
                
        except Exception as e:
            logger.error(f"❌ 실패 검사 중 오류 발생: {song_id} - {e}")
    

    
    @staticmethod
    def get_failed_songs():
        """실패한 곡 목록 조회 (디버깅용)"""
        try:
            failures = CrawlingFailure.objects.all().order_by('-failed_at')
            return [(f.song_id, f.failed_platforms, f.failed_at) for f in failures]
        except Exception as e:
            logger.error(f"실패 곡 목록 조회 중 오류: {e}")
            return []
    
    @staticmethod
    def debug_song_data(song_id: str, target_date=None):
        """
        특정 곡의 크롤링 데이터 디버깅 (테스트용)
        
        Args:
            song_id: 음원 ID
            target_date: 크롤링 날짜 (None이면 오늘)
        """
        try:
            from datetime import date
            from crawling_view.models import CrawlingData, PlatformType
            
            check_date = target_date or date.today()
            
            logger.info(f"🔍 디버깅: song_id={song_id}, 날짜={check_date}")
            
            platforms = [
                ('GENIE', PlatformType.GENIE),
                ('YOUTUBE_MUSIC', PlatformType.YOUTUBE_MUSIC),
                ('YOUTUBE', PlatformType.YOUTUBE),
                ('MELON', PlatformType.MELON)
            ]
            
            for platform_name, platform_type in platforms:
                crawling_data = CrawlingData.objects.filter(
                    song_id=song_id,
                    platform=platform_type,
                    created_at__date=check_date
                ).first()
                
                if crawling_data:
                    logger.info(f"  {platform_name}: views={crawling_data.views}, listeners={crawling_data.listeners}, created_at={crawling_data.created_at}")
                else:
                    logger.warning(f"  {platform_name}: 데이터 없음")
                    
        except Exception as e:
            logger.error(f"❌ 디버깅 중 오류: {song_id} - {e}") 