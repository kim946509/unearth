package com.rhoonart.unearth.crawling.service;

import com.rhoonart.unearth.crawling.dto.CrawlingCsvDownloadDto;
import com.rhoonart.unearth.crawling.dto.CrawlingExecuteRequestDto;
import com.rhoonart.unearth.crawling.dto.CrawlingDataResponseDto;
import com.rhoonart.unearth.crawling.dto.CrawlingDataWithSongInfoDto;
import com.rhoonart.unearth.crawling.dto.CrawlingFailureDto;
import com.rhoonart.unearth.crawling.entity.CrawlingPeriod;
import com.rhoonart.unearth.crawling.entity.CrawlingData;
import com.rhoonart.unearth.crawling.entity.PlatformType;
import com.rhoonart.unearth.crawling.repository.CrawlingPeriodRepository;
import com.rhoonart.unearth.crawling.repository.CrawlingDataRepository;
import com.rhoonart.unearth.crawling.repository.CrawlingFailureRepository;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.song.repository.SongInfoRepository;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.common.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlingService {

    // 크롤링 실행 시간 (17시)
    private static final int CRAWLING_HOUR = 17;
    // 크롤링 기간 (30일)
    private static final int CRAWLING_PERIOD_DAYS = 30;

    private final CrawlingPeriodRepository crawlingPeriodRepository;
    private final CrawlingDataRepository crawlingDataRepository;
    private final CrawlingFailureRepository crawlingFailureRepository;
    private final SongInfoRepository songInfoRepository;
    private final CrawlingExecuteService crawlingExecuteService;

    @Transactional
    public void executeCrawling(CrawlingExecuteRequestDto dto) {
        // 1. 음원 조회
        SongInfo song = songInfoRepository.findById(dto.getSongId())
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "음원을 찾을 수 없습니다."));

        // 2. 크롤링 시작일과 종료일 자동 계산
        LocalDate startDate = calculateStartDate();
        LocalDate endDate = startDate.plusDays(CRAWLING_PERIOD_DAYS);

        // 3. CrawlingPeriod 생성 및 저장
        CrawlingPeriod crawlingPeriod = CrawlingPeriod.builder()
                .song(song)
                .startDate(startDate)
                .endDate(endDate)
                .isActive(true)
                .channel(dto.getChannel())
                .youtubeTitle(dto.getYoutubeTitle())
                .youtubeUrl(dto.getYoutubeUrl())
                .songOrder(dto.getSongOrder())
                .uploadAt(parseUploadAt(dto.getUploadAt()))
                .build();

        crawlingPeriodRepository.save(crawlingPeriod);

        // 4. 크롤링 실행 (비동기)
        crawlingExecuteService.executeSingleSongCrawling(song.getId());
    }

    @Transactional
    public void executeCrawlingOnly(String songId) {
        // 음원 조회
        SongInfo song = songInfoRepository.findById(songId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "음원을 찾을 수 없습니다."));

        // 크롤링 실행 (비동기)
        crawlingExecuteService.executeSingleSongCrawling(song.getId());
    }

    private LocalDate calculateStartDate() {
        return LocalDate.now();
    }

    private java.time.LocalDateTime parseUploadAt(String uploadAtStr) {
        if (uploadAtStr == null || uploadAtStr.trim().isEmpty()) {
            return null;
        }
        try {
            return java.time.LocalDateTime.parse(uploadAtStr);
        } catch (Exception e) {
            log.warn("영상 업로드 시점 파싱 실패: {}", uploadAtStr, e);
            return null;
        }
    }

    public CrawlingDataWithSongInfoDto getCrawlingDataWithFilters(String songId, LocalDate startDate,
            LocalDate endDate, PlatformType platform, int page, int size) {
        // 음원 존재 여부 확인
        SongInfo songInfo = songInfoRepository.findById(songId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "음원을 찾을 수 없습니다."));

        // 디버깅: CrawlingPeriod 데이터 확인
        long periodCount = crawlingPeriodRepository.countBySongId(songId);

        if (periodCount > 0) {
            List<CrawlingPeriod> allPeriods = crawlingPeriodRepository.findAllBySongId(songId);
        }

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
            boolean isStartDate = isStartDate(songId, currentDate);
            log.debug("날짜 {}는 시작일인가? {}", currentDate, isStartDate);

            if (isStartDate) {
                videoInfos = getVideoInfosForDate(songId, currentDate);
                log.debug("날짜 {}의 영상 정보 개수: {}", currentDate, videoInfos.size());
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
                    if (currentData.getViews() == -999 || previousData.getViews() == -999) {
                        viewsIncrease = -1; // 오류 데이터가 섞여있어 제공되지 않음
                    } else if (currentData.getViews() == -1 || previousData.getViews() == -1) {
                        viewsIncrease = -1; // 기본적으로 데이터 제공되지 않음
                    } else {
                        viewsIncrease = currentData.getViews() - previousData.getViews();
                    }

                    // 청취자수 증가량 계산
                    if (currentData.getListeners() == -999 || previousData.getListeners() == -999) {
                        listenersIncrease = -1; // 오류 데이터가 섞여있어 제공되지 않음
                    } else if (currentData.getListeners() == -1 || previousData.getListeners() == -1) {
                        listenersIncrease = -1; // 기본적으로 데이터 제공되지 않음
                    } else {
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

    /**
     * 특정 날짜가 크롤링 시작일인지 확인합니다.
     */
    private boolean isStartDate(String songId, LocalDate date) {
        return !crawlingPeriodRepository.findBySongIdAndStartDate(songId, date).isEmpty();
    }

    /**
     * 특정 날짜의 영상 정보를 조회합니다.
     */
    private List<CrawlingDataResponseDto.VideoInfo> getVideoInfosForDate(String songId, LocalDate date) {
        List<CrawlingPeriod> periods = crawlingPeriodRepository.findBySongIdAndStartDate(songId, date);

        return periods.stream()
                .map(period -> CrawlingDataResponseDto.VideoInfo.builder()
                        .channel(period.getChannel())
                        .youtubeTitle(period.getYoutubeTitle())
                        .youtubeUrl(period.getYoutubeUrl())
                        .songOrder(period.getSongOrder())
                        .uploadAt(period.getUploadAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 음원 ID로 음원 정보를 조회합니다.
     * 
     * @param songId 음원 ID
     * @return SongInfo
     */
    public SongInfo getSongInfoById(String songId) {
        return songInfoRepository.findById(songId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "음원을 찾을 수 없습니다."));
    }

    /**
     * 크롤링 실패한 곡들을 조회합니다.
     * crawling_failure 테이블에서 조회합니다.
     */
    @Transactional(readOnly = true)
    public Page<CrawlingFailureDto> getCrawlingFailures(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // 크롤링 실패 테이블에서 조회
        Page<com.rhoonart.unearth.crawling.entity.CrawlingFailure> failureResults = crawlingFailureRepository
                .findAllWithSongInfo(pageable);

        return failureResults.map(CrawlingFailureDto::from);
    }

    /**
     * 크롤링 데이터를 CSV 형태로 생성합니다.
     * 
     * @param songId    음원 ID
     * @param startDate 시작일
     * @param endDate   종료일
     * @param platform  플랫폼 (null이면 전체)
     * @return CSV 문자열
     */
    @Transactional(readOnly = true)
    public String generateCrawlingDataCsv(String songId, LocalDate startDate, LocalDate endDate,
            PlatformType platform) {
        // 음원 정보 조회
        SongInfo songInfo = songInfoRepository.findById(songId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "음원을 찾을 수 없습니다."));

        // 전체 데이터 조회 (페이징 없이)
        List<CrawlingData> allData = crawlingDataRepository.findBySongIdAndDateRange(songId, platform, startDate,
                endDate);

        // 날짜별로 그룹화
        Map<LocalDate, List<CrawlingData>> dataByDate = allData.stream()
                .collect(Collectors.groupingBy(data -> data.getCreatedAt().toLocalDate()));

        // CSV 헤더 (단일 컬럼 방식)
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("날짜,아티스트명,노래제목,플랫폼,조회수,조회수증가,청취자수,청취자수증가,영상정보(채널명/제목/url/순서)\n");

        // 날짜 정렬 (오래된 날짜부터)
        List<LocalDate> sortedDates = dataByDate.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        // 이전 날짜의 데이터를 저장할 맵 (플랫폼별)
        Map<PlatformType, CrawlingData> previousDayData = new HashMap<>();

        for (LocalDate currentDate : sortedDates) {
            List<CrawlingData> currentDataList = dataByDate.get(currentDate);

            // 영상 정보 조회 (시작일인 경우에만)
            String videoInfo = "";
            boolean isStartDate = isStartDate(songId, currentDate);
            if (isStartDate) {
                List<CrawlingDataResponseDto.VideoInfo> videoInfos = getVideoInfosForDate(songId, currentDate);
                if (!videoInfos.isEmpty()) {
                    videoInfo = videoInfos.stream()
                            .map(v -> String.format("%s / %s / %s / %d",
                                    v.getChannel(),
                                    v.getYoutubeTitle(),
                                    v.getYoutubeUrl(),
                                    v.getSongOrder()))
                            .collect(Collectors.joining(" | ")); // 여러 영상은 | 로 구분
                }
            }

            // 플랫폼별로 정렬
            currentDataList.sort((a, b) -> a.getPlatform().compareTo(b.getPlatform()));

            for (CrawlingData currentData : currentDataList) {
                // 조회수 증가량 계산
                long viewsIncrease = -1;
                long listenersIncrease = -1;

                CrawlingData previousData = previousDayData.get(currentData.getPlatform());
                if (previousData != null) {
                    // 조회수 증가량 계산
                    if (currentData.getViews() == -999 || previousData.getViews() == -999) {
                        viewsIncrease = -1; // 오류 데이터가 있어 계산 불가
                    } else if (currentData.getViews() == -1 || previousData.getViews() == -1) {
                        viewsIncrease = -1; // 데이터 없음
                    } else {
                        viewsIncrease = currentData.getViews() - previousData.getViews();
                    }

                    // 청취자수 증가량 계산
                    if (currentData.getListeners() == -999 || previousData.getListeners() == -999) {
                        listenersIncrease = -1; // 오류 데이터가 있어 계산 불가
                    } else if (currentData.getListeners() == -1 || previousData.getListeners() == -1) {
                        listenersIncrease = -1; // 데이터 없음
                    } else {
                        listenersIncrease = currentData.getListeners() - previousData.getListeners();
                    }
                }

                // CSV 행 생성
                csvBuilder.append(String.format("%s,%s,%s,%s,%d,%d,%d,%d,\"%s\"\n",
                        currentDate.toString(),
                        escapeCsvField(songInfo.getArtistKo()),
                        escapeCsvField(songInfo.getTitleKo()),
                        currentData.getPlatform().name(),
                        currentData.getViews(),
                        viewsIncrease,
                        currentData.getListeners(),
                        listenersIncrease,
                        escapeCsvField(videoInfo)));

                // 다음 날 계산을 위해 현재 데이터 저장
                previousDayData.put(currentData.getPlatform(), currentData);
            }
        }

        return csvBuilder.toString();
    }

    /**
     * CSV 다운로드를 위한 데이터 생성 및 파일명 생성
     */
    @Transactional(readOnly = true)
    public CrawlingCsvDownloadDto generateCrawlingDataCsvForDownload(String songId, String startDateStr,
            String endDateStr) {
        // 날짜 파싱
        LocalDate startDate = parseDateParameter(startDateStr);
        LocalDate endDate = parseDateParameter(endDateStr);

        // CSV 데이터 생성 (플랫폼은 null로 전달하여 전체 플랫폼 포함)
        String csvData = generateCrawlingDataCsv(songId, startDate, endDate, null);

        // 음원 정보 조회 (파일명 생성용)
        SongInfo songInfo = songInfoRepository.findById(songId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "음원을 찾을 수 없습니다."));

        // 파일명 생성 (아티스트명_타이틀.csv)
        String filename = String.format("%s_%s.csv",
                cleanFilename(songInfo.getArtistKo()),
                cleanFilename(songInfo.getTitleKo()));

        return new CrawlingCsvDownloadDto(csvData, filename);
    }

    /**
     * 날짜 파라미터 파싱
     */
    private LocalDate parseDateParameter(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            // 잘못된 날짜 형식이면 null로 처리
            return null;
        }
    }

    /**
     * CSV 필드 이스케이프 처리
     */
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        // 쉼표, 따옴표, 줄바꿈이 있으면 따옴표로 감싸고 내부 따옴표는 이스케이프
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return field.replace("\"", "\"\"");
        }
        return field;
    }

    /**
     * 파일명에 사용할 수 없는 문자를 제거
     */
    private String cleanFilename(String filename) {
        if (filename == null) {
            return "";
        }
        // 파일명에 사용할 수 없는 문자들을 언더스코어로 대체하고, 공백은 제거
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "") // 공백을 제거
                .trim();
    }
}