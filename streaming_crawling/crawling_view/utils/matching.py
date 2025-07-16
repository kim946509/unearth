"""
문자열 매칭 관련 유틸리티
"""
import logging
from difflib import SequenceMatcher
from .utils import normalize_text

logger = logging.getLogger(__name__)

# 키워드 유사도 매칭 임계값 (기본 30%)
KEYWORD_SIMILARITY_THRESHOLD = 0.3

def compare_song_info(found_title, found_artist, target_info):
    """
    검색된 곡 정보와 목표 곡 정보를 비교
    
    Args:
        found_title (str): 검색된 곡 제목
        found_artist (str): 검색된 아티스트명
        target_info (dict): 목표 곡 정보 (title_ko, title_en, artist_ko, artist_en)
        
    Returns:
        dict: 매칭 결과
    """
    # 공백 제거 정규화
    def normalize_no_space(text):
        """공백을 제거한 정규화"""
        normalized = normalize_text(text)
        return normalized.replace(' ', '') if normalized else ''
    
    # 정규화 전 원본 값 로깅
    logger.info(f"🔍 매칭 시작:")
    logger.info(f"  찾은 제목: '{found_title}'")
    logger.info(f"  찾은 아티스트: '{found_artist}'")
    logger.info(f"  목표 제목(국문): '{target_info['title_ko']}'")
    logger.info(f"  목표 제목(영문): '{target_info['title_en']}'")
    logger.info(f"  목표 아티스트(국문): '{target_info['artist_ko']}'")
    logger.info(f"  목표 아티스트(영문): '{target_info['artist_en']}'")
    
    # 정규화
    found_title = normalize_text(found_title)
    found_artist = normalize_text(found_artist)
    found_title_no_space = normalize_no_space(found_title)
    found_artist_no_space = normalize_no_space(found_artist)
    
    target_title_ko = normalize_text(target_info['title_ko'])
    target_title_en = normalize_text(target_info['title_en'])
    target_artist_ko = normalize_text(target_info['artist_ko'])
    target_artist_en = normalize_text(target_info['artist_en'])
    
    target_title_ko_no_space = normalize_no_space(target_title_ko)
    target_title_en_no_space = normalize_no_space(target_title_en)
    target_artist_ko_no_space = normalize_no_space(target_artist_ko)
    target_artist_en_no_space = normalize_no_space(target_artist_en)
    
    # 정규화 후 값 로깅
    logger.info(f"🔍 정규화 후:")
    logger.info(f"  찾은 제목: '{found_title}' (공백제거: '{found_title_no_space}')")
    logger.info(f"  찾은 아티스트: '{found_artist}' (공백제거: '{found_artist_no_space}')")
    logger.info(f"  목표 제목(국문): '{target_title_ko}' (공백제거: '{target_title_ko_no_space}')")
    logger.info(f"  목표 제목(영문): '{target_title_en}' (공백제거: '{target_title_en_no_space}')")
    logger.info(f"  목표 아티스트(국문): '{target_artist_ko}' (공백제거: '{target_artist_ko_no_space}')")
    logger.info(f"  목표 아티스트(영문): '{target_artist_en}' (공백제거: '{target_artist_en_no_space}')")
    
    # 1단계: 정확 매칭 + 부분 매칭
    title_match_exact, artist_match_exact = exact_and_partial_match(
        found_title_no_space,
        [target_title_ko_no_space, target_title_en_no_space],
        found_artist_no_space,
        [target_artist_ko_no_space, target_artist_en_no_space]
    )
    
    # 2단계: 키워드 유사도 매칭 (실패한 부분만)
    title_match_keyword = False
    artist_match_keyword = False
    if not title_match_exact or not artist_match_exact:
        title_match_keyword, artist_match_keyword = keyword_similarity_match(
            found_title,
            [target_title_ko, target_title_en],
            found_artist,
            [target_artist_ko, target_artist_en]
        )
    
    # 3단계: 유사도 매칭
    title_ko_ratio = SequenceMatcher(None, found_title, target_title_ko).ratio()
    title_en_ratio = SequenceMatcher(None, found_title, target_title_en).ratio()
    artist_ko_ratio = SequenceMatcher(None, found_artist, target_artist_ko).ratio()
    artist_en_ratio = SequenceMatcher(None, found_artist, target_artist_en).ratio()
    
    title_match_ratio = title_ko_ratio > 0.8 or title_en_ratio > 0.8
    artist_match_ratio = artist_ko_ratio > 0.8 or artist_en_ratio > 0.8
    
    # 최종 매칭 결과
    title_match = title_match_exact or title_match_keyword or title_match_ratio
    artist_match = artist_match_exact or artist_match_keyword or artist_match_ratio
    
    # 매칭 타입 결정
    if title_match_exact and artist_match_exact:
        match_type = 'exact_partial'
    elif title_match_keyword and artist_match_keyword:
        match_type = 'keyword_similarity'
    elif title_match_ratio and artist_match_ratio:
        match_type = 'ratio'
    elif title_match and artist_match:
        match_type = 'mixed'
    else:
        match_type = 'none'
    
    # 매칭 상세 정보
    match_details = {
        'title_match': title_match,
        'artist_match': artist_match,
        'both_match': title_match and artist_match,
        'match_type': match_type,
        'details': {
            'exact': {
                'title': title_match_exact,
                'artist': artist_match_exact
            },
            'keyword': {
                'title': title_match_keyword,
                'artist': artist_match_keyword
            },
            'ratio': {
                'title_ko': title_ko_ratio,
                'title_en': title_en_ratio,
                'artist_ko': artist_ko_ratio,
                'artist_en': artist_en_ratio
            }
        }
    }
    
    logger.info(f"🔍 매칭 결과: {match_details}")
    return match_details

