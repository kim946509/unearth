package com.rhoonart.unearth.common.util;

import java.nio.charset.StandardCharsets;
import lombok.experimental.UtilityClass;
import org.mozilla.universalchardet.UniversalDetector;

public final class EncodingDetector {

    private EncodingDetector() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 바이트 배열로부터 파일 인코딩을 감지
     * @param bytes MultipartFile.getBytes() 결과
     * @return 감지된 인코딩 (기본값은 UTF-8)
     */
    public static String detectEncoding(byte[] bytes) {
        UniversalDetector detector = new UniversalDetector(null);
        detector.handleData(bytes, 0, bytes.length);
        detector.dataEnd();
        String encoding = detector.getDetectedCharset();
        return encoding != null ? encoding : StandardCharsets.UTF_8.name();
    }
}