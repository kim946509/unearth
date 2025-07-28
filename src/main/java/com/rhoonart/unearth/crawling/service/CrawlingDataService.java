package com.rhoonart.unearth.crawling.service;

import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.common.util.DataAuthorityService;
import com.rhoonart.unearth.common.util.ValidateInput;
import com.rhoonart.unearth.crawling.dto.CrawlingDataDto;
import com.rhoonart.unearth.crawling.dto.CrawlingDataWithSongInfoDto;
import com.rhoonart.unearth.crawling.dto.VideoInfoDto;
import com.rhoonart.unearth.crawling.dto.PageInfoDto;
import com.rhoonart.unearth.crawling.dto.DateGroupedCrawlingDataDto;
import com.rhoonart.unearth.crawling.entity.CrawlingData;
import com.rhoonart.unearth.crawling.entity.PlatformType;
import com.rhoonart.unearth.crawling.repository.CrawlingDataRepository;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.song.repository.SongInfoRepository;
import com.rhoonart.unearth.user.dto.UserDto;
import com.rhoonart.unearth.user.exception.ForbiddenException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlingDataService {

        private final SongInfoRepository songInfoRepository;
        private final CrawlingDataRepository crawlingDataRepository;
        private final CrawlingPeriodService crawlingPeriodService;
        private final DataAuthorityService dataAuthorityService;

        public CrawlingDataWithSongInfoDto getCrawlingDataWithFilters(UserDto userDto, String songId,
                        String startDateStr,
                        String endDateStr, String platformTypeStr, int page, int days) {
                // 음원 존재 여부 확인
                SongInfo songInfo = songInfoRepository.findById(songId)
                                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "음원을 찾을 수 없습니다."));

                // 사용자 권한 확인
                if (!dataAuthorityService.isAccessSongData(userDto, songInfo)) {
                        throw new ForbiddenException();
                }

                // 입력 값 검증 및 파싱
                LocalDate startDate = ValidateInput.parseDate(startDateStr);
                LocalDate endDate = ValidateInput.parseDate(endDateStr);

                PlatformType platform = null;
                if (platformTypeStr != null && !platformTypeStr.isEmpty()) {
                        platform = PlatformType.fromString(platformTypeStr);
                }

                // 일수를 페이지 크기로 변환
                int size = ValidateInput.restrictCrawlingDataPageSize(days);
                page = ValidateInput.calculatePageNumber(page);

                // 1. Pageable 생성 (실제로 데이터 상 페이지는 0부터 시작하므로 -1)
                Pageable pageable = PageRequest.of(page - 1, size);

                // 2. DB 레벨에서 페이징된 데이터 조회
                Page<CrawlingData> pagedResult = crawlingDataRepository.findPagedCrawlingData(
                                songId, platform, startDate, endDate, pageable);

                // 3. 날짜별로 그룹화
                Map<LocalDate, List<CrawlingData>> dataByDate = pagedResult.getContent().stream()
                                .collect(Collectors.groupingBy(data -> data.getCreatedAt().toLocalDate()));

                // 4. 날짜별로 그룹화된 데이터 리스트 생성
                List<DateGroupedCrawlingDataDto> groupedDataList = new ArrayList<>();

                // 날짜 순서대로 정렬 (최신순)
                List<LocalDate> sortedDates = dataByDate.keySet().stream()
                                .sorted((date1, date2) -> date2.compareTo(date1)) // 최신순
                                .toList();

                for (LocalDate currentDate : sortedDates) {
                        List<CrawlingData> currentDataList = dataByDate.get(currentDate);

                        // 영상 정보 조회 (모든 날짜에 대해 조회)
                        List<VideoInfoDto> videoInfoList = crawlingPeriodService.getVideoInfosForDate(songId,
                                        currentDate);

                        // 해당 날짜의 플랫폼별 데이터 생성
                        List<CrawlingDataDto> currentDateDataList = new ArrayList<>();

                        for (CrawlingData currentData : currentDataList) {
                                // 이전날 데이터를 DB에서 직접 조회
                                LocalDate previousDate = currentDate.minusDays(1);
                                Optional<CrawlingData> previousDataOpt = crawlingDataRepository
                                                .findBySongIdAndPlatformAndDate(songId, currentData.getPlatform(),
                                                                previousDate);

                                long viewsIncrease = -1; // 기본값: 이전 데이터 없음
                                long listenersIncrease = -1; // 기본값: 이전 데이터 없음

                                if (previousDataOpt.isPresent()) {
                                        CrawlingData previousData = previousDataOpt.get();

                                        // 조회수 증가량 계산
                                        viewsIncrease = CalculateIncreaseDataService.calculateIncrease(
                                                        currentData.getViews(),
                                                        previousData.getViews());

                                        // 청취자수 증가량 계산
                                        listenersIncrease = CalculateIncreaseDataService.calculateIncrease(
                                                        currentData.getListeners(),
                                                        previousData.getListeners());

                                }

                                // 그룹화된 데이터용 CrawlingDataDto 생성
                                CrawlingDataDto crawlingDataDto = CrawlingDataDto.ofBasicData(
                                                currentDate,
                                                currentData.getPlatform(),
                                                currentData.getViews(),
                                                currentData.getListeners(),
                                                viewsIncrease,
                                                listenersIncrease);

                                currentDateDataList.add(crawlingDataDto);
                        }

                        // 날짜별 그룹화된 데이터 생성
                        DateGroupedCrawlingDataDto groupedData = DateGroupedCrawlingDataDto.builder()
                                        .date(currentDate)
                                        .dataList(currentDateDataList)
                                        .videoInfos(videoInfoList) // 영상 정보는 날짜별로 한 번만
                                        .build();

                        groupedDataList.add(groupedData);
                }

                // 5. 페이지네이션 정보 생성
                PageInfoDto pageInfo = PageInfoDto.builder()
                                .totalPages(pagedResult.getTotalPages())
                                .totalElements(pagedResult.getTotalElements())
                                .currentPage(page)
                                .pageSize(size)
                                .build();

                // 6. 최종 응답 생성
                return CrawlingDataWithSongInfoDto.of(songInfo, groupedDataList)
                                .withPageInfo(pageInfo);
        }
}