def exact_and_partial_match(found_text, target_texts, found_artist, target_artists):
    # 텍스트 매칭: 정확히 일치하거나 한쪽이 다른 쪽에 포함
    text_match = False
    for target in target_texts:
        if not target:
            continue
            
        # 정확 매칭
        if found_text == target:
            logger.info(f"✅ 제목 정확 매칭: '{found_text}' == '{target}'")
            text_match = True
            break
            
        # 포함 매칭
        if len(found_text) >= 3 and found_text in target:
            logger.info(f"✅ 제목 포함 매칭: '{found_text}' in '{target}'")
            text_match = True
            break
            
        if len(target) >= 3 and target in found_text:
            logger.info(f"✅ 제목 포함 매칭: '{target}' in '{found_text}'")
            text_match = True
            break
            
        # 괄호 안의 영어 제목 제거 후 매칭
        if _match_title_with_brackets(found_text, target):
            logger.info(f"✅ 제목 괄호 매칭: '{found_text}' vs '{target}'")
            text_match = True
            break
    
    if not text_match:
        pass
    
    # 아티스트 매칭: 더 유연한 매칭
    artist_match = False
    for target in target_artists:
        if not target:
            continue
            
        if _match_artist_names(found_artist, target):
            logger.info(f"✅ 아티스트 매칭 성공: '{found_artist}' vs '{target}'")
            artist_match = True
            break
    
    if not artist_match:
        pass
    
    logger.info(f"🔍 정확/부분 매칭 결과: 텍스트={text_match}, 아티스트={artist_match}")
    return text_match, artist_match

