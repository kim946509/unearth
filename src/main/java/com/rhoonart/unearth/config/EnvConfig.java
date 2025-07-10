package com.rhoonart.unearth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:env.properties")
public class EnvConfig {
    // 환경변수 로드를 위한 설정 클래스
}