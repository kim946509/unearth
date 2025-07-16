"""
YouTube Music 크롤링 및 파싱 로직
"""
import time
import random
import logging
import re
import os
from datetime import datetime
from dotenv import load_dotenv
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from crawling_view.utils.constants import YouTubeMusicSelectors, CommonSettings
from crawling_view.utils.utils import normalize_text, make_soup, get_current_timestamp, convert_view_count
from crawling_view.utils.matching import compare_song_info_multilang

# .env 파일 로드
load_dotenv()

logger = logging.getLogger(__name__)

class YouTubeMusicCrawler:
    def __init__(self, driver):
        self.driver = driver
        self.wait = WebDriverWait(driver, CommonSettings.DEFAULT_WAIT_TIME)
        self.youtube_music_id = os.getenv('YOUTUBE_MUSIC_ID', '')
        self.youtube_music_password = os.getenv('YOUTUBE_MUSIC_PASSWORD', '')
        self.is_logged_in = False
    

    
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
            
            # 3. 페이지 타이틀 확인
            try:
                title = self.driver.title
                if "YouTube Music" in title and not any(x in title.lower() for x in ["sign in", "로그인"]):
                    logger.info("✅ 로그인된 상태 확인됨 (페이지 타이틀 기반)")
                    return True
            except Exception as e:
                logger.debug(f"타이틀 확인 중 오류: {e}")
            
            # 4. 현재 URL 확인
            try:
                current_url = self.driver.current_url
                if "music.youtube.com" in current_url and not any(x in current_url.lower() for x in ["signin", "login"]):
                    logger.info("✅ 로그인된 상태 확인됨 (URL 기반)")
                    return True
            except Exception as e:
                logger.debug(f"URL 확인 중 오류: {e}")
            
            # 5. 쿠키 기반 확인
            try:
                cookies = self.driver.get_cookies()
                auth_cookies = [c for c in cookies if any(x in c.get('name', '').lower() for x in ['sid', 'ssid', 'hsid', 'auth', 'apisid', 'sapisid'])]
                if auth_cookies:
                    logger.info(f"✅ 로그인된 상태 확인됨 (인증 쿠키 {len(auth_cookies)}개 발견)")
                    return True
            except Exception as e:
                logger.debug(f"쿠키 확인 중 오류: {e}")
            
            # 6. 페이지 소스 확인
            try:
                page_source = self.driver.page_source
                if 'ytmusic-app' in page_source and not any(x in page_source.lower() for x in ['sign in to continue', '로그인하세요']):
                    logger.info("✅ 로그인된 상태 확인됨 (페이지 소스 기반)")
                    return True
            except Exception as e:
                logger.debug(f"페이지 소스 확인 중 오류: {e}")
            
            logger.warning("⚠️ 로그인 상태를 명확히 판단할 수 없음 (모든 확인 방법 실패)")
            
            # 현재 페이지 상태 디버깅을 위한 스크린샷 저장
            try:
                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                screenshot_path = f"logs/login_check_{timestamp}.png"
                self.driver.save_screenshot(screenshot_path)
                logger.info(f"📸 로그인 상태 확인 스크린샷 저장됨: {screenshot_path}")
            except Exception as e:
                logger.debug(f"스크린샷 저장 실패: {e}")
            
                return False
            
        except Exception as e:
            logger.warning(f"로그인 상태 확인 실패: {e}")
            return False
    
    def login(self):
        """
        YouTube Music 로그인 (일반 로그인만 사용)
        
        Returns:
            bool: 로그인 성공 여부
        """
        try:
            logger.info("🔐 일반 로그인 시도")
            return self._perform_manual_login()
            
        except Exception as e:
            logger.error(f"❌ YouTube Music 로그인 실패: {e}", exc_info=True)
            return False
    
    def _find_element_with_fallback(self, selector_list, wait_type='presence'):
        for by, value in selector_list:
            try:
                if wait_type == 'presence':
                    return self.wait.until(EC.presence_of_element_located((by, value)))
                elif wait_type == 'clickable':
                    return self.wait.until(EC.element_to_be_clickable((by, value)))
            except Exception:
                continue
        raise Exception(f"요소를 찾을 수 없습니다: {selector_list}")

    def _perform_manual_login(self):
        """
        수동 로그인 수행
        Returns:
            bool: 로그인 성공 여부
        """
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
                time.sleep(random.uniform(CommonSettings.RANDOM_DELAY_MIN, CommonSettings.RANDOM_DELAY_MAX))
                email_input.send_keys(self.youtube_music_id)
                time.sleep(random.uniform(CommonSettings.RANDOM_DELAY_MIN, CommonSettings.RANDOM_DELAY_MAX))

                # '다음' 버튼 클릭
                next_button = self._find_element_with_fallback(YouTubeMusicSelectors.GOOGLE_LOGIN['EMAIL_NEXT'], 'clickable')
                time.sleep(random.uniform(CommonSettings.RANDOM_DELAY_MIN, CommonSettings.RANDOM_DELAY_MAX))
                next_button.click()
                time.sleep(random.uniform(CommonSettings.RANDOM_DELAY_MIN, CommonSettings.RANDOM_DELAY_MAX))

                # 비밀번호 입력
                password_input = self._find_element_with_fallback(YouTubeMusicSelectors.GOOGLE_LOGIN['PASSWORD_INPUT'], 'presence')
                time.sleep(random.uniform(CommonSettings.RANDOM_DELAY_MIN, CommonSettings.RANDOM_DELAY_MAX))
                password_input.send_keys(self.youtube_music_password)
                time.sleep(random.uniform(CommonSettings.RANDOM_DELAY_MIN, CommonSettings.RANDOM_DELAY_MAX))

                # 로그인 버튼 클릭
                login_button = self._find_element_with_fallback(YouTubeMusicSelectors.GOOGLE_LOGIN['PASSWORD_NEXT'], 'clickable')
                time.sleep(random.uniform(CommonSettings.RANDOM_DELAY_MIN, CommonSettings.RANDOM_DELAY_MAX))
                login_button.click()
                time.sleep(random.uniform(CommonSettings.RANDOM_DELAY_MIN, CommonSettings.RANDOM_DELAY_MAX))

                # 본인 인증 화면 감지 및 대기
                time.sleep(2)
                page_source = self.driver.page_source
                if any(keyword in page_source for keyword in YouTubeMusicSelectors.AUTHENTICATION_KEYWORDS):
                    logger.warning("⚠️ 본인 인증(추가 인증) 화면이 감지되었습니다. 자동화가 중단될 수 있습니다.")
                    time.sleep(30)

                # 로그인 완료 대기 (모든 로그인 버튼 셀렉터에 대해 확인)
                for selector in YouTubeMusicSelectors.LOGIN_BUTTON:
                    try:
                        self.wait.until_not(EC.presence_of_element_located((By.CSS_SELECTOR, selector)))
                    except Exception:
                        continue
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
    
    def crawl_song(self, song_info):
        """
        단일 곡 크롤링
        
        Args:
            song_info (dict): 곡 정보 (title_ko, title_en, artist_ko, artist_en)
            
        Returns:
            dict: 크롤링 결과 또는 None
        """
        try:
            if not self.is_logged_in:
                logger.error("❌ 로그인이 필요합니다.")
                return None
            
            # 먼저 국문으로 검색
            logger.info("🔍 국문으로 검색 시도")
            html = self._search_song(song_info['title_ko'], song_info['artist_ko'])
            if html:
                result = self._parse_song_info(html, song_info)
                if result:
                    return result
            
            # 국문 검색 실패시 영문으로 검색
            logger.info("🔍 영문으로 검색 시도")
            html = self._search_song(song_info['title_en'], song_info['artist_en'])
            if html:
                result = self._parse_song_info(html, song_info)
                if result:
                    return result
            
            logger.warning(f"❌ 모든 검색 시도 실패: {song_info}")
            return None
            
        except Exception as e:
            logger.error(f"❌ 곡 크롤링 실패: {e}", exc_info=True)
            return None
    
    def _search_song(self, song_title, artist_name):
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
                    try:
                        self.driver.execute_script("arguments[0].click();", search_input)
                        logger.info("✅ 검색 입력창 포커스 성공")
                    except Exception as e:
                        logger.warning(f"⚠️ 검색 입력창 포커스 실패: {e}")
                        search_input.click()
                    
                    time.sleep(1)
                    
                    # 검색어 입력
                    if not search_input:
                        raise Exception("검색 입력창을 찾을 수 없습니다.")
                    
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
                    
                    time.sleep(1)  # 검색어 입력 후 대기 시간 단축
                    
                    # 검색어가 제대로 입력되었는지 확인
                    current_value = search_input.get_attribute('value') or ''
                    if current_value != query:
                        logger.warning(f"⚠️ 검색어가 제대로 입력되지 않음: '{current_value}' != '{query}'")
                        # 다시 입력 시도
                        search_input.clear()
                        time.sleep(1)
                        search_input.send_keys(query)
                        time.sleep(1)
                    
                    # Enter 키로 검색 실행 (더 안정적)
                    search_input.send_keys(Keys.RETURN)
                    logger.info("✅ Enter 키로 검색 실행")
                    
                    time.sleep(1)  # 검색 실행 후 대기 시간 단축
                    
                    # "노래" 탭 클릭 (다국어 지원)
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
                    
                    time.sleep(2)  # 원래 대기 시간으로 복원
                    
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
    
    def _find_search_button(self):
        """검색 버튼 찾기"""
        for selector in YouTubeMusicSelectors.SEARCH_BUTTON:
            try:
                logger.debug(f"🔍 검색 버튼 셀렉터 시도: {selector}")
                search_button = self.wait.until(
                    EC.element_to_be_clickable((By.CSS_SELECTOR, selector))
                )
                logger.info(f"✅ 검색 버튼 찾기 성공: {selector}")
                return search_button
            except Exception as e:
                logger.debug(f"❌ 검색 버튼 셀렉터 실패: {selector} - {str(e)}")
                continue
        
        # 모든 셀렉터 실패 시 현재 페이지 상태 로깅
        logger.error("❌ 모든 검색 버튼 셀렉터 실패")
        self._log_page_state()
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
        self._log_page_state()
        return None
    
    def _parse_song_info(self, html, song_info):
        try:
            logger.info(f"[파싱] '{song_info['artist_ko']} - {song_info['title_ko']}' 정보 추출 시도 중...")
            
            soup = make_soup(html)
            if not soup:
                return None
            
            # 디버깅: 실제 HTML 구조 로깅
            logger.debug("=== 페이지 HTML 구조 분석 시작 ===")
            # ytmusic-responsive-list-item-renderer 태그 찾기
            all_items = soup.find_all('ytmusic-responsive-list-item-renderer')
            logger.debug(f"발견된 ytmusic-responsive-list-item-renderer 태그 수: {len(all_items)}")
            
            if all_items:
                sample_item = all_items[0]
                logger.debug(f"첫 번째 아이템의 클래스: {sample_item.get('class', [])}")
                logger.debug(f"첫 번째 아이템의 속성들: {sample_item.attrs}")
            
            # 여러 셀렉터를 시도하여 검색 결과 찾기
            song_items = []
            for selector in YouTubeMusicSelectors.SONG_ITEMS:
                items = soup.select(selector)
                if items:
                    song_items = items
                    logger.info(f"🔍 YouTube Music 검색 결과: {len(song_items)}개 곡 발견 (셀렉터: {selector})")
                    logger.debug(f"매칭된 첫 번째 아이템 HTML: {items[0]}")
                    # 상위 5개 곡명/아티스트 로그
                    for idx, item in enumerate(song_items[:5]):
                        try:
                            title_tag = item.select_one(YouTubeMusicSelectors.SONG_TITLE)
                            artist_col = item.select_one(YouTubeMusicSelectors.ARTIST_COLUMN)
                            artist_tag = artist_col.select_one(YouTubeMusicSelectors.ARTIST_LINK) if artist_col else None
                            logger.info(f"  [{idx+1}] 곡명: '{title_tag.get_text(strip=True) if title_tag else None}' / 아티스트: '{artist_tag.get_text(strip=True) if artist_tag else None}'")
                        except Exception as e:
                            logger.info(f"  [{idx+1}] 곡명/아티스트 추출 실패: {e}")
                    break
            
            if not song_items:
                logger.warning("⚠️ 모든 셀렉터에서 검색 결과를 찾지 못했습니다")
                logger.info(f"🔍 YouTube Music 검색 결과: 0개 곡 발견")
                return None
            
            for i, item in enumerate(song_items):
                logger.info(f"🔍 검사 중인 곡 {i+1}/{len(song_items)}")
                try:
                    # 곡명 추출
                    song_title = self._extract_song_title(item)
                    if not song_title:
                        continue

                    # 아티스트명 추출
                    artist_name = self._extract_artist_name(item)
                    if not artist_name:
                        continue
                        
                    logger.info(f"🔍 발견된 곡: '{song_title}' - '{artist_name}'")

                    # 조회수 추출
                    view_count = self._extract_view_count(item)

                    # matching.py의 compare_song_info 함수 사용
                    match_result = compare_song_info_multilang(song_title, artist_name, song_info)
                    
                    logger.debug(f"매칭 결과: {match_result}")
                    
                    if match_result['both_match']:
                        result = {
                            'song_title': song_title,
                            'artist_name': artist_name,
                            'views': convert_view_count(view_count),
                            'listeners': -1,  # YouTube Music은 청취자 수 제공 안함
                            'crawl_date': get_current_timestamp(),
                            'song_id': song_info.get('song_id')  # song_id 추가
                        }
                        logger.info(f"[성공] 일치하는 곡 발견: {song_title} - {artist_name} ({view_count})")
                        return result
                    else:
                        logger.debug(f"매칭 실패: {match_result}")

                except Exception as e:
                    logger.warning(f"개별 곡 파싱 중 예외 발생: {e}")
                    continue

            # 일치하는 곡을 찾지 못한 경우
            logger.warning(f"[실패] '{song_info['artist_ko']} - {song_info['title_ko']}'와 일치하는 곡을 찾지 못함")
            return None
            
        except Exception as e:
            logger.error(f"❌ 파싱 실패: {e}", exc_info=True)
            return None
    
    def _extract_song_title(self, item):
        """곡명 추출"""
        song_name_tag = item.select_one(YouTubeMusicSelectors.SONG_TITLE)
        if song_name_tag:
            song_title = song_name_tag.get_text(strip=True)
            logger.debug(f"✅ 곡명 추출 성공: {song_title}")
            return song_title
        return None
    
    def _extract_artist_name(self, item):
        """아티스트명 추출"""
        artist_column = item.select_one(YouTubeMusicSelectors.ARTIST_COLUMN)
        if artist_column:
            artist_a = artist_column.select_one(YouTubeMusicSelectors.ARTIST_LINK)
            if artist_a:
                artist_name = artist_a.get_text(strip=True)
                logger.debug(f"✅ 아티스트명 추출 성공: {artist_name}")
                return artist_name
        return None
    
    def _extract_view_count(self, item):
        """조회수 추출 (aria-label, title, textContent 모두 검사)"""
        try:
            flex_columns = item.select(YouTubeMusicSelectors.VIEW_COUNT_FLEX)
            logger.debug(f"🔍 발견된 flex-column 요소 수: {len(flex_columns)}")
            
            for i, flex_col in enumerate(flex_columns):
                # 1. aria-label 우선
                view_text = flex_col.get('aria-label', '').strip()
                # 2. 없으면 title
                if not view_text:
                    view_text = flex_col.get('title', '').strip()
                # 3. 없으면 textContent
                if not view_text:
                    view_text = flex_col.get_text(strip=True)
                logger.debug(f"🔍 flex-column {i+1}: view_text='{view_text}'")
                
                # 조회수 관련 키워드가 있는지 확인
                view_keywords = ['회', '재생', 'views', 'view', '억', '만', '천', 'k', 'm', 'b']
                if any(keyword in view_text.lower() for keyword in view_keywords):
                    # 정규표현식으로 숫자+단위만 추출
                    import re
                    match = re.search(r'([\d,.]+(?:\.\d+)?)[ ]*([억만천mkb]*)', view_text.lower())
                    if match:
                        number = match.group(1)
                        unit = match.group(2)
                        view_count_str = f'{number}{unit}'
                        logger.debug(f"✅ 조회수 추출 성공: '{view_text}' -> '{view_count_str}'")
                        return view_count_str
                    else:
                        # 키워드는 있으나 패턴이 안 맞으면 원본 반환(후처리에서 걸러짐)
                        return view_text
            logger.warning("⚠️ flex-column에서 조회수 정보를 찾을 수 없음")
            return None
        except Exception as e:
            logger.error(f"❌ 조회수 추출 실패: {e}")
            return None
    
    def _log_page_state(self):
        """현재 페이지 상태 로깅 (디버깅용)"""
        try:
            current_url = self.driver.current_url
            page_title = self.driver.title
            logger.info(f"📄 현재 URL: {current_url}")
            logger.info(f"📄 페이지 제목: {page_title}")
            
            # 검색 관련 요소들 확인
            search_elements = self.driver.find_elements(By.CSS_SELECTOR, '[aria-label*="검색"], [aria-label*="Search"], yt-icon-button, button#button')
            logger.info(f"🔍 검색 관련 요소 개수: {len(search_elements)}")
            
            for i, elem in enumerate(search_elements[:5]):  # 처음 5개만 로깅
                try:
                    aria_label = elem.get_attribute('aria-label') or 'N/A'
                    tag_name = elem.tag_name
                    is_displayed = elem.is_displayed()
                    is_enabled = elem.is_enabled()
                    logger.info(f"  요소 {i+1}: {tag_name} - aria-label: {aria_label} - 표시: {is_displayed} - 활성: {is_enabled}")
                except Exception:
                    logger.info(f"  요소 {i+1}: 정보 추출 실패")
            
            # 검색 입력창 관련 요소들 확인
            input_elements = self.driver.find_elements(By.CSS_SELECTOR, 'input[type="search"], input[aria-autocomplete], input[role="combobox"]')
            logger.info(f"🔍 검색 입력창 관련 요소 개수: {len(input_elements)}")
            
            for i, elem in enumerate(input_elements[:3]):  # 처음 3개만 로깅
                try:
                    input_type = elem.get_attribute('type') or 'N/A'
                    aria_autocomplete = elem.get_attribute('aria-autocomplete') or 'N/A'
                    role = elem.get_attribute('role') or 'N/A'
                    placeholder = elem.get_attribute('placeholder') or 'N/A'
                    is_displayed = elem.is_displayed()
                    is_enabled = elem.is_enabled()
                    logger.info(f"  입력창 {i+1}: type={input_type}, aria-autocomplete={aria_autocomplete}, role={role}, placeholder={placeholder}, 표시: {is_displayed}, 활성: {is_enabled}")
                except Exception:
                    logger.info(f"  입력창 {i+1}: 정보 추출 실패")
            
            # 페이지 소스 일부 저장 (디버깅용)
            page_source = self.driver.page_source
            if len(page_source) > 1000:
                logger.debug(f"📄 페이지 소스 (처음 1000자): {page_source[:1000]}...")
            else:
                logger.debug(f"📄 페이지 소스: {page_source}")
                
        except Exception as e:
            logger.error(f"❌ 페이지 상태 로깅 실패: {e}")
    

