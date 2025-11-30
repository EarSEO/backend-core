package com.earseo.core.common.config;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Configuration
@Profile("!test")
public class GoogleCloudConfig {

    @Value("${gcp.credentials.base64:}")
    private String credentialsBase64;

    @Bean
    public TextToSpeechClient textToSpeechClient() throws IOException {
        byte[] decodedBytes = Base64.getDecoder().decode(credentialsBase64);
        InputStream credentialsStream = new ByteArrayInputStream(decodedBytes);
        Credentials credentials = GoogleCredentials.fromStream(credentialsStream);

        TextToSpeechSettings settings = TextToSpeechSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();

        return TextToSpeechClient.create(settings);
    }
}
