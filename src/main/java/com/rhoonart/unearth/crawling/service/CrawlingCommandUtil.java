package com.rhoonart.unearth.crawling.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CrawlingCommandUtil {

    /**
     * 운영체제를 감지합니다.
     */
    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    /**
     * Windows 환경에서 가상환경을 포함한 명령어를 생성합니다.
     */
    public static List<String> createWindowsCommand(String scriptPath, String... args) {
        List<String> command = new ArrayList<>();
        command.add("cmd");
        command.add("/c");
        command.add("cd streaming_crawling && env\\Scripts\\activate && python " + scriptPath);

        // 추가 인자들 추가
        command.addAll(Arrays.asList(args));

        return command;
    }

    /**
     * Ubuntu/Linux 환경에서 명령어를 생성합니다.
     */
    public static List<String> createLinuxCommand(String scriptPath, String... args) {
        List<String> command = new ArrayList<>();
        command.add("python3");
        command.add(scriptPath);

        // 추가 인자들 추가
        command.addAll(Arrays.asList(args));

        return command;
    }

}
