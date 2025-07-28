"""
YouTube Music 검색 관련 로직
"""
import time
import random
import logging
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from crawling.utils.constants import YouTubeMusicSelectors, CommonSettings

logger = logging.getLogger(__name__)

class YouTubeMusicSearchLogic:
    """YouTube Music 검색 관련 로직을 담당하는 클래스"""
    
    def __init__(self, driver):
        self.driver = driver
        self.wait = WebDriverWait(driver, CommonSettings.DEFAULT_WAIT_TIME)
    
    def search_song(self, song_title: str, artist_name: str) -> str:
        """
        YouTube Music에서 곡 검색
        
        Args:
            song_title (str): 곡 제목
            artist_name (str): 아티스트명
            
        Returns:
            str: 검색 결과 HTML 또는 None
        """
        try:
            # 줄바꿈 제거 및 공백 정리
            clean_artist = artist_name.strip().replace('\n', ' ').replace('\r', ' ')
            clean_song = song_title.strip().replace('\n', ' ').replace('\r', ' ')
            query = f"{clean_artist} {clean_song}"
            logger.info(f"🔍 YouTube Music 검색어: {query}")
            max_attempts = 3
            
            for attempt in range(max_attempts):
                try:
                    logger.info(f"🔍 검색 시도 {attempt+1}/{max_attempts}")
                    
                    # 검색어 입력창 찾기
                    search_input = self._find_search_input()
                    if not search_input:
                        raise Exception("검색 입력창을 찾을 수 없습니다.")
                    
                    # 검색 입력창 클릭하여 포커스
                    self._focus_search_input(search_input)
                    
                    # 검색어 입력
                    self._input_search_query(search_input, query)
                    
                    # 검색 실행
                    self._execute_search(search_input)
                    
                    # "노래" 탭 클릭
                    self._click_song_tab()
                    
                    time.sleep(2)
                    
                    # HTML 반환
                    html = self.driver.page_source
                    logger.info(f"✅ 검색 성공: {artist_name} - {song_title}")
                    return html
                    
                except Exception as e:
                    logger.warning(f"검색 시도 {attempt+1} 실패: {e}")
                    if attempt < max_attempts - 1:
                        # 유튜브 뮤직 메인 페이지로 돌아가기
                        self.driver.get("https://music.youtube.com/")
                        time.sleep(2)
                    else:
                        logger.error(f"❌ 모든 검색 시도 실패: {artist_name} - {song_title}")
                        
            return None
            
        except Exception as e:
            logger.error(f"❌ 곡 검색 실패: {e}", exc_info=True)
            return None
    
    def _find_search_input(self):
        """검색 입력창 찾기"""
        for selector in YouTubeMusicSelectors.SEARCH_INPUT:
            try:
                logger.debug(f"🔍 검색 입력창 셀렉터 시도: {selector}")
                
                # 먼저 요소가 존재하는지 확인
                search_input = self.wait.until(
                    EC.presence_of_element_located((By.CSS_SELECTOR, selector))
                )
                
                # 요소가 화면에 보이는지 확인
                if not search_input.is_displayed():
                    logger.debug(f"❌ 검색 입력창이 화면에 보이지 않음: {selector}")
                    continue
                
                # 요소가 상호작용 가능한지 확인
                if not search_input.is_enabled():
                    logger.debug(f"❌ 검색 입력창이 비활성화됨: {selector}")
                    continue
                
                # 입력창의 속성 확인
                input_type = search_input.get_attribute('type') or ''
                if input_type == 'hidden':
                    logger.debug(f"❌ 검색 입력창이 숨겨진 상태: {selector}")
                    continue
                
                logger.info(f"✅ 검색 입력창 찾기 성공: {selector}")
                return search_input
                
            except Exception as e:
                logger.debug(f"❌ 검색 입력창 셀렉터 실패: {selector} - {str(e)}")
                continue
        
        # 모든 셀렉터 실패 시 현재 페이지 상태 로깅
        logger.error("❌ 모든 검색 입력창 셀렉터 실패")
        return None
    
    def _focus_search_input(self, search_input):
        """검색 입력창에 포커스"""
        try:
            self.driver.execute_script("arguments[0].click();", search_input)
            logger.info("✅ 검색 입력창 포커스 성공")
        except Exception as e:
            logger.warning(f"⚠️ 검색 입력창 포커스 실패: {e}")
            search_input.click()
        
        time.sleep(1)
    
    def _input_search_query(self, search_input, query: str):
        """검색어 입력"""
        # 검색 입력창이 활성화될 때까지 대기
        try:
            self.wait.until(EC.element_to_be_clickable((By.CSS_SELECTOR, search_input.tag_name)))
        except Exception as e:
            logger.warning(f"⚠️ 검색 입력창 활성화 대기 실패: {e}")
        
        # 검색어 입력 전에 입력창 상태 확인
        try:
            # 입력창이 비어있는지 확인
            current_value = search_input.get_attribute('value') or ''
            if current_value:
                logger.info(f"🔍 기존 검색어 제거: {current_value}")
                search_input.clear()
                time.sleep(1)
        except Exception as e:
            logger.warning(f"⚠️ 기존 검색어 제거 실패: {e}")
        
        # 검색어 입력 (더 안전한 방법)
        try:
            # JavaScript로 값 설정 시도
            self.driver.execute_script("arguments[0].value = arguments[1];", search_input, query)
            logger.info("✅ JavaScript로 검색어 입력 성공")
        except Exception as e:
            logger.warning(f"⚠️ JavaScript 입력 실패, 일반 입력 시도: {e}")
            search_input.send_keys(query)
        
        time.sleep(1)
        
        # 검색어가 제대로 입력되었는지 확인
        current_value = search_input.get_attribute('value') or ''
        if current_value != query:
            logger.warning(f"⚠️ 검색어가 제대로 입력되지 않음: '{current_value}' != '{query}'")
            # 다시 입력 시도
            search_input.clear()
            time.sleep(1)
            search_input.send_keys(query)
            time.sleep(1)
    
    def _execute_search(self, search_input):
        """검색 실행"""
        search_input.send_keys(Keys.RETURN)
        logger.info("✅ Enter 키로 검색 실행")
        time.sleep(1)
    
    def _click_song_tab(self):
        """노래 탭 클릭"""
        song_tab_clicked = False
        for song_tab_selector in YouTubeMusicSelectors.SONG_TAB:
            try:
                logger.debug(f"🔍 노래 탭 셀렉터 시도: {song_tab_selector}")
                song_tab = self.wait.until(
                    EC.element_to_be_clickable((
                        By.XPATH,
                        song_tab_selector
                    ))
                )
                
                # 탭이 화면에 보이는지 확인
                if not song_tab.is_displayed():
                    logger.debug(f"❌ 노래 탭이 화면에 보이지 않음: {song_tab_selector}")
                    continue
                
                # JavaScript로 클릭 시도
                self.driver.execute_script("arguments[0].click();", song_tab)
                logger.info(f"✅ JavaScript로 노래 탭 클릭 성공: {song_tab_selector}")
                song_tab_clicked = True
                break
                
            except Exception as e:
                logger.debug(f"❌ 노래 탭 셀렉터 실패: {song_tab_selector} - {str(e)}")
                continue
        
        if not song_tab_clicked:
            logger.warning("⚠️ 모든 노래 탭 셀렉터 실패, 탭 클릭 없이 계속 진행") 