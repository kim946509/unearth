package com.rhoonart.unearth.song.service;

import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.song.repository.SongInfoRepository;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import com.rhoonart.unearth.right_holder.repository.RightHolderRepository;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.common.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongBulkRegisterService {

    private final SongInfoRepository songInfoRepository;
    private final RightHolderRepository rightHolderRepository;

    /**
     * CSV 파일을 읽어서 곡을 일괄 등록
     * 
     * @param file 업로드된 CSV 파일
     * @return 일괄 등록 결과
     */
    @Transactional
    public BulkRegisterResult bulkRegisterFromCsv(MultipartFile file) {
        log.info("🎵 CSV 일괄 등록 시작: 파일명={}, 크기={}bytes", file.getOriginalFilename(), file.getSize());

        try {
            // 1. CSV 파일 읽기
            List<CsvSongData> csvDataList = readCsvFile(file);
            log.info("📊 CSV 파일 읽기 완료: {}개 행", csvDataList.size());

            // 2. 권리자 검증 및 매핑
            Map<String, RightHolder> rightHolderMap = validateAndMapRightHolders(csvDataList);

            // 3. 중복 검사 및 등록 대상 분리
            BulkRegisterResult result = processBulkRegistration(csvDataList, rightHolderMap);

            log.info("✅ CSV 일괄 등록 완료: 성공={}개, 중복={}개, 실패={}개",
                    result.getSuccessCount(), result.getDuplicateCount(), result.getFailureCount());

            return result;

        } catch (IOException e) {
            log.error("❌ CSV 파일 읽기 중 오류 발생", e);
            throw new BaseException(ResponseCode.INVALID_INPUT, "CSV 파일을 읽을 수 없습니다: " + e.getMessage());
        } catch (BaseException e) {
            // BaseException은 그대로 재던지기
            throw e;
        } catch (Exception e) {
            log.error("❌ 일괄 등록 중 오류 발생", e);
            throw new BaseException(ResponseCode.SERVER_ERROR, "일괄 등록 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * CSV 파일을 읽어서 데이터 리스트로 변환
     */
    private List<CsvSongData> readCsvFile(MultipartFile file) throws IOException {
        List<CsvSongData> dataList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                // 헤더 라인 스킵
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // 빈 라인 스킵
                if (line.trim().isEmpty()) {
                    continue;
                }

                CsvSongData csvData = parseCsvLine(line);
                if (csvData != null) {
                    dataList.add(csvData);
                }
            }
        }

        return dataList;
    }

    /**
     * CSV 라인을 파싱하여 CsvSongData 객체로 변환
     */
    private CsvSongData parseCsvLine(String line) {
        try {
            // CSV 파싱 (쉼표로 구분, 따옴표 처리)
            String[] columns = parseCsvColumns(line);

            if (columns.length < 12) {
                log.warn("⚠️ CSV 컬럼 수 부족: {}", line);
                return null;
            }

            return CsvSongData.builder()
                    .artistKo(cleanString(columns[1])) // 아티스트명 (국문)
                    .artistEn(cleanString(columns[2])) // 아티스트명 (영문)
                    .albumKo(cleanString(columns[3])) // 앨범명 (국문)
                    .albumEn(cleanString(columns[4])) // 앨범명 (영문)
                    .titleKo(cleanString(columns[5])) // 트랙명 (국문)
                    .titleEn(cleanString(columns[6])) // 트랙명 (영문)
                    .youtubeUrl(cleanString(columns[7])) // 음원 링크(유튜브 URL)
                    .melonSongId(cleanString(columns[10])) // Melon Id
                    .rightHolderName(cleanString(columns[11])) // 권리자
                    .build();

        } catch (Exception e) {
            log.warn("⚠️ CSV 라인 파싱 실패: {}", line, e);
            return null;
        }
    }

    /**
     * CSV 컬럼을 파싱 (쉼표로 구분, 따옴표 처리)
     */
    private String[] parseCsvColumns(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder currentColumn = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                columns.add(currentColumn.toString());
                currentColumn = new StringBuilder();
            } else {
                currentColumn.append(c);
            }
        }

        // 마지막 컬럼 추가
        columns.add(currentColumn.toString());

        return columns.toArray(new String[0]);
    }

    /**
     * 문자열 정리 (앞뒤 공백 제거, 따옴표 제거)
     */
    private String cleanString(String str) {
        if (str == null)
            return "";
        return str.trim().replaceAll("^\"|\"$", "");
    }

    /**
     * 권리자 검증 및 매핑
     */
    private Map<String, RightHolder> validateAndMapRightHolders(List<CsvSongData> csvDataList) {
        // 고유한 권리자명 추출
        Set<String> rightHolderNames = csvDataList.stream()
                .map(CsvSongData::getRightHolderName)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());

        // 권리자 조회
        Map<String, RightHolder> rightHolderMap = new HashMap<>();
        List<String> notFoundRightHolders = new ArrayList<>();

        for (String rightHolderName : rightHolderNames) {
            Optional<RightHolder> rightHolder = rightHolderRepository.findByHolderName(rightHolderName);
            if (rightHolder.isPresent()) {
                rightHolderMap.put(rightHolderName, rightHolder.get());
            } else {
                notFoundRightHolders.add(rightHolderName);
            }
        }

        // 존재하지 않는 권리자가 있으면 예외 발생
        if (!notFoundRightHolders.isEmpty()) {
            String errorMessage;
            if (notFoundRightHolders.size() == 1) {
                errorMessage = String.format("다음 권리자를 찾을 수 없습니다: %s", notFoundRightHolders.get(0));
            } else {
                errorMessage = String.format("다음 권리자들을 찾을 수 없습니다: %s",
                        String.join(", ", notFoundRightHolders));
            }
            throw new BaseException(ResponseCode.NOT_FOUND, errorMessage);
        }

        return rightHolderMap;
    }

    /**
     * 중복 검사 및 일괄 등록 처리
     */
    private BulkRegisterResult processBulkRegistration(List<CsvSongData> csvDataList,
            Map<String, RightHolder> rightHolderMap) {
        BulkRegisterResult result = new BulkRegisterResult();

        for (CsvSongData csvData : csvDataList) {
            try {
                // 필수 필드 검증
                if (!isValidCsvData(csvData)) {
                    result.addFailure(csvData, "필수 필드가 누락되었습니다");
                    continue;
                }

                // 중복 검사
                if (isDuplicateSong(csvData)) {
                    result.addDuplicate(csvData);
                    continue;
                }

                // 곡 등록
                SongInfo song = createSongFromCsvData(csvData, rightHolderMap.get(csvData.getRightHolderName()));
                songInfoRepository.save(song);
                result.addSuccess(csvData);

            } catch (Exception e) {
                log.error("❌ 곡 등록 실패: {}", csvData, e);
                result.addFailure(csvData, e.getMessage());
            }
        }

        return result;
    }

    /**
     * CSV 데이터 유효성 검사
     */
    private boolean isValidCsvData(CsvSongData csvData) {
        return csvData.getArtistKo() != null && !csvData.getArtistKo().trim().isEmpty() &&
                csvData.getTitleKo() != null && !csvData.getTitleKo().trim().isEmpty() &&
                csvData.getRightHolderName() != null && !csvData.getRightHolderName().trim().isEmpty();
    }

    /**
     * 중복 곡 검사
     */
    private boolean isDuplicateSong(CsvSongData csvData) {
        // artist_ko + title_ko 중복 검사
        if (songInfoRepository.existsByArtistKoAndTitleKo(csvData.getArtistKo(), csvData.getTitleKo())) {
            return true;
        }

        // melon_song_id 중복 검사 (있는 경우에만)
        if (csvData.getMelonSongId() != null && !csvData.getMelonSongId().trim().isEmpty()) {
            if (songInfoRepository.existsByMelonSongId(csvData.getMelonSongId())) {
                return true;
            }
        }

        return false;
    }

    /**
     * CSV 데이터로부터 SongInfo 엔티티 생성
     */
    private SongInfo createSongFromCsvData(CsvSongData csvData, RightHolder rightHolder) {
        return SongInfo.builder()
                .artistKo(csvData.getArtistKo())
                .artistEn(csvData.getArtistEn() != null ? csvData.getArtistEn() : "")
                .albumKo(csvData.getAlbumKo() != null ? csvData.getAlbumKo() : "")
                .albumEn(csvData.getAlbumEn() != null ? csvData.getAlbumEn() : "")
                .titleKo(csvData.getTitleKo())
                .titleEn(csvData.getTitleEn() != null ? csvData.getTitleEn() : "")
                .youtubeUrl(csvData.getYoutubeUrl() != null ? csvData.getYoutubeUrl() : "")
                .melonSongId(csvData.getMelonSongId() != null ? csvData.getMelonSongId() : "")
                .rightHolder(rightHolder)
                .build();
    }

    /**
     * CSV 곡 데이터 클래스
     */
    public static class CsvSongData {
        private String artistKo;
        private String artistEn;
        private String albumKo;
        private String albumEn;
        private String titleKo;
        private String titleEn;
        private String youtubeUrl;
        private String melonSongId;
        private String rightHolderName;

        // Builder 패턴
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private CsvSongData csvData = new CsvSongData();

            public Builder artistKo(String artistKo) {
                csvData.artistKo = artistKo;
                return this;
            }

            public Builder artistEn(String artistEn) {
                csvData.artistEn = artistEn;
                return this;
            }

            public Builder albumKo(String albumKo) {
                csvData.albumKo = albumKo;
                return this;
            }

            public Builder albumEn(String albumEn) {
                csvData.albumEn = albumEn;
                return this;
            }

            public Builder titleKo(String titleKo) {
                csvData.titleKo = titleKo;
                return this;
            }

            public Builder titleEn(String titleEn) {
                csvData.titleEn = titleEn;
                return this;
            }

            public Builder youtubeUrl(String youtubeUrl) {
                csvData.youtubeUrl = youtubeUrl;
                return this;
            }

            public Builder melonSongId(String melonSongId) {
                csvData.melonSongId = melonSongId;
                return this;
            }

            public Builder rightHolderName(String rightHolderName) {
                csvData.rightHolderName = rightHolderName;
                return this;
            }

            public CsvSongData build() {
                return csvData;
            }
        }

        // Getters
        public String getArtistKo() {
            return artistKo;
        }

        public String getArtistEn() {
            return artistEn;
        }

        public String getAlbumKo() {
            return albumKo;
        }

        public String getAlbumEn() {
            return albumEn;
        }

        public String getTitleKo() {
            return titleKo;
        }

        public String getTitleEn() {
            return titleEn;
        }

        public String getYoutubeUrl() {
            return youtubeUrl;
        }

        public String getMelonSongId() {
            return melonSongId;
        }

        public String getRightHolderName() {
            return rightHolderName;
        }

        @Override
        public String toString() {
            return String.format("%s - %s", artistKo, titleKo);
        }
    }

    /**
     * 일괄 등록 결과 클래스
     */
    public static class BulkRegisterResult {
        private final List<CsvSongData> successList = new ArrayList<>();
        private final List<CsvSongData> duplicateList = new ArrayList<>();
        private final List<FailureData> failureList = new ArrayList<>();

        public void addSuccess(CsvSongData csvData) {
            successList.add(csvData);
        }

        public void addDuplicate(CsvSongData csvData) {
            duplicateList.add(csvData);
        }

        public void addFailure(CsvSongData csvData, String reason) {
            failureList.add(new FailureData(csvData, reason));
        }

        public int getSuccessCount() {
            return successList.size();
        }

        public int getDuplicateCount() {
            return duplicateList.size();
        }

        public int getFailureCount() {
            return failureList.size();
        }

        public int getTotalCount() {
            return getSuccessCount() + getDuplicateCount() + getFailureCount();
        }

        public List<CsvSongData> getSuccessList() {
            return new ArrayList<>(successList);
        }

        public List<CsvSongData> getDuplicateList() {
            return new ArrayList<>(duplicateList);
        }

        public List<FailureData> getFailureList() {
            return new ArrayList<>(failureList);
        }

        public static class FailureData {
            private final CsvSongData csvData;
            private final String reason;

            public FailureData(CsvSongData csvData, String reason) {
                this.csvData = csvData;
                this.reason = reason;
            }

            public CsvSongData getCsvData() {
                return csvData;
            }

            public String getReason() {
                return reason;
            }
        }
    }
}
