package com.rhoonart.unearth.crawling.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 페이지네이션 정보 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageInfoDto {
    private int totalPages;
    private long totalElements;
    private int currentPage;
    private int pageSize;

    /**
     * 현재 페이지가 첫 페이지인지 확인
     */
    public boolean isFirstPage() {
        return currentPage == 0;
    }

    /**
     * 현재 페이지가 마지막 페이지인지 확인
     */
    public boolean isLastPage() {
        return currentPage >= totalPages - 1;
    }

    /**
     * 다음 페이지가 있는지 확인
     */
    public boolean hasNext() {
        return currentPage < totalPages - 1;
    }

    /**
     * 이전 페이지가 있는지 확인
     */
    public boolean hasPrevious() {
        return currentPage > 0;
    }
}