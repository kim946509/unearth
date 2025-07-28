"""
Melon 곡 ID 자동 검색 및 추출 로직
"""
import time
import random
import logging
from urllib.parse import urlparse, parse_qs
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from crawling.utils.constants import MelonSelectors, MelonSettings
from crawling.utils.utils import normalize_text
from crawling.utils.matching import compare_song_info_multilang

logger = logging.getLogger(__name__)

class MelonSongIdFinder:
    def __init__(self, driver):
        self.driver = driver
        self.wait = WebDriverWait(driver, MelonSettings.DEFAULT_WAIT_TIME)
    
    def find_melon_song_id(self, song_info):
        """
        멜론에서 곡 ID를 자동으로 찾는 메인 메서드
        
        Args:
            song_info (dict): 곡 정보 (title_ko, title_en, artist_ko, artist_en)
            
        Returns:
            str: 멜론 곡 ID 또는 None
        """
        try:
            logger.info(f"🍈 멜론 곡 ID 검색 시작: {song_info['artist_ko']} - {song_info['title_ko']}")
            
            # 한글 조합으로 먼저 검색 (한글 아티스트 + 한글 곡명)
            melon_song_id = self._try_search_combination(
                song_info['artist_ko'], 
                song_info['title_ko'], 
                song_info,
                "한글 아티스트 + 한글 곡명"
            )
            if melon_song_id:
                return melon_song_id
            
            # 영문이 있는 경우 다른 조합들도 시도
            if song_info.get('artist_en') and song_info.get('title_en'):
                # 영문 아티스트 + 한글 곡명
                melon_song_id = self._try_search_combination(
                    song_info['artist_en'], 
                    song_info['title_ko'], 
                    song_info,
                    "영문 아티스트 + 한글 곡명"
                )
                if melon_song_id:
                    return melon_song_id
                
                # 한글 아티스트 + 영문 곡명
                melon_song_id = self._try_search_combination(
                    song_info['artist_ko'], 
                    song_info['title_en'], 
                    song_info,
                    "한글 아티스트 + 영문 곡명"
                )
                if melon_song_id:
                    return melon_song_id
                
                # 영문 아티스트 + 영문 곡명
                melon_song_id = self._try_search_combination(
                    song_info['artist_en'], 
                    song_info['title_en'], 
                    song_info,
                    "영문 아티스트 + 영문 곡명"
                )
                if melon_song_id:
                    return melon_song_id
            
            logger.warning(f"❌ 모든 검색 조합 실패: {song_info['artist_ko']} - {song_info['title_ko']}")
            return None
            
        except Exception as e:
            logger.error(f"❌ 멜론 곡 ID 검색 실패: {e}", exc_info=True)
            return None
    
    def _try_search_combination(self, artist_name, song_title, original_song_info, combination_desc):
        """
        특정 아티스트명과 곡명 조합으로 검색 시도
        
        Args:
            artist_name (str): 검색할 아티스트명
            song_title (str): 검색할 곡명
            original_song_info (dict): 원본 곡 정보 (매칭 검증용)
            combination_desc (str): 조합 설명 (로그용)
            
        Returns:
            str: 멜론 곡 ID 또는 None
        """
        try:
            logger.info(f"🔍 {combination_desc}로 검색 시도: {artist_name} + {song_title}")
            
            # 멜론 검색 페이지로 이동
            search_query = f"{artist_name}+{song_title}"
            search_url = f"{MelonSettings.SEARCH_URL}?q={search_query}"
            
            logger.info(f"검색 URL: {search_url}")
            self.driver.get(search_url)
            time.sleep(random.uniform(3, 5))
            
            # 검색 결과가 로드될 때까지 대기
            try:
                self.wait.until(
                    EC.presence_of_element_located((By.CSS_SELECTOR, "table, .wrap_song_list, .list"))
                )
                logger.info("검색 결과 로딩 완료")
            except:
                logger.warning("검색 결과 로딩 대기 실패, 계속 진행")
            
            # 검색 결과에서 곡 리스트 찾기
            song_items = self._find_song_items()
            if not song_items:
                logger.warning(f"❌ 검색 결과 없음: {combination_desc}")
                return None
            
            # 각 곡에 대해 매칭 시도
            for i, song_item in enumerate(song_items[:5]):  # 상위 5개만 확인
                try:
                    # 곡명과 아티스트명 추출
                    extracted_title = self._extract_song_title(song_item)
                    extracted_artist = self._extract_artist_name(song_item)
                    
                    if not extracted_title or not extracted_artist:
                        continue
                    
                    logger.info(f"검색 결과 {i+1}: {extracted_artist} - {extracted_title}")
                    
                    # 매칭 검증
                    comparison_result = compare_song_info_multilang(
                        extracted_title, extracted_artist,
                        original_song_info['title_ko'],
                        original_song_info.get('title_en', ''),
                        original_song_info['artist_ko'],
                        original_song_info.get('artist_en', '')
                    )
                    
                    if comparison_result['both_match']:
                        logger.info(f"✅ 매칭 성공: {extracted_artist} - {extracted_title}")
                        
                        # 상세페이지로 이동하여 곡 ID 추출
                        melon_song_id = self._extract_song_id_from_detail(song_item)
                        if melon_song_id:
                            logger.info(f"✅ 멜론 곡 ID 추출 성공: {melon_song_id}")
                            return melon_song_id
                    else:
                        logger.debug(f"매칭 실패: {comparison_result}")
                        
                except Exception as e:
                    logger.warning(f"곡 아이템 처리 중 오류: {e}")
                    continue
            
            logger.warning(f"❌ 매칭되는 곡을 찾지 못함: {combination_desc}")
            return None
            
        except Exception as e:
            logger.error(f"❌ 검색 조합 시도 실패 ({combination_desc}): {e}", exc_info=True)
            return None
    
    def _find_song_items(self):
        """
        검색 결과 페이지에서 곡 아이템들 찾기
        
        Returns:
            list: 곡 아이템 WebElement 리스트
        """
        try:
            # 페이지 로딩 대기
            time.sleep(3)
            
            # 현재 페이지 URL 로깅
            current_url = self.driver.current_url
            logger.info(f"현재 페이지 URL: {current_url}")
            
            # 페이지 소스 일부 로깅 (디버깅용)
            page_source = self.driver.page_source
            if "검색결과" in page_source or "song" in page_source.lower():
                logger.info("✅ 검색 결과 페이지 감지됨")
            else:
                logger.warning("⚠️ 검색 결과 페이지가 아닐 수 있음")
            
            # 다양한 셀렉터로 시도
            song_items = []
            
            # 1. 기본 셀렉터 시도
            try:
                song_items = self.driver.find_elements(By.CSS_SELECTOR, MelonSelectors.SONG_ITEMS)
                logger.info(f"기본 셀렉터로 찾은 곡 수: {len(song_items)}개")
            except Exception as e:
                logger.warning(f"기본 셀렉터 실패: {e}")
            
            # 2. 더 일반적인 셀렉터 시도
            if not song_items:
                try:
                    song_items = self.driver.find_elements(By.CSS_SELECTOR, "tr.list")
                    logger.info(f"list 클래스로 찾은 곡 수: {len(song_items)}개")
                except Exception as e:
                    logger.warning(f"list 클래스 셀렉터 실패: {e}")
            
            # 3. 테이블 행 전체 시도
            if not song_items:
                try:
                    song_items = self.driver.find_elements(By.CSS_SELECTOR, "table tbody tr")
                    logger.info(f"테이블 행으로 찾은 곡 수: {len(song_items)}개")
                except Exception as e:
                    logger.warning(f"테이블 행 셀렉터 실패: {e}")
            
            # 4. 곡명 링크로 직접 찾기
            if not song_items:
                try:
                    title_links = self.driver.find_elements(By.CSS_SELECTOR, "a.fc_gray")
                    logger.info(f"fc_gray 클래스 링크 수: {len(title_links)}개")
                    # 링크가 있는 부모 tr 찾기
                    for link in title_links[:5]:  # 상위 5개만 확인
                        try:
                            parent_tr = link.find_element(By.XPATH, "./ancestor::tr")
                            if parent_tr not in song_items:
                                song_items.append(parent_tr)
                        except:
                            continue
                    logger.info(f"링크 기반으로 찾은 곡 수: {len(song_items)}개")
                except Exception as e:
                    logger.warning(f"링크 기반 셀렉터 실패: {e}")
            
            # 실제 곡 데이터가 있는 tr만 필터링 (헤더 제외)
            valid_song_items = []
            for i, item in enumerate(song_items):
                try:
                    # 곡명과 아티스트명이 모두 있는지 확인
                    title_found = False
                    artist_found = False
                    
                    # 곡명 확인
                    for title_selector in ["a.fc_gray", "div.ellipsis.rank01 a", "a[title]"]:
                        try:
                            title_element = item.find_element(By.CSS_SELECTOR, title_selector)
                            title_text = title_element.text.strip()
                            title_attr = title_element.get_attribute("title")
                            
                            if (title_text and len(title_text) < 200 and "상세정보" not in title_text) or \
                               (title_attr and len(title_attr) < 200 and "상세정보" not in title_attr):
                                title_found = True
                                logger.debug(f"곡 {i+1} 곡명: {title_text or title_attr}")
                                break
                        except:
                            continue
                    
                    # 아티스트명 확인 (td 기반으로)
                    try:
                        td_elements = item.find_elements(By.CSS_SELECTOR, "td.t_left")
                        if len(td_elements) >= 2:
                            artist_td = td_elements[1]
                            artist_links = artist_td.find_elements(By.CSS_SELECTOR, "a")
                            for link in artist_links:
                                artist_text = link.text.strip()
                                if artist_text and len(artist_text) < 100 and "상세정보" not in artist_text:
                                    artist_found = True
                                    logger.debug(f"곡 {i+1} 아티스트: {artist_text}")
                                    break
                    except:
                        pass
                    
                    # 곡명과 아티스트명이 모두 있으면 유효한 곡으로 판단
                    if title_found and artist_found:
                        valid_song_items.append(item)
                    else:
                        logger.debug(f"곡 {i+1} 필터링: 곡명={title_found}, 아티스트={artist_found}")
                        
                except Exception as e:
                    logger.debug(f"곡 {i+1} 필터링 실패: {e}")
                    continue
            
            logger.info(f"최종 검색 결과 곡 수: {len(valid_song_items)}개")
            return valid_song_items
            
        except Exception as e:
            logger.error(f"❌ 곡 아이템 찾기 실패: {e}")
            return []
    
    def _extract_song_title(self, song_item):
        """
        곡 아이템에서 곡명 추출
        
        Args:
            song_item: 곡 아이템 WebElement
            
        Returns:
            str: 곡명 또는 None
        """
        try:
            # 곡명 셀렉터 (우선순위 순서)
            title_selectors = [
                "a.fc_gray",                    # 가장 일반적인 곡명 링크
                "div.ellipsis.rank01 a",        # 곡명 컬럼의 링크
                "td.t_left:first-child a",      # 첫 번째 td의 링크 (보통 곡명)
                "a[title]",                     # title 속성이 있는 링크
                "span a",                       # span 안의 링크
            ]
            
            for selector in title_selectors:
                try:
                    title_element = song_item.find_element(By.CSS_SELECTOR, selector)
                    title = title_element.text.strip()
                    
                    # 곡명이 유효한지 확인 (아티스트명이나 기타 텍스트 필터링)
                    if title and len(title) < 200 and "상세정보" not in title and "페이지" not in title:
                        # title 속성도 확인해보기
                        title_attr = title_element.get_attribute("title")
                        if title_attr and title_attr.strip():
                            title = title_attr.strip()
                        
                        logger.debug(f"곡명 추출 성공 ({selector}): {title}")
                        return normalize_text(title)
                    else:
                        logger.debug(f"곡명 필터링됨 ({selector}): {title}")
                except:
                    continue
            
            # 추가 시도: 첫 번째 td.t_left에서 찾기
            try:
                td_elements = song_item.find_elements(By.CSS_SELECTOR, "td.t_left")
                if len(td_elements) >= 1:
                    # 보통 첫 번째는 곡명
                    title_td = td_elements[0]
                    title_links = title_td.find_elements(By.CSS_SELECTOR, "a")
                    for link in title_links:
                        title = link.text.strip()
                        if title and len(title) < 200 and "상세정보" not in title:
                            # title 속성도 확인
                            title_attr = link.get_attribute("title")
                            if title_attr and title_attr.strip():
                                title = title_attr.strip()
                            logger.debug(f"첫 번째 td에서 곡명 추출 성공: {title}")
                            return normalize_text(title)
            except Exception as e:
                logger.debug(f"첫 번째 td 방법 실패: {e}")
            
            logger.debug("모든 곡명 셀렉터 실패")
            return None
            
        except Exception as e:
            logger.debug(f"곡명 추출 실패: {e}")
            return None
    
    def _extract_artist_name(self, song_item):
        """
        곡 아이템에서 아티스트명 추출
        
        Args:
            song_item: 곡 아이템 WebElement
            
        Returns:
            str: 아티스트명 또는 None
        """
        try:
            # MelonSelectors.ARTIST_NAME 사용 (리스트 형태)
            artist_selectors = MelonSelectors.ARTIST_NAME + [
                "span a",
                "a.fc_mgray",
                "td a",  # 더 일반적인 셀렉터
            ]
            
            for selector in artist_selectors:
                try:
                    artist_element = song_item.find_element(By.CSS_SELECTOR, selector)
                    artist = artist_element.text.strip()
                    
                    # 아티스트명이 유효한지 확인 (너무 길거나 이상한 텍스트 필터링)
                    if artist and len(artist) < 100 and "상세정보" not in artist and "페이지" not in artist:
                        logger.debug(f"아티스트명 추출 성공 ({selector}): {artist}")
                        return normalize_text(artist)
                    else:
                        logger.debug(f"아티스트명 필터링됨 ({selector}): {artist}")
                except:
                    continue
            
            # 모든 셀렉터 실패 시 추가 시도: 두 번째 td.t_left에서 찾기
            try:
                td_elements = song_item.find_elements(By.CSS_SELECTOR, "td.t_left")
                if len(td_elements) >= 2:
                    # 보통 첫 번째는 곡명, 두 번째는 아티스트명
                    artist_td = td_elements[1]
                    artist_links = artist_td.find_elements(By.CSS_SELECTOR, "a")
                    for link in artist_links:
                        artist = link.text.strip()
                        if artist and len(artist) < 100 and "상세정보" not in artist:
                            logger.debug(f"두 번째 td에서 아티스트명 추출 성공: {artist}")
                            return normalize_text(artist)
            except Exception as e:
                logger.debug(f"두 번째 td 방법 실패: {e}")
            
            logger.debug("모든 아티스트명 셀렉터 실패")
            return None
            
        except Exception as e:
            logger.debug(f"아티스트명 추출 실패: {e}")
            return None
    
    def _extract_song_id_from_detail(self, song_item):
        """
        곡 상세 페이지로 이동하여 곡 ID 추출
        
        Args:
            song_item: 곡 아이템 WebElement
            
        Returns:
            str: 멜론 곡 ID 또는 None
        """
        try:
            # 상세 버튼 찾기 (여러 셀렉터 시도)
            detail_button = None
            detail_selectors = [
                "a.btn_icon_detail",
                "a[title*='곡정보']",
                "a[title*='상세']",
                "button.btn_icon_detail",
                "a.btn_detail"
            ]
            
            for selector in detail_selectors:
                try:
                    detail_button = song_item.find_element(By.CSS_SELECTOR, selector)
                    if detail_button:
                        logger.debug(f"상세 버튼 찾기 성공 ({selector})")
                        break
                except:
                    continue
            
            if not detail_button:
                logger.error("상세 버튼을 찾을 수 없음")
                return None
            
            # 방법 1: href 속성에서 JavaScript 함수 파라미터 추출 (우선 시도)
            try:
                href = detail_button.get_attribute("href")
                if href and ("melon.link.goSongDetail" in href or "searchLog" in href):
                    # JavaScript 함수에서 곡 ID 추출
                    import re
                    # melon.link.goSongDetail('32061975') 패턴에서 추출
                    match = re.search(r"melon\.link\.goSongDetail\('(\d+)'\)", href)
                    if match:
                        song_id = match.group(1)
                        logger.info(f"✅ href에서 곡 ID 추출 성공: {song_id}")
                        return song_id
                    
                    # searchLog 함수에서도 추출 시도 (마지막 파라미터가 곡 ID)
                    match = re.search(r"searchLog\([^,]+,[^,]+,[^,]+,[^,]+,'(\d+)'\)", href)
                    if match:
                        song_id = match.group(1)
                        logger.info(f"✅ href searchLog에서 곡 ID 추출 성공: {song_id}")
                        return song_id
            except Exception as e:
                logger.debug(f"href에서 추출 실패: {e}")
            
            # 방법 2: onclick 속성에서 URL 추출
            try:
                onclick = detail_button.get_attribute("onclick")
                if onclick and ("songId=" in onclick or "melon.link.goSongDetail" in onclick):
                    # onclick에서 songId 파라미터 추출
                    import re
                    match = re.search(r'songId=(\d+)', onclick)
                    if match:
                        song_id = match.group(1)
                        logger.info(f"✅ onclick에서 곡 ID 추출 성공: {song_id}")
                        return song_id
            except Exception as e:
                logger.debug(f"onclick에서 추출 실패: {e}")
            
            # 방법 3: 현재 창에서 상세 페이지로 이동 (백업용)
            logger.info("현재 창에서 상세 페이지로 이동하여 곡 ID 추출 시도")
            try:
                # 현재 URL 저장
                original_url = self.driver.current_url
                
                # 상세 버튼 클릭 (현재 창에서 이동)
                detail_button.click()
                time.sleep(3)  # 페이지 로딩 대기
                
                # URL에서 songId 파라미터 추출
                current_url = self.driver.current_url
                song_id = self._extract_song_id_from_url(current_url)
                
                if song_id:
                    logger.info(f"✅ 현재 창에서 곡 ID 추출 성공: {song_id}")
                    # 원래 검색 페이지로 돌아가기
                    self.driver.get(original_url)
                    time.sleep(2)
                    return song_id
                else:
                    logger.error("현재 창에서 곡 ID를 찾을 수 없음")
                    # 원래 검색 페이지로 돌아가기
                    self.driver.get(original_url)
                    time.sleep(2)
                    return None
                    
            except Exception as e:
                logger.error(f"❌ 현재 창 방식에서 곡 ID 추출 실패: {e}")
                # 오류 발생 시 원래 검색 페이지로 돌아가기 시도
                try:
                    self.driver.get(original_url)
                    time.sleep(2)
                except:
                    pass
                return None
            
        except Exception as e:
            logger.error(f"❌ 상세 페이지에서 곡 ID 추출 실패: {e}")
            return None
    
    def _extract_song_id_from_url(self, url):
        """
        URL에서 songId 파라미터 추출
        
        Args:
            url (str): 멜론 곡 상세 페이지 URL
            
        Returns:
            str: 곡 ID 또는 None
        """
        try:
            # URL 파싱
            parsed_url = urlparse(url)
            query_params = parse_qs(parsed_url.query)
            
            # songId 파라미터 추출
            song_id = query_params.get('songId', [None])[0]
            
            if song_id:
                logger.info(f"✅ URL에서 곡 ID 추출: {song_id}")
                return song_id
            else:
                logger.warning(f"❌ URL에서 songId 파라미터를 찾을 수 없음: {url}")
                return None
                
        except Exception as e:
            logger.error(f"❌ URL 파싱 실패: {e}")
            return None 