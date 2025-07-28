package com.rhoonart.unearth.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LogFileUtil {

    private final String logDirPath;

    public LogFileUtil(@Value("${log.crawling-dir:streaming_crawling/logs}") String logDirPath) {
        this.logDirPath = logDirPath;
    }

    /**
     * 지정한 보존일(daysToKeep)보다 오래된 로그 파일을 삭제합니다.
     * 
     * @param daysToKeep 최근 N일 이내 파일은 삭제하지 않음
     * @return 삭제된 파일 개수
     */
    public int deleteOldLogs(int daysToKeep) {
        File logDir = new File(logDirPath);
        if (!logDir.exists() || !logDir.isDirectory())
            return 0;

        Pattern pattern = Pattern.compile(".*_(\\d{8})_\\d{6}\\.log$");
        LocalDate today = LocalDate.now();
        int deleted = 0;

        for (File file : logDir.listFiles()) {
            Matcher matcher = pattern.matcher(file.getName());
            if (matcher.matches()) {
                String dateStr = matcher.group(1);
                LocalDate fileDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
                if (fileDate.isBefore(today.minusDays(daysToKeep))&&file.delete()) {
                    deleted++;
                }
            }
        }
        return deleted;
    }
}