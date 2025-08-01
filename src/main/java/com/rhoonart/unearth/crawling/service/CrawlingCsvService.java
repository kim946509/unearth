package com.rhoonart.unearth.crawling.service;

import com.rhoonart.unearth.common.ResponseCode;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.common.util.DataAuthorityService;
import com.rhoonart.unearth.crawling.dto.CrawlingCsvDownloadDto;
import com.rhoonart.unearth.crawling.dto.VideoInfoDto;
import com.rhoonart.unearth.crawling.entity.CrawlingData;
import com.rhoonart.unearth.crawling.entity.PlatformType;
import com.rhoonart.unearth.crawling.repository.CrawlingDataRepository;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.song.repository.SongInfoRepository;
import com.rhoonart.unearth.user.dto.UserDto;
import com.rhoonart.unearth.user.exception.ForbiddenException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CrawlingCsvService {

    private final SongInfoRepository songInfoRepository;
    private final CrawlingDataRepository crawlingDataRepository;
    private final CrawlingPeriodService crawlingPeriodService;
    private final DataAuthorityService dataAuthorityService;

    /**
     * CSV 다운로드를 위한 메인 메서드입니다.
     */
    @Transactional(readOnly = true)
    public CrawlingCsvDownloadDto generateCrawlingDataCsvForDownload(UserDto userDto, String songId,
            String startDateStr,
            String endDateStr) {

        // 권한 체크
        if (!dataAuthorityService.isAccessSongData(userDto, songId)) {
            throw new ForbiddenException();
        }

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

        // 한글 파일명 인코딩 (RFC 5987 형식)
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");

        return new CrawlingCsvDownloadDto(csvData, encodedFilename);
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
    private String generateCrawlingDataCsv(String songId, LocalDate startDate, LocalDate endDate,
            PlatformType platform) {
        // 음원 정보 조회
        SongInfo songInfo = songInfoRepository.findById(songId)
                .orElseThrow(() -> new BaseException(ResponseCode.NOT_FOUND, "음원을 찾을 수 없습니다."));

        // 전체 데이터 조회
        List<CrawlingData> allData = crawlingDataRepository.findBySongIdAndDateRange(songId, platform, startDate,
                endDate);

        // 날짜별로 그룹화
        Map<LocalDate, List<CrawlingData>> dataByDate = allData.stream()
                .collect(Collectors.groupingBy(data -> data.getCreatedAt().toLocalDate()));

        // CSV 헤더 (단일 컬럼 방식) - UTF-8 인코딩 명시
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("날짜,아티스트명,노래제목,플랫폼,조회수,조회수증가,청취자수,청취자수증가,영상정보(채널명/제목/url/순서/조회수)\n");

        // 날짜 정렬 (최신 날짜부터)
        List<LocalDate> sortedDates = dataByDate.keySet().stream()
                .sorted(Comparator.reverseOrder())
                .toList();

        // 영상 정보를 일괄 조회 (N+1 문제 해결)
        Map<LocalDate, List<VideoInfoDto>> allVideoInfos = crawlingPeriodService
                .getVideoInfosForDateRange(songId, startDate, endDate);

        // 이전 날짜 데이터를 일괄 조회 (N+1 문제 해결)
        Map<String, CrawlingData> previousDataMap = new HashMap<>();
        if (!sortedDates.isEmpty()) {
            // 모든 날짜의 이전 날짜 중 가장 이른 날짜부터 가장 늦은 날짜까지 조회
            LocalDate earliestPreviousDate = sortedDates.get(sortedDates.size() - 1).minusDays(1);
            LocalDate latestPreviousDate = sortedDates.get(0).minusDays(1);

            List<CrawlingData> allPreviousData = crawlingDataRepository
                    .findBySongIdAndDateRange(songId, null, earliestPreviousDate, latestPreviousDate);

            // 플랫폼별, 날짜별로 맵핑
            previousDataMap = allPreviousData.stream()
                    .collect(Collectors.toMap(
                            data -> data.getPlatform().name() + "_" + data.getCreatedAt().toLocalDate().toString(),
                            data -> data,
                            (existing, replacement) -> existing // 중복 시 기존 값 유지
                    ));
        }

        for (LocalDate currentDate : sortedDates) {
            List<CrawlingData> currentDataList = dataByDate.get(currentDate);

            // 영상 정보 조회 (일괄 조회된 데이터에서 가져오기)
            String videoInfo = "";
            List<VideoInfoDto> videoInfos = allVideoInfos.getOrDefault(currentDate, new ArrayList<>());
            if (!videoInfos.isEmpty()) {
                videoInfo = videoInfos.stream()
                        .map(v -> String.format("%s / %s / %s / %d / %s",
                                v.getChannel() != null ? v.getChannel() : "-",
                                v.getYoutubeTitle() != null ? v.getYoutubeTitle() : "-",
                                v.getYoutubeUrl() != null ? v.getYoutubeUrl() : "-",
                                v.getSongOrder(),
                                formatVideoViewCount(v.getViewCount())))
                        .collect(Collectors.joining(" | ")); // 여러 영상은 | 로 구분
            }

            // 플랫폼별로 정렬
            currentDataList.sort((a, b) -> a.getPlatform().compareTo(b.getPlatform()));

            for (CrawlingData currentData : currentDataList) {
                // 이전날 데이터를 일괄 조회된 데이터에서 가져오기
                LocalDate previousDate = currentDate.minusDays(1);
                String previousDataKey = currentData.getPlatform().name() + "_" + previousDate.toString();
                CrawlingData previousData = previousDataMap.get(previousDataKey);

                long viewsIncrease = -1; // 기본값: 이전 데이터 없음
                long listenersIncrease = -1; // 기본값: 이전 데이터 없음

                if (previousData != null) {
                    // 조회수 증가량 계산
                    viewsIncrease = CalculateIncreaseDataService.calculateIncrease(
                            currentData.getViews(),
                            previousData.getViews());

                    // 청취자수 증가량 계산
                    listenersIncrease = CalculateIncreaseDataService.calculateIncrease(
                            currentData.getListeners(),
                            previousData.getListeners());
                }

                // CSV 행 생성
                csvBuilder.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,\"%s\"\n",
                        currentDate.toString(),
                        escapeCsvField(songInfo.getArtistKo()),
                        escapeCsvField(songInfo.getTitleKo()),
                        currentData.getPlatform().name(),
                        formatNumericValue(currentData.getViews()),
                        formatNumericValue(viewsIncrease),
                        formatNumericValue(currentData.getListeners()),
                        formatNumericValue(listenersIncrease),
                        escapeCsvField(videoInfo)));
            }
        }

        return csvBuilder.toString();
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

    /**
     * 숫자 값을 CSV 형식으로 변환
     * -1: "-", -999: "Fail", 그 외: 숫자 그대로
     */
    private String formatNumericValue(long value) {
        if (value == -1) {
            return "-"; // 데이터 없음 (HTML과 일관성)
        } else if (value == -999) {
            return "Fail"; // 오류
        } else {
            return String.valueOf(value); // 숫자 그대로
        }
    }

    /**
     * 영상 조회수를 CSV 형식으로 변환
     * null: "-", -999: "Fail", 그 외: 숫자 그대로
     */
    private String formatVideoViewCount(Integer viewCount) {
        if (viewCount == null) {
            return "-"; // 크롤링되지 않음
        } else if (viewCount == -999) {
            return "Fail"; // 크롤링 실패
        } else {
            return String.valueOf(viewCount); // 숫자 그대로
        }
    }
}
