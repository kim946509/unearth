package com.rhoonart.unearth.crawling.entity;

import com.rhoonart.unearth.user.exception.BadRequestException;

public enum PlatformType {
    MELON,
    GENIE,
    YOUTUBE,
    YOUTUBE_MUSIC;

    public static PlatformType fromString(String platform) {
        try {
            return PlatformType.valueOf(platform.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("지원하지 않는 플랫폼 타입입니다: " + platform);
        }
    }
}