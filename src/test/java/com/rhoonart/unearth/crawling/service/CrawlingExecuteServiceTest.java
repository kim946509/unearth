package com.rhoonart.unearth.crawling.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CrawlingExecuteServiceTest {

    @InjectMocks
    private CrawlingExecuteService crawlingExecuteService;

    @Mock
    private ProcessBuilder processBuilder;

    @Mock
    private Process process;

}