def _match_title_with_brackets(found_title, target_title):
    """
    괄호 안의 다양한 내용을 고려한 제목 매칭
    
    Args:
        found_title (str): 찾은 제목 (예: "어떻게 이별까지 사랑하겠어, 널 사랑하는 거지(How can I love the heartbreak, you're the one I love)")
        target_title (str): 목표 제목 (예: "어떻게 이별까지 사랑하겠어, 널 사랑하는 거지")
        
    Returns:
        bool: 매칭 성공 여부
    """
    import re
    
    # 1. 괄호 안의 모든 내용 제거한 버전으로 매칭
    cleaned_found = re.sub(r'\([^)]*\)', '', found_title).strip()
    
    if cleaned_found == target_title:
        logger.debug(f"괄호 제거 후 정확 매칭: '{cleaned_found}' == '{target_title}'")
        return True
    
    if len(cleaned_found) >= 3 and cleaned_found in target_title:
        logger.debug(f"괄호 제거 후 포함 매칭: '{cleaned_found}' in '{target_title}'")
        return True
    
    if len(target_title) >= 3 and target_title in cleaned_found:
        logger.debug(f"괄호 제거 후 포함 매칭: '{target_title}' in '{cleaned_found}'")
        return True
    
    # 2. 괄호 안의 모든 내용을 추출해서 매칭 시도
    bracket_matches = re.findall(r'\(([^)]*)\)', found_title)
    for bracket_content in bracket_matches:
        bracket_content = bracket_content.strip()
        
        # 괄호 내용과 목표 제목 매칭
        if bracket_content == target_title:
            logger.debug(f"괄호 내용 정확 매칭: '{bracket_content}' == '{target_title}'")
            return True
        
        if len(bracket_content) >= 3 and bracket_content in target_title:
            logger.debug(f"괄호 내용 포함 매칭: '{bracket_content}' in '{target_title}'")
            return True
        
        if len(target_title) >= 3 and target_title in bracket_content:
            logger.debug(f"괄호 내용 포함 매칭: '{target_title}' in '{bracket_content}'")
            return True
    
    # 3. 목표 제목이 괄호 안에 있는지 확인
    if '(' in found_title and target_title in found_title:
        logger.debug(f"목표 제목이 괄호 안에 포함: '{target_title}' in '{found_title}'")
        return True
    
    # 4. 괄호 안의 영어 제목만 추출해서 매칭 (기존 로직 유지)
    english_bracket_match = re.search(r'\(([^)]*[a-zA-Z][^)]*)\)', found_title)
    if english_bracket_match:
        english_title = english_bracket_match.group(1).strip()
        
        # 영어 제목과 목표 제목 매칭
        if english_title == target_title:
            logger.debug(f"영어 괄호 내용 정확 매칭: '{english_title}' == '{target_title}'")
            return True
        
        if len(english_title) >= 3 and english_title in target_title:
            logger.debug(f"영어 괄호 내용 포함 매칭: '{english_title}' in '{target_title}'")
            return True
        
        if len(target_title) >= 3 and target_title in english_title:
            logger.debug(f"영어 괄호 내용 포함 매칭: '{target_title}' in '{english_title}'")
            return True
    
    return False

def _match_artist_names(artist1, artist2):
    """
    아티스트명 매칭 (유연한 방식)
    
    Args:
        artist1 (str): 첫 번째 아티스트명
        artist2 (str): 두 번째 아티스트명
        
    Returns:
        bool: 매칭 성공 여부
    """
    # 1. 정확 매칭
    if artist1 == artist2:
        return True
    
    # 2. 부분 문자열 매칭 (더 유연하게)
    if (len(artist1) >= 2 and artist1 in artist2) or (len(artist2) >= 2 and artist2 in artist1):
        return True
    
    # 3. 공통 키워드 매칭 (2글자 이상의 공통 부분)
    common_chars = set(artist1) & set(artist2)
    if len(common_chars) >= 2:
        # 공통 문자들이 연속적으로 나타나는지 확인
        for char in common_chars:
            if char in artist1 and char in artist2:
                # 각 아티스트명에서 해당 문자의 위치 확인
                pos1 = artist1.find(char)
                pos2 = artist2.find(char)
                # 같은 위치 근처에 있으면 매칭 가능성 높음
                if abs(pos1 - pos2) <= 2:
                    return True
    
    # 4. 특별한 케이스 처리
    special_cases = {
        ('악뮤', '악동뮤지션'): True,
        ('악동뮤지션', '악뮤'): True,
        ('akmu', '악동뮤지션'): True,
        ('악동뮤지션', 'akmu'): True,
    }
    
    if (artist1, artist2) in special_cases:
        return special_cases[(artist1, artist2)]
    
    return False

