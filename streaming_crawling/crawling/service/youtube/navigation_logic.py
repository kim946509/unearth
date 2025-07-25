"""
YouTube 네비게이션 관련 로직
"""
import time
import logging
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from crawling.utils.constants import YouTubeSelectors, CommonSettings

logger = logging.getLogger(__name__)


class YouTubeNavigationLogic:
    """YouTube 네비게이션 관련 로직"""
    
    def __init__(self, driver):
        self.driver = driver
        self.wait = WebDriverWait(driver, CommonSettings.DEFAULT_WAIT_TIME)
    
    def wait_for_title_load(self) -> bool:
        """
        제목 로딩 대기
        
        Returns:
            bool: 로딩 성공 여부
        """
        selectors = YouTubeSelectors.TITLE_SELECTORS[:3]  # 처음 3개만 사용
        
        for sel in selectors:
            try:
                self.wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, sel)))
                logger.debug(f"✅ 제목 로딩 완료: {sel}")
                return True
            except:
                continue
        
        logger.error("❌ 제목 selector를 찾지 못함")
        return False
    
    def click_expand_button(self) -> bool:
        """
        조회수 expand 버튼 클릭하여 정확한 조회수 표시
        
        Returns:
            bool: 클릭 성공 여부
        """
        expand_selectors = YouTubeSelectors.EXPAND_BUTTON_SELECTORS
        
        for selector in expand_selectors:
            try:
                # expand 버튼 찾기
                expand_button = self.wait.until(
                    EC.element_to_be_clickable((By.CSS_SELECTOR, selector))
                )
                
                if expand_button:
                    # 버튼 클릭
                    expand_button.click()
                    
                    # 클릭 후 잠시 대기 (조회수 업데이트 시간)
                    time.sleep(1)
                    logger.debug(f"✅ expand 버튼 클릭 성공: {selector}")
                    return True
                    
            except Exception as e:
                logger.debug(f"expand 버튼 클릭 시도 실패 ({selector}): {e}")
                continue
        
        logger.warning("⚠️ expand 버튼을 찾을 수 없습니다. 기본 조회수를 사용합니다.")
        return False
    
    def wait_for_page_load(self) -> bool:
        """
        페이지 완전 로딩 대기
        
        Returns:
            bool: 로딩 성공 여부
        """
        try:
            # 제목 로딩 대기
            if not self.wait_for_title_load():
                return False
            
            # expand 버튼 클릭
            self.click_expand_button()
            
            # 추가 대기 시간
            time.sleep(2)
            
            return True
            
        except Exception as e:
            logger.error(f"❌ 페이지 로딩 대기 실패: {e}")
            return False 