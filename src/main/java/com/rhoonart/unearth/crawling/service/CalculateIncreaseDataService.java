package com.rhoonart.unearth.crawling.service;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CalculateIncreaseDataService {

    public static long calculateIncrease(long currentValue, long previousValue) {
        if(currentValue>=0 && previousValue>=0)
            return (long)currentValue - (long)previousValue;

        // 데이터가 없거나 오류일 경우
        return -1;
    }
}
