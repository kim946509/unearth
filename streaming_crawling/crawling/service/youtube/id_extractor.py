"""
YouTube URL에서 동영상 ID 추출 유틸리티
"""
import re
import logging
from urllib.parse import urlparse, parse_qs

logger = logging.getLogger(__name__)

def extract_youtube_id(url: str) -> str:
    """
    YouTube URL에서 동영상 ID를 추출합니다.
    
    지원하는 URL 형태:
    - https://www.youtube.com/watch?v=VIDEO_ID
    - https://youtu.be/VIDEO_ID
    - https://www.youtube.com/embed/VIDEO_ID
    - https://m.youtube.com/watch?v=VIDEO_ID
    
    Args:
        url (str): YouTube URL
        
    Returns:
        str: 동영상 ID (11자리) 또는 None
    """
    if not url or not isinstance(url, str):
        return None
    
    try:
        
        # 1. youtube.com/watch?v=VIDEO_ID 형태
        if 'youtube.com/watch' in url:
            parsed = urlparse(url)
            query_params = parse_qs(parsed.query)
            video_id = query_params.get('v', [None])[0]
            if video_id and len(video_id) == 11:
                logger.debug(f"✅ watch?v= 형태에서 ID 추출: {video_id}")
                return video_id
            
        # 2. youtu.be/VIDEO_ID 형태
        elif 'youtu.be/' in url:
            match = re.search(r'youtu\.be/([0-9A-Za-z_-]{11})', url)
            if match:
                video_id = match.group(1)
                logger.debug(f"✅ youtu.be 형태에서 ID 추출: {video_id}")
                return video_id
        
        
        # 3. youtube.com/embed/VIDEO_ID 형태
        elif 'youtube.com/embed/' in url:
            match = re.search(r'youtube\.com/embed/([0-9A-Za-z_-]{11})', url)
            if match:
                video_id = match.group(1)
                logger.debug(f"✅ embed 형태에서 ID 추출: {video_id}")
                return video_id
        
        # 4. 일반적인 패턴으로 11자리 ID 추출 
        match = re.search(r'(?:v=|/)([0-9A-Za-z_-]{11})', url)
        if match:
            video_id = match.group(1)
            logger.debug(f"✅ 일반 패턴에서 ID 추출: {video_id}")
            return video_id
        
        logger.warning(f"❌ YouTube ID 추출 실패: {url}")
        return None
        
    except Exception as e:
        logger.error(f"❌ YouTube ID 추출 중 오류: {url} - {e}")
        return None

def validate_youtube_id(video_id: str) -> bool:
    """
    YouTube 동영상 ID 유효성 검사
    
    Args:
        video_id (str): YouTube 동영상 ID
        
    Returns:
        bool: 유효한 ID 여부
    """
    if not video_id or not isinstance(video_id, str):
        return False
    
    # YouTube 동영상 ID는 11자리 영문자/숫자/하이픈/언더스코어
    return bool(re.match(r'^[0-9A-Za-z_-]{11}$', video_id))

def extract_multiple_youtube_ids(urls: list) -> dict:
    """
    여러 YouTube URL에서 동영상 ID를 일괄 추출
    
    Args:
        urls (list): YouTube URL 리스트
        
    Returns:
        dict: {url: video_id} 매핑 (실패한 URL은 제외)
    """
    result = {}
    for url in urls:
        video_id = extract_youtube_id(url)
        if video_id:
            result[url] = video_id
    
    logger.info(f"🔍 YouTube ID 추출 완료: {len(result)}개 성공 / {len(urls)}개 중")
    return result 