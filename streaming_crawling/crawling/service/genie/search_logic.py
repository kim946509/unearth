"""
Genie 검색 관련 로직
"""
import time
import random
import logging
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By
from crawling.utils.constants import GenieSelectors, GenieSettings, CommonSettings

logger = logging.getLogger(__name__)

class GenieSearchLogic:
    """Genie 검색 관련 로직을 담당하는 클래스"""
    
    def __init__(self, driver):
        self.driver = driver
        self.wait = WebDriverWait(driver, CommonSettings.DEFAULT_WAIT_TIME)
    
    def search_song(self, song_title: str, artist_name: str) -> str:
        """
        Genie에서 곡 검색
        
        Args:
            song_title (str): 곡 제목
            artist_name (str): 아티스트명
            
        Returns:
            str: 곡 정보 페이지 HTML 또는 None
        """
        try:
            query = f"{artist_name} {song_title}"
            self.driver.get(GenieSettings.BASE_URL)
            
            max_attempts = 2
            # 검색 입력창 찾기 및 검색 실행
            for attempt in range(max_attempts):
                try:
                    # 검색 입력창 찾기
                    search_input = self._find_search_input()
                    if not search_input:
                        raise Exception("검색 입력창을 찾을 수 없습니다.")
                    
                    # 검색어 입력
                    self._input_search_query(search_input, query)
                    
                    # 검색 실행
                    self._execute_search(search_input)
                    
                    time.sleep(3)
                    
                    # 검색 결과 로딩 대기 후, 곡 정보 버튼 클릭
                    try:
                        # 곡 정보 버튼 찾기 (여러 개 있을 수 있으니 첫 번째 것 클릭)
                        song_info_button = self.wait.until(
                            EC.element_to_be_clickable((By.CSS_SELECTOR, 'a.btn-basic.btn-info[onclick^="fnViewSongInfo"]'))
                        )
                        song_info_button.click()
                        logger.info("✅ 곡 정보 페이지 버튼 클릭 완료")
                        
                        # 곡 정보 페이지의 곡명(h2.name)이 나타날 때까지 wait
                        try:
                            self.wait.until(EC.visibility_of_element_located((By.CSS_SELECTOR, 'h2.name')))
                            logger.info("✅ 곡 정보 페이지 로딩 완료")
                        except Exception as e:
                            logger.warning(f"곡 정보 페이지 로딩 대기 실패: {e}")
                        
                        # 곡 정보 페이지의 html 반환
                        return self.driver.page_source
                    except Exception as e:
                        logger.error(f"❌ 곡 정보 버튼 클릭 실패: {e}")
                        return None
                    break
                except Exception as e:
                    logger.warning(f"검색 입력창 입력 실패(시도 {attempt+1}): {e}")
                    if attempt < max_attempts - 1:
                        self.driver.refresh()
                        time.sleep(3)
                    else:
                        logger.error(f"검색 입력창 입력 마지막 시도({attempt+1})도 실패: {e}")
                        raise
            
            return None
            
        except Exception as e:
            logger.error(f"❌ 곡 검색 실패: {e}", exc_info=True)
            return None
    
    def _find_search_input(self):
        """검색 입력창 찾기"""
        for selector in GenieSelectors.SEARCH_INPUT:
            try:
                search_input = self.wait.until(
                    EC.visibility_of_element_located((By.CSS_SELECTOR, selector))
                )
                return search_input
            except Exception:
                continue
        return None
    
    def _input_search_query(self, search_input, query: str):
        """검색어 입력"""
        search_input.clear()
        time.sleep(random.uniform(0.7, 1.5))
        search_input.send_keys(query)
        time.sleep(random.uniform(0.7, 1.5))
    
    def _execute_search(self, search_input):
        """검색 실행 (엔터키 입력)"""
        try:
            search_input.send_keys(u'\ue007')  # 엔터키 전송
        except Exception as e:
            # StaleElementReferenceException 발생 시 재시도
            if "stale element reference" in str(e).lower():
                logger.warning("StaleElementReferenceException 발생, 검색 입력창을 다시 찾아서 엔터키 입력 재시도")
                # 검색 입력창을 다시 찾아서 엔터키 입력
                for selector in GenieSelectors.SEARCH_INPUT:
                    try:
                        search_input = self.wait.until(
                            EC.visibility_of_element_located((By.CSS_SELECTOR, selector))
                        )
                        search_input.send_keys(u'\ue007')
                        break
                    except Exception:
                        continue
                else:
                    raise Exception("재시도에서도 검색 입력창을 찾을 수 없습니다.")
            else:
                raise 