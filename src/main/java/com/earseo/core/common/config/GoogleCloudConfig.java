package com.earseo.core.common.config;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class GoogleCloudConfig {

    @Bean
    public TextToSpeechClient textToSpeechClient() throws IOException {
        ClassPathResource resource = new ClassPathResource("gcp-service-account-key.json");

        InputStream credentialsStream = resource.getInputStream();

        Credentials credentials = GoogleCredentials.fromStream(credentialsStream);

        TextToSpeechSettings settings = TextToSpeechSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();

        return TextToSpeechClient.create(settings);
    }
}
