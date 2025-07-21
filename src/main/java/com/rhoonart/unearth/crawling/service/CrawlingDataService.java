package com.rhoonart.unearth.crawling.service;

import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.common.util.DataAuthorityService;
import com.rhoonart.unearth.crawling.dto.CrawlingDataResponseDto;
import com.rhoonart.unearth.crawling.dto.CrawlingDataWithSongInfoDto;
import com.rhoonart.unearth.crawling.entity.CrawlingData;
import com.rhoonart.unearth.crawling.entity.PlatformType;
import com.rhoonart.unearth.crawling.repository.CrawlingDataRepository;
import com.rhoonart.unearth.crawling.repository.CrawlingPeriodRepository;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.song.repository.SongInfoRepository;
import com.rhoonart.unearth.user.dto.UserDto;
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
    private final CrawlingPeriodRepository crawlingPeriodRepository;
    private final CrawlingPeriodService crawlingPeriodService;
    private final DataAuthorityService dataAuthorityService;

    public CrawlingDataWithSongInfoDto getCrawlingDataWithFilters(UserDto userDto, String songId, LocalDate startDate,
                                                                  LocalDate endDate, PlatformType platform, int page, int size) {
        // 음원 존재 여부 확인
        SongInfo songInfo = songInfoRepository.findById(songId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "음원을 찾을 수 없습니다."));

        // 사용자 권한 확인
        dataAuthorityService.isAccessSongData(userDto, songInfo);

        // 1. Pageable 생성 (페이지는 0부터 시작하므로 -1)
        Pageable pageable = PageRequest.of(page - 1, size);

        // 2. DB 레벨에서 페이징된 데이터 조회
        Page<CrawlingData> pagedResult = crawlingDataRepository.findPagedCrawlingData(
                songId, platform, startDate, endDate, pageable);

        // 3. 날짜별로 그룹화
        Map<LocalDate, List<CrawlingData>> dataByDate = pagedResult.getContent().stream()
                .collect(Collectors.groupingBy(data -> data.getCreatedAt().toLocalDate()));

        // 4. 날짜별로 그룹화된 데이터 생성
        List<CrawlingDataResponseDto.DateGroupedData> groupedDataList = new ArrayList<>();

        for (Map.Entry<LocalDate, List<CrawlingData>> entry : dataByDate.entrySet()) {
            LocalDate currentDate = entry.getKey();
            List<CrawlingData> currentDataList = entry.getValue();

            // 영상 정보 조회 (startDate인 날에만)
            List<CrawlingDataResponseDto.VideoInfo> videoInfos = new ArrayList<>();
            boolean isStartDate = crawlingPeriodService.isStartDate(songId, currentDate);

            if (isStartDate) {
                videoInfos = crawlingPeriodService.getVideoInfosForDate(songId, currentDate);
            }

            // 플랫폼별 데이터 생성
            List<CrawlingDataResponseDto.DateGroupedData.PlatformData> platformDataList = new ArrayList<>();

            for (CrawlingData currentData : currentDataList) {
                // 이전날 데이터를 DB에서 직접 조회
                LocalDate previousDate = currentDate.minusDays(1);
                Optional<CrawlingData> previousDataOpt = crawlingDataRepository
                        .findBySongIdAndPlatformAndDate(songId, currentData.getPlatform(), previousDate);

                long viewsIncrease = -1; // 기본값: 이전 데이터 없음
                long listenersIncrease = -1; // 기본값: 이전 데이터 없음

                if (previousDataOpt.isPresent()) {
                    CrawlingData previousData = previousDataOpt.get();

                    // 조회수 증가량 계산
                    if (currentData.getViews() != -999 &&  currentData.getViews() != -1 && previousData.getViews() != -999 && previousData.getViews() != -1) {
                        viewsIncrease = currentData.getViews() - previousData.getViews();
                    }

                    // 청취자수 증가량 계산
                    if (currentData.getListeners() != -999 &&  currentData.getListeners() != -1 && previousData.getListeners() != -999 && previousData.getListeners() != -1) {
                        listenersIncrease = currentData.getListeners() - previousData.getListeners();
                    }
                }

                CrawlingDataResponseDto.DateGroupedData.PlatformData platformData = CrawlingDataResponseDto.DateGroupedData.PlatformData
                        .builder()
                        .platform(currentData.getPlatform())
                        .views(currentData.getViews())
                        .listeners(currentData.getListeners())
                        .viewsIncrease(viewsIncrease)
                        .listenersIncrease(listenersIncrease)
                        .build();

                platformDataList.add(platformData);
            }

            CrawlingDataResponseDto.DateGroupedData groupedData = CrawlingDataResponseDto.DateGroupedData.builder()
                    .date(currentDate)
                    .videoInfos(videoInfos)
                    .platformDataList(platformDataList)
                    .build();

            groupedDataList.add(groupedData);
        }

        List<CrawlingDataResponseDto> resultList = new ArrayList<>();
        for (CrawlingDataResponseDto.DateGroupedData groupedData : groupedDataList) {
            for (CrawlingDataResponseDto.DateGroupedData.PlatformData platformData : groupedData
                    .getPlatformDataList()) {
                CrawlingDataResponseDto dto = CrawlingDataResponseDto.builder()
                        .date(groupedData.getDate())
                        .platform(platformData.getPlatform())
                        .views(platformData.getViews())
                        .listeners(platformData.getListeners())
                        .viewsIncrease(platformData.getViewsIncrease())
                        .listenersIncrease(platformData.getListenersIncrease())
                        .videoInfos(groupedData.getVideoInfos())
                        .build();

                resultList.add(dto);
            }
        }

        return CrawlingDataWithSongInfoDto.builder()
                .songInfo(songInfo)
                .crawlingDataList(resultList)
                .groupedDataList(groupedDataList)
                .totalPages(pagedResult.getTotalPages())
                .totalElements(pagedResult.getTotalElements())
                .currentPage(page)
                .pageSize(size)
                .build();
    }



}
