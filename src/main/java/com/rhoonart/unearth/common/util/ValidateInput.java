package com.rhoonart.unearth.common.util;

import com.rhoonart.unearth.user.exception.BadRequestException;
import java.time.LocalDate;

public class ValidateInput {

    public static LocalDate parseDate(String dateStr) {
        LocalDate date = null;
        if(dateStr != null && !dateStr.isEmpty()) {
            try {
                date = LocalDate.parse(dateStr);
            } catch (Exception e) {
                throw new BadRequestException("날짜 형식이 올바르지 않습니다: " + dateStr);
            }
        }

        return date;
    }

    public static int restrictPageSize(int pageSize){
        if (pageSize != 10 && pageSize != 20 && pageSize != 50 && pageSize != 100){
            pageSize = 10; // 기본값으로 10 설정
        }

        return pageSize;
    }

    public static int calculatePageNumber(int page) {
        return Math.max(1, page); // 페이지 번호는 1부터 시작
    }
}
