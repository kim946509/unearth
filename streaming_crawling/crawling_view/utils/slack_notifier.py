"""
Slack 알림 유틸리티
"""
import logging
import os
import requests
from typing import Optional
from dotenv import load_dotenv

# .env 파일 로드
load_dotenv()

logger = logging.getLogger(__name__)

class SlackNotifier:
    """Slack 알림 클래스"""
    
    def __init__(self, webhook_url: Optional[str] = None):
        """
        Args:
            webhook_url (str, optional): Slack Webhook URL. None이면 환경변수에서 가져옴
        """
        self.webhook_url = webhook_url or os.getenv('SLACK_WEBHOOK_URL')
        
        if not self.webhook_url:
            logger.warning("⚠️ SLACK_WEBHOOK_URL 환경변수가 설정되지 않았습니다.")
    
    def send_message(self, message: str, channel: Optional[str] = None) -> bool:
        """
        Slack으로 메시지 전송
        
        Args:
            message (str): 전송할 메시지
            channel (str, optional): 전송할 채널. None이면 기본 채널
            
        Returns:
            bool: 전송 성공 여부
        """
        if not self.webhook_url:
            logger.error("❌ Slack Webhook URL이 설정되지 않았습니다.")
            return False
        
        try:
            payload = {
                'text': message
            }
            
            # 채널이 지정된 경우 추가
            if channel:
                payload['channel'] = channel
            
            response = requests.post(
                self.webhook_url,
                json=payload,
                timeout=10
            )
            
            if response.status_code == 200:
                logger.info("✅ Slack 메시지 전송 성공")
                return True
            else:
                logger.error(f"❌ Slack 메시지 전송 실패: {response.status_code} - {response.text}")
                return False
                
        except requests.exceptions.RequestException as e:
            logger.error(f"❌ Slack 메시지 전송 중 오류: {e}")
            return False
        except Exception as e:
            logger.error(f"❌ Slack 메시지 전송 중 예상치 못한 오류: {e}")
            return False
    
    def send_error_message(self, error_message: str, context: str = "") -> bool:
        """
        에러 메시지 전송 (에러 형식으로 포맷)
        
        Args:
            error_message (str): 에러 메시지
            context (str): 에러 컨텍스트 (선택사항)
            
        Returns:
            bool: 전송 성공 여부
        """
        formatted_message = f"💥 *크롤링 에러 발생*\n"
        if context:
            formatted_message += f"📍 컨텍스트: {context}\n"
        formatted_message += f"❌ 에러: {error_message}"
        
        return self.send_message(formatted_message)
    
    def send_success_message(self, success_message: str) -> bool:
        """
        성공 메시지 전송 (성공 형식으로 포맷)
        
        Args:
            success_message (str): 성공 메시지
            
        Returns:
            bool: 전송 성공 여부
        """
        formatted_message = f"✅ *크롤링 완료*\n{success_message}"
        return self.send_message(formatted_message)


# 전역 인스턴스 생성
_slack_notifier = None

def get_slack_notifier() -> SlackNotifier:
    """전역 SlackNotifier 인스턴스 반환"""
    global _slack_notifier
    if _slack_notifier is None:
        _slack_notifier = SlackNotifier()
    return _slack_notifier

def send_slack_message(message: str, channel: Optional[str] = None) -> bool:
    """
    Slack으로 메시지 전송 (편의 함수)
    
    Args:
        message (str): 전송할 메시지
        channel (str, optional): 전송할 채널
        
    Returns:
        bool: 전송 성공 여부
    """
    notifier = get_slack_notifier()
    return notifier.send_message(message, channel)

def send_slack_error(error_message: str, context: str = "") -> bool:
    """
    에러 메시지 전송 (편의 함수)
    
    Args:
        error_message (str): 에러 메시지
        context (str): 에러 컨텍스트
        
    Returns:
        bool: 전송 성공 여부
    """
    notifier = get_slack_notifier()
    return notifier.send_error_message(error_message, context)

def send_slack_success(success_message: str) -> bool:
    """
    성공 메시지 전송 (편의 함수)
    
    Args:
        success_message (str): 성공 메시지
        
    Returns:
        bool: 전송 성공 여부
    """
    notifier = get_slack_notifier()
    return notifier.send_success_message(success_message) 