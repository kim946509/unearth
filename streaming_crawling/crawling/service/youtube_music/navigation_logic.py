"""
YouTube Music 네비게이션 관련 로직
"""
import os
import time
import json
import logging
from pathlib import Path
from datetime import datetime
from dotenv import load_dotenv
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from crawling.utils.constants import YouTubeMusicSelectors, CommonSettings

# .env 파일 로드
load_dotenv()

logger = logging.getLogger(__name__)

class YouTubeMusicNavigationLogic:
    """YouTube Music 네비게이션 관련 로직을 담당하는 클래스"""
    
    def __init__(self, driver):
        self.driver = driver
        self.wait = WebDriverWait(driver, CommonSettings.DEFAULT_WAIT_TIME)
        self.youtube_music_id = os.getenv('YOUTUBE_MUSIC_ID', '')
        self.youtube_music_password = os.getenv('YOUTUBE_MUSIC_PASSWORD', '')
        self.is_logged_in = False
        
        # 쿠키 저장 경로 설정
        self.cookies_dir = Path("user_data/cookies")
        self.cookies_dir.mkdir(parents=True, exist_ok=True)
        self.cookies_file = self.cookies_dir / "youtube_music_cookies.json"
    
    def login(self):
        """
        YouTube Music 로그인 (쿠키 우선, 실패 시 수동 로그인)
        
        Returns:
            bool: 로그인 성공 여부
        """
        try:
            # 1. 먼저 쿠키 기반 로그인 시도
            if self._try_cookie_login():
                return True
            
            # 2. 쿠키 로그인 실패 시 기존 쿠키 삭제 후 수동 로그인
            self._clear_stored_cookies()
            self.driver.delete_all_cookies()
            
            # 수동 로그인 수행
            if self._perform_manual_login():
                self._save_cookies()
                return True
            else:
                logger.error("❌ 수동 로그인 실패")
                return False
            
        except Exception as e:
            logger.error(f"❌ YouTube Music 로그인 실패: {e}", exc_info=True)
            return False
    
    def navigate_to_main_page(self):
        """YouTube Music 메인 페이지로 이동"""
        try:
            self.driver.get("https://music.youtube.com/")
            time.sleep(2)
            return True
        except Exception as e:
            logger.error(f"❌ 메인 페이지 이동 실패: {e}")
            return False
    
    def _try_cookie_login(self):
        """저장된 쿠키를 사용하여 로그인 시도"""
        try:
            # 쿠키 로드
            cookies = self._load_cookies()
            if not cookies:
                return False
            
            # 쿠키 적용
            if not self._apply_cookies(cookies):
                return False
            
            # 페이지 새로고침하여 쿠키 적용 확인
            self.driver.refresh()
            time.sleep(3)
            
            # 로그인 상태 확인
            if self._check_login_status():
                logger.info("🍪 쿠키 로그인 성공")
                self.is_logged_in = True
                return True
            else:
                logger.info("🍪 쿠키 로그인 실패")
                return False
                
        except Exception as e:
            logger.error(f"❌ 쿠키 로그인 오류: {e}")
            return False
    
    def _perform_manual_login(self):
        """수동 로그인 수행"""
        try:
            self.driver.get("https://music.youtube.com/")
            time.sleep(2)

            # 로그인 버튼이 보이면(=로그인 안 된 상태)만 로그인 로직 실행
            need_login = False
            for selector in YouTubeMusicSelectors.LOGIN_BUTTON:
                try:
                    login_btn = self.driver.find_element(By.CSS_SELECTOR, selector)
                    if login_btn.is_displayed():
                        need_login = True
                        break
                except Exception:
                    continue

            if need_login:
                # 로그인 프로세스 실행
                login_btn.click()
                time.sleep(2)

                # 이메일 입력
                email_input = self._find_element_with_fallback(YouTubeMusicSelectors.GOOGLE_LOGIN['EMAIL_INPUT'], 'presence')
                time.sleep(1)
                email_input.send_keys(self.youtube_music_id)
                time.sleep(1)

                # '다음' 버튼 클릭
                next_button = self._find_element_with_fallback(YouTubeMusicSelectors.GOOGLE_LOGIN['EMAIL_NEXT'], 'clickable')
                time.sleep(1)
                next_button.click()
                time.sleep(1)

                # 비밀번호 입력
                password_input = self._find_element_with_fallback(YouTubeMusicSelectors.GOOGLE_LOGIN['PASSWORD_INPUT'], 'presence')
                time.sleep(1)
                password_input.send_keys(self.youtube_music_password)
                time.sleep(1)

                # 로그인 버튼 클릭
                login_button = self._find_element_with_fallback(YouTubeMusicSelectors.GOOGLE_LOGIN['PASSWORD_NEXT'], 'clickable')
                time.sleep(1)
                login_button.click()
                time.sleep(1)

                # 본인 인증 화면 감지 및 대기
                time.sleep(2)
                page_source = self.driver.page_source
                if any(keyword in page_source for keyword in YouTubeMusicSelectors.AUTHENTICATION_KEYWORDS):
                    logger.warning("⚠️ 본인 인증(추가 인증) 화면이 감지되었습니다. 자동화가 중단될 수 있습니다.")
                    time.sleep(30)

                # 로그인 완료 대기
                time.sleep(2)

            # 유튜브 뮤직 페이지로 이동
            self.driver.get("https://music.youtube.com/")
            time.sleep(2)

            # 최종 로그인 상태 확인
            if self._check_login_status():
                self.is_logged_in = True
                logger.info("✅ 일반 로그인 성공")
                return True
            else:
                logger.error("❌ 일반 로그인 실패")
                return False

        except Exception as e:
            logger.error(f"❌ 수동 로그인 실패: {e}", exc_info=True)
            return False
    
    def _check_login_status(self):
        """로그인 상태 확인"""
        try:
            # 페이지 새로고침
            self.driver.get("https://music.youtube.com")
            time.sleep(2)
            
            # 1. 로그인 버튼 확인 (로그아웃 상태 체크)
            for selector in YouTubeMusicSelectors.LOGIN_BUTTON:
                elements = self.driver.find_elements(By.CSS_SELECTOR, selector)
                if elements and elements[0].is_displayed():
                    logger.info(f"❌ 로그인되지 않은 상태 (로그인 버튼 발견: {selector})")
                    return False
            
            # 2. 로그인 상태 확인 (여러 방법으로 체크)
            for selector in YouTubeMusicSelectors.LOGIN_STATUS_INDICATORS:
                try:
                    elements = self.driver.find_elements(By.CSS_SELECTOR, selector)
                    if elements and elements[0].is_displayed():
                        logger.info(f"✅ 로그인된 상태 확인됨 (지표: {selector})")
                        return True
                except Exception as e:
                    logger.debug(f"셀렉터 확인 중 오류: {selector} - {e}")
            
            # 기타 확인 방법들...
            return True  # 기본적으로 로그인된 것으로 간주
            
        except Exception as e:
            logger.warning(f"로그인 상태 확인 실패: {e}")
            return False
    
    def _find_element_with_fallback(self, selector_list, wait_type='presence'):
        """여러 셀렉터로 요소 찾기"""
        for by, value in selector_list:
            try:
                if wait_type == 'presence':
                    return self.wait.until(EC.presence_of_element_located((by, value)))
                elif wait_type == 'clickable':
                    return self.wait.until(EC.element_to_be_clickable((by, value)))
            except Exception:
                continue
        raise Exception(f"요소를 찾을 수 없습니다: {selector_list}")
    
    def _save_cookies(self):
        """현재 브라우저의 쿠키를 파일로 저장"""
        try:
            cookies = self.driver.get_cookies()
            if cookies:
                with open(self.cookies_file, 'w', encoding='utf-8') as f:
                    json.dump(cookies, f, ensure_ascii=False, indent=2)
                logger.info(f"🍪 쿠키 저장 완료 ({len(cookies)}개)")
                return True
            else:
                logger.warning("⚠️ 저장할 쿠키가 없음")
                return False
        except Exception as e:
            logger.error(f"❌ 쿠키 저장 실패: {e}")
            return False
    
    def _load_cookies(self):
        """저장된 쿠키 파일을 로드"""
        try:
            if not self.cookies_file.exists():
                return None
            
            with open(self.cookies_file, 'r', encoding='utf-8') as f:
                cookies = json.load(f)
            
            return cookies if cookies else None
                
        except Exception as e:
            logger.error(f"❌ 쿠키 로드 실패: {e}")
            return None
    
    def _apply_cookies(self, cookies):
        """브라우저에 쿠키 적용"""
        try:
            # YouTube Music 페이지로 이동 (쿠키 적용을 위해)
            self.driver.get("https://music.youtube.com/")
            time.sleep(2)
            
            applied_count = 0
            for cookie in cookies:
                try:
                    # 쿠키의 domain이 현재 도메인과 호환되는지 확인
                    current_domain = "music.youtube.com"
                    cookie_domain = cookie.get('domain', '').lstrip('.')
                    
                    if (cookie_domain == current_domain or 
                        cookie_domain == "youtube.com" or 
                        cookie_domain == ".youtube.com" or
                        cookie_domain == "google.com" or
                        cookie_domain == ".google.com"):
                        
                        # 만료일 체크 (expiry가 있는 경우)
                        if 'expiry' in cookie:
                            if cookie['expiry'] < time.time():
                                continue
                        
                        # 쿠키 적용
                        cookie_to_add = {
                            'name': cookie['name'],
                            'value': cookie['value'],
                            'domain': cookie.get('domain', '.youtube.com'),
                            'path': cookie.get('path', '/'),
                            'secure': cookie.get('secure', True),
                            'httpOnly': cookie.get('httpOnly', False)
                        }
                        
                        # expiry가 있으면 추가
                        if 'expiry' in cookie:
                            cookie_to_add['expiry'] = cookie['expiry']
                            
                        self.driver.add_cookie(cookie_to_add)
                        applied_count += 1
                        
                except Exception:
                    continue
            
            return applied_count > 0
            
        except Exception as e:
            logger.error(f"❌ 쿠키 적용 실패: {e}")
            return False
    
    def _clear_stored_cookies(self):
        """저장된 쿠키 파일 삭제"""
        try:
            if self.cookies_file.exists():
                self.cookies_file.unlink()
        except Exception:
            pass 