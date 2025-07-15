package com.rhoonart.unearth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value="classpath:env.properties", ignoreResourceNotFound = true)
public class EnvConfig {
    // 환경변수 로드를 위한 설정 클래스
}