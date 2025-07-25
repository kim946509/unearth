"""
Genie 네비게이션 관련 로직
"""
import logging
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By
from crawling.utils.constants import GenieSelectors

logger = logging.getLogger(__name__)

class GenieNavigationLogic:
    """Genie 네비게이션 관련 로직을 담당하는 클래스"""
    
    def __init__(self, driver):
        self.driver = driver
        self.wait = WebDriverWait(driver, 10)  # 기본 대기 시간
    
    def navigate_to_song_info(self) -> str:
        """
        첫 번째 곡의 정보 페이지로 이동
        
        Returns:
            str: 곡 정보 페이지 HTML 또는 None
        """
        try:
            # 곡 정보 버튼 찾기 및 클릭
            song_info_button = self.wait.until(
                EC.element_to_be_clickable((By.CSS_SELECTOR, GenieSelectors.SONG_INFO_BUTTON))
            )
            song_info_button.click()
            logger.info("✅ 곡 정보 페이지 버튼 클릭 완료")
            
            # 곡 정보 페이지의 곡명이 나타날 때까지 대기
            try:
                self.wait.until(
                    EC.visibility_of_element_located((By.CSS_SELECTOR, GenieSelectors.SONG_TITLE))
                )
                logger.info("✅ 곡 정보 페이지 로딩 완료")
            except Exception as e:
                logger.warning(f"곡 정보 페이지 로딩 대기 실패: {e}")
            
            return self.driver.page_source
            
        except Exception as e:
            logger.error(f"❌ 곡 정보 버튼 클릭 실패: {e}")
            return None
    
    def wait_for_song_info_page(self) -> bool:
        """
        곡 정보 페이지 로딩 대기
        
        Returns:
            bool: 로딩 성공 여부
        """
        try:
            self.wait.until(
                EC.visibility_of_element_located((By.CSS_SELECTOR, GenieSelectors.SONG_TITLE))
            )
            logger.info("✅ 곡 정보 페이지 로딩 완료")
            return True
        except Exception as e:
            logger.warning(f"곡 정보 페이지 로딩 대기 실패: {e}")
            return False 