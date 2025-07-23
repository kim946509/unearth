package com.rhoonart.unearth.common.util;

import com.rhoonart.unearth.user.exception.BadRequestException;
import java.time.LocalDate;

public class ValidateInput {

    public static LocalDate parseDate(String dateStr) {
        LocalDate date = null;
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                date = LocalDate.parse(dateStr);
            } catch (Exception e) {
                throw new BadRequestException("날짜 형식이 올바르지 않습니다: " + dateStr);
            }
        }

        return date;
    }

    public static int restrictPageSize(int pageSize) {
        if (pageSize != 10 && pageSize != 30 && pageSize != 50) {
            pageSize = 10; // 기본값으로 10 설정
        }

        return pageSize;
    }

    /**
     * 크롤링 데이터 일수를 검증하고 유효한 값으로 보정
     * 
     * @param days 사용자가 요청한 일수
     * @return 유효한 일수 (5, 7, 14, 30 중 하나)
     */
    public static int restrictCrawlingDataDays(int days) {
        if (days != 5 && days != 7 && days != 14 && days != 30) {
            days = 7; // 기본값으로 7일 설정
        }
        return days;
    }

    /**
     * 크롤링 데이터 일수를 페이지 크기로 변환
     * 각 날짜별로 4개 플랫폼 데이터가 있으므로 일수 * 4로 계산
     * 
     * @param days 일수
     * @return 페이지 크기
     */
    public static int convertDaysToPageSize(int days) {
        final int PLATFORM_COUNT = 4; // 멜론, 유튜브뮤직, 유튜브, 지니
        return days * PLATFORM_COUNT;
    }

    /**
     * 크롤링 데이터 페이지 크기를 검증하고 유효한 값으로 변환
     * 
     * @param requestedDays 사용자가 요청한 일수
     * @return 유효한 페이지 크기
     */
    public static int restrictCrawlingDataPageSize(int requestedDays) {
        int validDays = restrictCrawlingDataDays(requestedDays);
        return convertDaysToPageSize(validDays);
    }

    public static int calculatePageNumber(int page) {
        return Math.max(1, page); // 페이지 번호는 1부터 시작
    }
}
