package com.rhoonart.unearth.song.service;

import com.rhoonart.unearth.common.util.EncodingDetector;
import com.rhoonart.unearth.song.dto.CsvSongDataDto;
import com.rhoonart.unearth.song.dto.SongBulkRegisterResponseDto;
import com.rhoonart.unearth.song.dto.SongBulkRegisterResultDto;
import com.rhoonart.unearth.song.dto.SongRegistrationFailureDto;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.song.repository.SongInfoRepository;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import com.rhoonart.unearth.right_holder.repository.RightHolderRepository;
import com.rhoonart.unearth.common.exception.BaseException;
import com.rhoonart.unearth.common.ResponseCode;
import java.io.ByteArrayInputStream;
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
    public SongBulkRegisterResponseDto bulkRegisterFromCsv(MultipartFile file) {

        try {
            // 1. CSV 파일 읽기
            List<CsvSongDataDto> csvDataList = readCsvFile(file);

            // 2. 권리자 검증 및 매핑
            Map<String, RightHolder> rightHolderMap = validateAndMapRightHolders(csvDataList);

            // 3. 중복 검사 및 등록 대상 분리
            SongBulkRegisterResultDto result = processBulkRegistration(csvDataList, rightHolderMap);

            // 결과를 DTO로 변환
            return SongBulkRegisterResponseDto.from(result);

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
    private List<CsvSongDataDto> readCsvFile(MultipartFile file) throws IOException {
        List<CsvSongDataDto> dataList = new ArrayList<>();

        // 1. 파일을 byte[]로 먼저 읽음
        byte[] fileBytes = file.getBytes();

        // 2. 인코딩 감지
        String detectedEncoding = EncodingDetector.detectEncoding(fileBytes);
        log.info("📑 감지된 인코딩: {}", detectedEncoding);
        // 3. 인코딩에 맞춰 읽기
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(fileBytes), detectedEncoding))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                if (line.trim().isEmpty())
                    continue;

                CsvSongDataDto csvData = parseCsvLine(line);
                if (csvData != null) {
                    dataList.add(csvData);
                }
            }
        }

        return dataList;
    }

    /**
     * CSV 라인을 파싱하여 CsvSongDataDto 객체로 변환
     */
    private CsvSongDataDto parseCsvLine(String line) {
        try {
            // CSV 파싱 (쉼표로 구분, 따옴표 처리)
            String[] columns = parseCsvColumns(line);

            if (columns.length < 11) {
                log.warn("⚠️ CSV 컬럼 수 부족: {}", line);
                return null;
            }

            return CsvSongDataDto.builder()
                    .artistKo(cleanString(columns[1])) // 아티스트명 (국문)
                    .artistEn(cleanString(columns[2])) // 아티스트명 (영문)
                    .albumKo(cleanString(columns[3])) // 앨범명 (국문)
                    .albumEn(cleanString(columns[4])) // 앨범명 (영문)
                    .titleKo(cleanString(columns[5])) // 트랙명 (국문)
                    .titleEn(cleanString(columns[6])) // 트랙명 (영문)
                    .youtubeUrl(cleanString(columns[7])) // 음원 링크(유튜브 URL)
                    .melonSongId("") // Melon Id는 빈 값으로 설정 (자동 검색으로 찾을 예정)
                    .rightHolderName(cleanString(columns[8])) // 권리자 (한 칸 당겨짐)
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
    private Map<String, RightHolder> validateAndMapRightHolders(List<CsvSongDataDto> csvDataList) {
        // 고유한 권리자명 추출
        Set<String> rightHolderNames = csvDataList.stream()
                .map(CsvSongDataDto::getRightHolderName)
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
    private SongBulkRegisterResultDto processBulkRegistration(List<CsvSongDataDto> csvDataList,
            Map<String, RightHolder> rightHolderMap) {
        SongBulkRegisterResultDto result = new SongBulkRegisterResultDto();

        for (CsvSongDataDto csvData : csvDataList) {
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
    private boolean isValidCsvData(CsvSongDataDto csvData) {
        return csvData.getArtistKo() != null && !csvData.getArtistKo().trim().isEmpty() &&
                csvData.getTitleKo() != null && !csvData.getTitleKo().trim().isEmpty() &&
                csvData.getRightHolderName() != null && !csvData.getRightHolderName().trim().isEmpty();
    }

    /**
     * 중복 곡 검사
     */
    private boolean isDuplicateSong(CsvSongDataDto csvData) {
        // artist_ko + title_ko 중복 검사
        if (songInfoRepository.existsByArtistKoAndTitleKo(csvData.getArtistKo(), csvData.getTitleKo())) {
            return true;
        }

        // CSV 대량등록에서는 melon_song_id가 빈 값이므로 중복 검사 제외
        // 나중에 단일 곡 크롤링 시 자동으로 찾아서 저장됨

        return false;
    }

    /**
     * CSV 데이터로부터 SongInfo 엔티티 생성
     */
    private SongInfo createSongFromCsvData(CsvSongDataDto csvData, RightHolder rightHolder) {
        return SongInfo.builder()
                .artistKo(csvData.getArtistKo())
                .artistEn(csvData.getArtistEn() != null ? csvData.getArtistEn() : "")
                .albumKo(csvData.getAlbumKo() != null ? csvData.getAlbumKo() : "")
                .albumEn(csvData.getAlbumEn() != null ? csvData.getAlbumEn() : "")
                .titleKo(csvData.getTitleKo())
                .titleEn(csvData.getTitleEn() != null ? csvData.getTitleEn() : "")
                .youtubeUrl(csvData.getYoutubeUrl() != null ? csvData.getYoutubeUrl() : "")
                .melonSongId("") // CSV 대량등록에서는 빈 값으로 설정 (단일 곡 크롤링 시 자동 검색)
                .rightHolder(rightHolder)
                .build();
    }
}