def keyword_similarity_match(found_text, target_texts, found_artist, target_artists):
    """
    키워드 유사도 매칭
    
    Args:
        found_text (str): 찾은 텍스트 (공백 포함)
        target_texts (list): 목표 텍스트 리스트 [국문, 영문] (공백 포함)
        found_artist (str): 찾은 아티스트 (공백 포함)
        target_artists (list): 목표 아티스트 리스트 [국문, 영문] (공백 포함)
        
    Returns:
        tuple: (text_match, artist_match)
    """
    def get_keywords(text):
        """텍스트에서 주요 키워드 추출 (2글자 이상의 단어들)"""
        if not text:
            return set()
        words = text.split()
        return {word for word in words if len(word) >= 2}
    
    def calculate_similarity(keywords1, keywords2):
        """키워드 유사도 계산 (자카드 유사도)"""
        if not keywords1 or not keywords2:
            return 0.0
        
        common_keywords = keywords1 & keywords2
        total_keywords = keywords1 | keywords2
        
        if not total_keywords:
            return 0.0
        
        return len(common_keywords) / len(total_keywords)
    
    # 키워드 추출
    found_text_keywords = get_keywords(found_text)
    found_artist_keywords = get_keywords(found_artist)
    
    # 각 언어별로 유사도 계산하여 가장 높은 값 사용
    text_similarities = [
        calculate_similarity(found_text_keywords, get_keywords(target))
        for target in target_texts if target
    ]
    artist_similarities = [
        calculate_similarity(found_artist_keywords, get_keywords(target))
        for target in target_artists if target
    ]
    
    text_similarity = max(text_similarities) if text_similarities else 0.0
    artist_similarity = max(artist_similarities) if artist_similarities else 0.0
    
    # 임계값 이상이면 매칭 성공
    text_match = text_similarity >= KEYWORD_SIMILARITY_THRESHOLD
    artist_match = artist_similarity >= KEYWORD_SIMILARITY_THRESHOLD
    
    logger.debug(f"키워드 유사도 매칭:")
    logger.debug(f"  텍스트 유사도: {text_similarity:.2f} → {text_match}")
    logger.debug(f"  아티스트 유사도: {artist_similarity:.2f} → {artist_match}")
    
    return text_match, artist_match 

def compare_song_info_multilang(found_title, found_artist, target_title_ko, target_title_en, target_artist_ko, target_artist_en=''):
    """
    국문/영문 조합을 모두 시도하여 하나라도 매칭되면 True 반환
    Args:
        found_title (str): 검색된 곡 제목
        found_artist (str): 검색된 아티스트명
        target_title_ko (str): 목표 곡 제목 (한글)
        target_title_en (str): 목표 곡 제목 (영문)
        target_artist_ko (str): 목표 아티스트명 (한글)
        target_artist_en (str): 목표 아티스트명 (영문, 선택사항)
    Returns:
        dict: 매칭 결과 (기존 compare_song_info 결과 + 어떤 조합에서 매칭됐는지)
    """
    logger.info(f"🔍 다국어 매칭 시작:")
    logger.info(f"  찾은 제목: '{found_title}'")
    logger.info(f"  찾은 아티스트: '{found_artist}'")
    
    results = []
    combos = []
    
    # 한글 제목과 한글 아티스트 (기본)
    if target_title_ko and target_artist_ko:
        combos.append((target_title_ko, target_artist_ko, 'ko/ko'))
    
    # 영문 제목과 영문 아티스트
    if target_title_en and target_artist_en:
        combos.append((target_title_en, target_artist_en, 'en/en'))
    
    # 한글 제목과 영문 아티스트
    if target_title_ko and target_artist_en:
        combos.append((target_title_ko, target_artist_en, 'ko/en'))
    
    # 영문 제목과 한글 아티스트
    if target_title_en and target_artist_ko:
        combos.append((target_title_en, target_artist_ko, 'en/ko'))
    
    if not combos:
        logger.warning(f"❌ 매칭할 수 있는 조합이 없음 (한글 제목: '{target_title_ko}', 영문 제목: '{target_title_en}', 한글 아티스트: '{target_artist_ko}', 영문 아티스트: '{target_artist_en}')")
        return {
            'both_match': False,
            'title_match': False,
            'artist_match': False,
            'match_type': 'none',
            'matched_combo': None,
            'details': {}
        }
    
    for tgt_title, tgt_artist, combo in combos:
        logger.info(f"🔍 조합 시도: {combo} (제목: '{tgt_title}', 아티스트: '{tgt_artist}')")
        
        result = compare_song_info(found_title, found_artist, {
            'title_ko': tgt_title,
            'title_en': tgt_title,
            'artist_ko': tgt_artist,
            'artist_en': tgt_artist
        })
        result['combo'] = combo
        results.append(result)
        
        logger.info(f"🔍 조합 {combo} 결과: {result['both_match']} (제목: {result['title_match']}, 아티스트: {result['artist_match']})")
        
        if result['both_match']:
            result['matched_combo'] = combo
            logger.info(f"✅ 매칭 성공! 조합: {combo}")
            return result
    
    # 모두 실패 시 마지막 결과 반환
    results[-1]['matched_combo'] = None
    logger.warning(f"❌ 모든 조합 매칭 실패")
    return results[-1] 