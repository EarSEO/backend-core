package com.earseo.core.service.internal;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.earseo.core.dto.internal.StoryDocentRequest;
import com.earseo.core.dto.internal.StoryDocentResponse;
import com.google.cloud.texttospeech.v1.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InternalService {

    private final AmazonS3 amazonS3;
    private final TextToSpeechClient textToSpeechClient;

    @Value(("${cloud.aws.s3.bucket}"))
    private String bucketName;
    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudFrontDomain;

    List<String> voices = List.of("Kore", "Algieba", "Despina", "Enceladus");

    public List<StoryDocentResponse> createStorySpotDocent(List<StoryDocentRequest> requests) {
        List<StoryDocentResponse> responses = new ArrayList<>();

        for(StoryDocentRequest request : requests) {
            try {
                SynthesisInput input = SynthesisInput.newBuilder()
                        .setText(request.summary())
                        .build();

                String langCode = switch (request.locale()) {
                    case "KO" -> "ko-KR";
                    case "EN" -> "en-US";
                    default -> throw new IllegalStateException("Unexpected value: " + request.locale());
                };

                int index = (int) (Math.random() * 4);

                String voiceName = voices.get(index);

                VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                        .setLanguageCode(langCode)
                        .setName(langCode + "-Chirp3-HD-"+ voiceName)
                        .build();

                AudioConfig audioConfig = AudioConfig.newBuilder()
                        .setAudioEncoding(AudioEncoding.MP3)
                        .setSpeakingRate(1.0)
                        .build();

                SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

                byte[] audioContent = response.getAudioContent().toByteArray();
                String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddhhmm"));

                String s3Key = "story/docent/" + request.summaryId() + "/" + date + ".mp3";
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(audioContent.length);
                metadata.setContentType("audio/mpeg");

                try (ByteArrayInputStream inputStream = new ByteArrayInputStream(audioContent)) {
                    amazonS3.putObject(
                            new PutObjectRequest(
                                    bucketName,
                                    s3Key,
                                    inputStream,
                                    metadata
                            )
                    );
                }

                String docentUrl = String.format("https://%s/%s", cloudFrontDomain, s3Key);

                responses.add(new StoryDocentResponse(request.summaryId(), request.summary(), docentUrl));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return responses;
    }
}
