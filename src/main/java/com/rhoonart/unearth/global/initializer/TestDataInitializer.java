package com.rhoonart.unearth.global.initializer;

import com.rhoonart.unearth.crawling.entity.CrawlingData;
import com.rhoonart.unearth.crawling.entity.PlatformType;
import com.rhoonart.unearth.crawling.repository.CrawlingDataRepository;
import com.rhoonart.unearth.song.entity.SongInfo;
import com.rhoonart.unearth.song.repository.SongInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestDataInitializer implements ApplicationRunner {

    private final CrawlingDataRepository crawlingDataRepository;
    private final SongInfoRepository songInfoRepository;
    private final Environment environment;

    @Value("${test.data.enabled:false}")
    private boolean testDataEnabled;

    @Value("${test.data.song.id:a0cbe0c5-1808-4a1f-958e-01974a4716f8}")
    private String targetSongId;

    @Value("${test.data.start.date:2024-07-24}")
    private String startDateStr;

    @Value("${test.data.end.date:2025-07-28}")
    private String endDateStr;

    private static final PlatformType[] PLATFORMS = {
            PlatformType.YOUTUBE_MUSIC,
            PlatformType.MELON,
            PlatformType.GENIE,
            PlatformType.YOUTUBE
    };

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 테스트 데이터 생성이 비활성화되어 있으면 실행하지 않음
        if (!testDataEnabled) {
            log.info("ℹ️ 테스트 데이터 생성이 비활성화되어 있습니다. (test.data.enabled=false)");
            return;
        }

        log.info("🧪 테스트 데이터 초기화 시작");
        log.info("📅 기간: {} ~ {}", startDateStr, endDateStr);
        log.info("🎵 대상 곡 ID: {}", targetSongId);

        // 날짜 파싱
        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        // 대상 곡이 존재하는지 확인
        Optional<SongInfo> songInfo = songInfoRepository.findById(targetSongId);
        if (songInfo.isEmpty()) {
            log.warn("⚠️ 대상 곡을 찾을 수 없습니다: {}", targetSongId);
            return;
        }

        // 기존 데이터가 있는지 확인
        if (crawlingDataRepository.existsBySongId(targetSongId)) {
            log.info("ℹ️ 이미 크롤링 데이터가 존재합니다. 테스트 데이터 생성을 건너뜁니다.");
            return;
        }

        // 테스트 데이터 생성
        List<CrawlingData> testDataList = generateTestData(songInfo.get(), startDate, endDate);

        // 데이터베이스에 저장
        crawlingDataRepository.saveAll(testDataList);

        log.info("✅ 테스트 데이터 생성 완료: {}개 데이터 생성됨", testDataList.size());
    }

    private List<CrawlingData> generateTestData(SongInfo songInfo, LocalDate startDate, LocalDate endDate) {
        List<CrawlingData> dataList = new ArrayList<>();
        Random random = new Random();

        // 각 플랫폼별 초기값 설정
        long youtubeMusicViews = 100;
        long melonViews = 100;
        long genieViews = 100;
        long youtubeViews = 100;

        long melonListeners = 100;
        long genieListeners = 100;

        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            // 각 날짜의 오후 5시로 고정
            LocalDateTime createdAt = currentDate.atTime(17, 0, 0); // 오후 5시

            // YOUTUBE_MUSIC 데이터 (listeners = -1)
            long viewsIncrease = 1 + random.nextInt(1000); // 1 ~ 1000 증가
            youtubeMusicViews += viewsIncrease;

            CrawlingData youtubeMusicData = CrawlingData.builder()
                    .platform(PlatformType.YOUTUBE_MUSIC)
                    .song(songInfo)
                    .views(youtubeMusicViews)
                    .listeners(-1)
                    .build();
            youtubeMusicData.setCustomCreatedAt(createdAt);
            dataList.add(youtubeMusicData);

            // MELON 데이터
            viewsIncrease = 1 + random.nextInt(1000); // 1 ~ 1000 증가
            melonViews += viewsIncrease;

            long listenersIncrease = 1 + random.nextInt(1000); // 1 ~ 1000 증가
            melonListeners += listenersIncrease;

            CrawlingData melonData = CrawlingData.builder()
                    .platform(PlatformType.MELON)
                    .song(songInfo)
                    .views(melonViews)
                    .listeners(melonListeners)
                    .build();
            melonData.setCustomCreatedAt(createdAt);
            dataList.add(melonData);

            // GENIE 데이터
            viewsIncrease = 1 + random.nextInt(1000); // 1 ~ 1000 증가
            genieViews += viewsIncrease;

            listenersIncrease = 1 + random.nextInt(1000); // 1 ~ 1000 증가
            genieListeners += listenersIncrease;

            CrawlingData genieData = CrawlingData.builder()
                    .platform(PlatformType.GENIE)
                    .song(songInfo)
                    .views(genieViews)
                    .listeners(genieListeners)
                    .build();
            genieData.setCustomCreatedAt(createdAt);
            dataList.add(genieData);

            // YOUTUBE 데이터 (listeners = -1)
            viewsIncrease = 1 + random.nextInt(1000); // 1 ~ 1000 증가
            youtubeViews += viewsIncrease;

            CrawlingData youtubeData = CrawlingData.builder()
                    .platform(PlatformType.YOUTUBE)
                    .song(songInfo)
                    .views(youtubeViews)
                    .listeners(-1)
                    .build();
            youtubeData.setCustomCreatedAt(createdAt);
            dataList.add(youtubeData);

            currentDate = currentDate.plusDays(1);
        }

        return dataList;
    }
}