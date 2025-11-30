package com.earseo.core;

import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class BackendCoreApplicationTests {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public TextToSpeechClient textToSpeechClient() {
            return Mockito.mock(TextToSpeechClient.class);
        }
    }

    @Test
    void contextLoads() {
    }

}
