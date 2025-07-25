"""
YouTube Music 데이터 추출 관련 로직
"""
import logging
from crawling.utils.constants import YouTubeMusicSelectors
from crawling.utils.utils import make_soup, get_current_timestamp, convert_view_count
from crawling.utils.matching import compare_song_info_multilang

logger = logging.getLogger(__name__)

class YouTubeMusicDataExtractionLogic:
    """YouTube Music 데이터 추출 관련 로직을 담당하는 클래스"""
    
    def __init__(self):
        pass
    
    def parse_song_info(self, html: str, song_info: dict) -> dict:
        """
        검색 결과 HTML 파싱
        
        Args:
            html (str): 검색 결과 HTML
            song_info (dict): 검색한 곡 정보 (title_ko, title_en, artist_ko, artist_en)
            
        Returns:
            dict: 파싱된 곡 정보 또는 None
        """
        try:
            logger.info(f"[파싱] '{song_info['artist_ko']} - {song_info['title_ko']}' 정보 추출 시도 중...")
            
            soup = make_soup(html)
            if not soup:
                return None
            
            # 검색 결과에서 곡 리스트 찾기
            song_items = self._find_song_items(soup)
            if not song_items:
                logger.warning("⚠️ 모든 셀렉터에서 검색 결과를 찾지 못했습니다")
                return None
            
            # 검사할 곡 수를 최대 20개로 제한
            max_check_count = min(20, len(song_items))
            logger.info(f"🔍 검사할 곡 수: {max_check_count}개 (전체 {len(song_items)}개 중)")
            
            for i, item in enumerate(song_items[:max_check_count]):
                logger.info(f"🔍 검사 중인 곡 {i+1}/{max_check_count}")
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

                    # matching.py의 compare_song_info 함수 사용 (한글/영문 제목과 아티스트명 모두 사용)
                    match_result = compare_song_info_multilang(
                        song_title, artist_name, 
                        song_info['title_ko'], 
                        song_info.get('title_en', ''),
                        song_info['artist_ko'], 
                        song_info.get('artist_en', '')
                    )
                    
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
    
    def _find_song_items(self, soup):
        """검색 결과에서 곡 아이템들 찾기"""
        # 여러 셀렉터를 시도하여 검색 결과 찾기
        song_items = []
        for selector in YouTubeMusicSelectors.SONG_ITEMS:
            items = soup.select(selector)
            if items:
                song_items = items
                logger.info(f"🔍 YouTube Music 검색 결과: {len(song_items)}개 곡 발견 (셀렉터: {selector})")
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
        
        return song_items
    
    def _extract_song_title(self, item):
        """곡명 추출"""
        song_name_tag = item.select_one(YouTubeMusicSelectors.SONG_TITLE)
        if song_name_tag:
            song_title = song_name_tag.get_text(strip=True)
            logger.debug(f"✅ 곡명 추출 성공: {song_title}")
            return song_title
        return None
    
    def _extract_artist_name(self, item):
        """아티스트명 추출 (첫 번째 텍스트 요소를 아티스트명으로 사용)"""
        artist_column = item.select_one(YouTubeMusicSelectors.ARTIST_COLUMN)
        if artist_column:
            # 모든 텍스트 요소(a, span, yt-formatted-string 등)에서 첫 번째 유효한 텍스트 찾기
            all_elements = artist_column.select('a, span, yt-formatted-string')
            
            for element in all_elements:
                text = element.get_text(strip=True)
                logger.debug(f"🔍 요소 텍스트: '{text}'")
                
                # "•" 문자, 시간 형식(MM:SS), 빈 텍스트가 아닌 경우에만 아티스트명으로 인정
                if (text and text != "•" and text != "·" and len(text) > 1 and 
                    not self._is_time_format(text)):
                    logger.debug(f"✅ 아티스트명 추출 성공: {text}")
                    return text
            
            # 개별 요소에서 찾지 못했다면 전체 텍스트에서 첫 번째 부분 추출
            all_text = artist_column.get_text(strip=True)
            logger.debug(f"🔍 전체 텍스트: '{all_text}'")
            
            # "•"로 분리해서 첫 번째 부분을 아티스트명으로 사용
            if "•" in all_text:
                artist_part = all_text.split("•")[0].strip()
                if artist_part and len(artist_part) > 1 and not self._is_time_format(artist_part):
                    logger.debug(f"✅ 분리된 아티스트명 추출 성공: {artist_part}")
                    return artist_part
                    
        return None
    
    def _extract_view_count(self, item):
        """조회수 추출 (두 번째 flex-column 요소 선택)"""
        try:
            flex_columns = item.select(YouTubeMusicSelectors.VIEW_COUNT_FLEX)
            logger.debug(f"🔍 발견된 flex-column 요소 수: {len(flex_columns)}")
            
            # 모든 flex-column 요소의 정보 로깅
            for i, flex_col in enumerate(flex_columns):
                aria_label = flex_col.get('aria-label', '').strip()
                title = flex_col.get('title', '').strip()
                text_content = flex_col.get_text(strip=True)
                logger.debug(f"🔍 flex-column {i+1}: aria-label='{aria_label}', title='{title}', text='{text_content}'")
            
            # 두 번째 요소가 있으면 그것을 사용 (조회수는 보통 두 번째에 위치)
            if len(flex_columns) >= 2:
                target_element = flex_columns[1]  # 두 번째 요소 (인덱스 1)
                logger.debug(f"✅ 두 번째 flex-column 요소 선택 (인덱스 1)")
            elif len(flex_columns) == 1:
                target_element = flex_columns[0]  # 하나만 있으면 첫 번째 사용
                logger.debug(f"✅ 첫 번째 flex-column 요소 선택 (인덱스 0)")
            else:
                logger.warning("⚠️ flex-column 요소를 찾을 수 없음")
                return None
            
            # 선택된 요소에서 조회수 추출
            # 1. aria-label 우선
            view_text = target_element.get('aria-label', '').strip()
            # 2. 없으면 title
            if not view_text:
                view_text = target_element.get('title', '').strip()
            # 3. 없으면 textContent
            if not view_text:
                view_text = target_element.get_text(strip=True)
            
            logger.debug(f"🔍 선택된 요소: view_text='{view_text}'")
            
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
                    logger.debug(f"✅ 조회수 추출 성공: '{view_text}' (패턴 미매칭)")
                    return view_text
            
            logger.warning("⚠️ 선택된 요소에서 조회수 정보를 찾을 수 없음")
            return None
        except Exception as e:
            logger.error(f"❌ 조회수 추출 실패: {e}")
            return None
    
    def _is_time_format(self, text):
        """시간 형식인지 확인 (MM:SS 형태)"""
        import re
        time_pattern = r'^\d{1,2}:\d{2}$'  # MM:SS 또는 M:SS 형태
        return bool(re.match(time_pattern, text)) 