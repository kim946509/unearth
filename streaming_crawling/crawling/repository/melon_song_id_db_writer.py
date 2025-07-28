"""
Melon 곡 ID DB 저장 로직
"""
import logging
import django
from django.db import transaction
from crawling.models import SongInfo

logger = logging.getLogger(__name__)

def save_melon_song_id_to_db(song_id, melon_song_id):
    """
    멜론 곡 ID를 데이터베이스에 저장
    
    Args:
        song_id (str): 곡 ID (SongInfo의 PK)
        melon_song_id (str): 멜론 곡 ID
        
    Returns:
        bool: 저장 성공 여부
    """
    try:
        with transaction.atomic():
            # SongInfo 객체 조회
            try:
                song_info = SongInfo.objects.get(id=song_id)
            except SongInfo.DoesNotExist:
                logger.error(f"❌ 곡 정보를 찾을 수 없음: {song_id}")
                return False
            
            # 이미 melon_song_id가 있는지 확인
            if song_info.melon_song_id:
                logger.warning(f"⚠️ 이미 멜론 곡 ID가 존재함: {song_info.melon_song_id} -> {melon_song_id}")
            
            # melon_song_id 업데이트
            song_info.melon_song_id = melon_song_id
            song_info.save(update_fields=['melon_song_id'])
            
            logger.info(f"✅ 멜론 곡 ID 저장 완료: {song_id} -> {melon_song_id}")
            return True
            
    except Exception as e:
        logger.error(f"❌ 멜론 곡 ID 저장 실패: {e}", exc_info=True)
        return False

def get_song_info_for_melon_search(song_id):
    """
    멜론 검색을 위한 곡 정보 조회
    
    Args:
        song_id (str): 곡 ID (SongInfo의 PK)
        
    Returns:
        dict: 곡 정보 또는 None
    """
    try:
        song_info = SongInfo.objects.get(id=song_id)
        
        return {
            'song_id': song_info.id,
            'title_ko': song_info.title_ko,
            'title_en': song_info.title_en,
            'artist_ko': song_info.artist_ko,
            'artist_en': song_info.artist_en,
            'melon_song_id': song_info.melon_song_id
        }
        
    except SongInfo.DoesNotExist:
        logger.error(f"❌ 곡 정보를 찾을 수 없음: {song_id}")
        return None
    except Exception as e:
        logger.error(f"❌ 곡 정보 조회 실패: {e}", exc_info=True)
        return None
