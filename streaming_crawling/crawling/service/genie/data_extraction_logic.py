"""
Genie 데이터 추출 관련 로직
"""
import logging
from crawling.utils.constants import GenieSelectors, GenieSettings
from crawling.utils.utils import make_soup, get_current_timestamp
from crawling.utils.matching import compare_song_info_multilang

logger = logging.getLogger(__name__)

class GenieDataExtractionLogic:
    """Genie 데이터 추출 관련 로직을 담당하는 클래스"""
    
    def __init__(self):
        pass
    
    def parse_song_info(self, html: str, target_song_info: dict) -> dict:
        """
        곡 정보 페이지 HTML 파싱
        
        Args:
            html (str): 곡 정보 페이지 HTML
            target_song_info (dict): 검색한 곡 정보 (title_ko, title_en, artist_ko, artist_en)
            
        Returns:
            dict: 파싱된 곡 정보 또는 None
        """
        for attempt in range(GenieSettings.MAX_PARSE_ATTEMPTS):
            try:
                logger.info(f"[시도 {attempt+1}/{GenieSettings.MAX_PARSE_ATTEMPTS}] '{target_song_info['artist_ko']} - {target_song_info['title_ko']}' 정보 추출 시도 중...")
                
                soup = make_soup(html)
                if not soup:
                    continue
                
                # 곡명 추출
                song_title = self._extract_song_title(soup)
                if not song_title:
                    logger.warning("❌ 곡명 추출 실패")
                    continue
                
                # 아티스트명 추출
                artist_name = self._extract_artist_name(soup)
                if not artist_name:
                    logger.warning("❌ 아티스트명 추출 실패, 검색한 값 사용")
                    artist_name = target_song_info['artist_ko'] # 국문 아티스트명 사용
                
                # 곡명과 아티스트명 검증 (한글/영문 제목과 아티스트명 모두 사용)
                comparison_result = compare_song_info_multilang(
                    song_title, artist_name, 
                    target_song_info['title_ko'], 
                    target_song_info.get('title_en', ''),
                    target_song_info['artist_ko'], 
                    target_song_info.get('artist_en', '')
                )
                
                if not comparison_result['both_match']:
                    logger.warning(f"❌ 매칭 실패: {comparison_result}")
                    continue
                
                # 조회수 정보 추출
                view_data = self._extract_view_count(soup)
                
                # 결과 반환 (실제 추출된 정보 사용)
                result = {
                    'song_title': song_title,
                    'artist_name': artist_name,
                    'views': view_data.get('views', -1),
                    'listeners': view_data.get('listeners', -1),
                    'crawl_date': get_current_timestamp(),
                    'song_id': target_song_info.get('song_id')  # target_song_info에서 song_id 가져오기
                }
                
                logger.info(f"✅ '{song_title}' - '{artist_name}' 파싱 성공!")
                return result
                
            except Exception as e:
                logger.error(f"❌ 파싱 시도 {attempt+1}/{GenieSettings.MAX_PARSE_ATTEMPTS} 실패: {e}", exc_info=True)
                continue
        
        logger.warning(f"❌ '{target_song_info['title_ko']}' 파싱 실패 - 모든 시도 실패")
        return None
    
    def _extract_song_title(self, soup) -> str:
        """곡명 추출"""
        song_title_tag = soup.select_one(GenieSelectors.SONG_TITLE)
        if song_title_tag:
            song_title = song_title_tag.text.strip()
            logger.info(f"✅ 곡명 추출 성공: {song_title}")
            return song_title
        return None
    
    def _extract_artist_name(self, soup) -> str:
        """아티스트명 추출"""
        try:
            # 곡 정보 페이지에서 아티스트명 추출 시도
            for selector in GenieSelectors.ARTIST_SELECTORS:
                artist_tag = soup.select_one(selector)
                if artist_tag:
                    artist_name = artist_tag.text.strip()
                    logger.info(f"✅ 아티스트명 추출 성공: {artist_name}")
                    return artist_name
            
            logger.warning("❌ 아티스트명 추출 실패: 해당 selector를 찾지 못함")
            return None
            
        except Exception as e:
            logger.error(f"❌ 아티스트명 추출 실패: {e}")
            return None
    
    def _extract_view_count(self, soup) -> dict:
        """조회수 정보 추출"""
        try:
            # 더 정확한 셀렉터 사용
            total_container = soup.select_one('.daily-chart .total')
            if total_container:
                # 첫 번째 div (전체 청취자수)
                first_div = total_container.select_one('div:first-child')
                # 두 번째 div (전체 재생수)
                second_div = total_container.select_one('div:nth-child(2)')
                
                total_person_count = -999
                total_play_count = -999
                
                if first_div:
                    p_tag = first_div.select_one('p')
                    if p_tag:
                        try:
                            total_person_count = int(p_tag.text.replace(',', '').strip())
                            logger.info(f"✅ 전체 청취자수 추출 성공: {total_person_count}")
                        except (ValueError, TypeError) as e:
                            logger.warning(f"❌ 전체 청취자수 변환 실패: {e}")
                
                if second_div:
                    p_tag = second_div.select_one('p')
                    if p_tag:
                        try:
                            total_play_count = int(p_tag.text.replace(',', '').strip())
                            logger.info(f"✅ 전체 재생수 추출 성공: {total_play_count}")
                        except (ValueError, TypeError) as e:
                            logger.warning(f"❌ 전체 재생수 변환 실패: {e}")
                
                return {
                    'views': total_play_count,
                    'listeners': total_person_count
                }
            
            logger.warning("❌ 통계 정보 컨테이너를 찾을 수 없음")
            return {'views': -999, 'listeners': -999}
            
        except Exception as e:
            logger.error(f"❌ 조회수 정보 추출 실패: {e}")
            return {'views': -999, 'listeners': -999} 