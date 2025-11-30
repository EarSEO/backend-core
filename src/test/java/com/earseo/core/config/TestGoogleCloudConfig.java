package com.earseo.core.config;

import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestGoogleCloudConfig {

    @Bean
    @Primary
    public TextToSpeechClient textToSpeechClientMock() {
        return Mockito.mock(TextToSpeechClient.class);
    }
}