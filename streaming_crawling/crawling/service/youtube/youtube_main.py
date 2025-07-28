"""
YouTube 크롤링 메인 실행 파일 (YouTube API 사용)
"""
import logging
from crawling.service.youtube.youtube_api_service import YouTubeApiService
from crawling.service.youtube.id_extractor import extract_youtube_id, validate_youtube_id
from crawling.utils.utils import get_current_timestamp

logger = logging.getLogger(__name__)

def run_youtube_crawling(url_artist_song_id_list):
    """
    YouTube 크롤링 실행 (YouTube API 사용)
    
    Args:
        url_artist_song_id_list (list): 크롤링할 URL, 아티스트, song_id 리스트 [('url1', 'artist1', 'song_id1'), ('url2', 'artist2', 'song_id2'), ...]
    
    Returns:
        dict: 크롤링된 데이터 딕셔너리
    """
    logger.info(f"🎥 YouTube API 크롤링 시작 - 총 {len(url_artist_song_id_list)}개 URL")
    
    try:
        # YouTube API 서비스 초기화
        youtube_api = YouTubeApiService()
        
        # URL에서 video_id 추출
        video_id_to_song_mapping = {}
        valid_requests = []
        
        for url, artist_name, song_id in url_artist_song_id_list:
            try:
                # YouTube URL에서 video_id 추출
                video_id = extract_youtube_id(url)
                
                if video_id and validate_youtube_id(video_id):
                    video_id_to_song_mapping[video_id] = {
                        'song_id': song_id,
                        'artist_name': artist_name,
                        'url': url
                    }
                    valid_requests.append(video_id)
                else:
                    logger.warning(f"❌ 유효하지 않은 YouTube URL: {url}")
                    
            except Exception as e:
                logger.error(f"❌ URL 처리 실패 ({url}): {e}")
        
        if not valid_requests:
            logger.warning("⚠️ 유효한 YouTube URL이 없습니다.")
            return {}
        
        # YouTube API로 조회수 정보 가져오기
        logger.info(f"📊 YouTube API 호출 - {len(valid_requests)}개 비디오")
        video_data = youtube_api._get_video_statistics_batch(valid_requests)
        
        # 결과 구성
        results = {}
        success_count = 0
        
        for video_id, stats in video_data.items():
            if video_id in video_id_to_song_mapping:
                song_info = video_id_to_song_mapping[video_id]
                view_count = stats.get('view_count', -999)
                
                # 기존 인터페이스와 호환되는 형태로 결과 구성
                result = {
                    'song_id': song_info['song_id'],
                    'song_name': stats.get('title', '제목 없음'),
                    'artist_name': song_info['artist_name'],
                    'views': view_count,
                    'listeners': -1,  # YouTube는 청취자 수 제공 안함
                    'youtube_url': song_info['url'],
                    'extracted_date': get_current_timestamp()
                }
                
                results[song_info['song_id']] = result
                
                # 성공/실패 판단 (조회수가 유효한 경우만 성공)
                if view_count is not None and view_count != -999 and view_count >= 0:
                    success_count += 1
                    logger.debug(f"✅ YouTube API 크롤링 성공: {song_info['artist_name']} - {result['song_name']} (조회수: {result['views']})")
                else:
                    logger.warning(f"❌ YouTube API 크롤링 실패: {song_info['artist_name']} - 조회수: {view_count}")
        
        # 실패한 항목들도 기본 구조로 추가 (기존 로직과 호환)
        for url, artist_name, song_id in url_artist_song_id_list:
            if song_id not in results:
                results[song_id] = {
                    'song_id': song_id,
                    'song_name': None,
                    'artist_name': artist_name,
                    'views': None,
                    'listeners': -1,
                    'youtube_url': url,
                    'extracted_date': get_current_timestamp(),
                }
                logger.warning(f"❌ YouTube API 크롤링 실패: {artist_name}")
        
        logger.info(f"🎥 YouTube API 크롤링 완료 - 성공: {success_count}개 / 총 {len(url_artist_song_id_list)}개")
        
        return results
        
    except Exception as e:
        logger.error(f"❌ YouTube API 크롤링 실행 중 오류 발생: {e}", exc_info=True)
        
        # 오류 시 기본 구조로 결과 반환 (기존 로직과 호환)
        results = {}
        for url, artist_name, song_id in url_artist_song_id_list:
            results[song_id] = {
                'song_id': song_id,
                'song_name': None,
                'artist_name': artist_name,
                'views': None,
                'listeners': -1,
                'youtube_url': url,
                'extracted_date': get_current_timestamp(),
            }
        
        return results

if __name__ == "__main__":
    # 테스트용 실행
    test_urls = [
        ("https://www.youtube.com/watch?v=Sv2mIvMwrSY", "Jaerium", "test_song_id_1"),
        ("https://www.youtube.com/watch?v=R1CZTJ8hW0s", "Jaerium", "test_song_id_2"),
    ]
    
    results = run_youtube_crawling(test_urls)
    print(f"크롤링 결과: {len(results)}개")
    for song_id, result in results.items():
        print(f"[YouTube] 곡명: {result['song_name']}, 아티스트: {result['artist_name']}, "
              f"조회수: {result['views']}") 