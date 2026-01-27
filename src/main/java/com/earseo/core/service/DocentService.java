package com.earseo.core.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.earseo.core.dto.etl.DocentItemDto;
import com.earseo.core.dto.etl.JoinItemDto;
import com.earseo.core.entity.Docent;
import com.earseo.core.entity.OdiiData;
import com.earseo.core.repository.DocentRepository;
import com.earseo.core.repository.OdiiDataRepository;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.cloud.texttospeech.v1.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.earseo.core.service.ai.Prompts.DOCENT_SCRIPT;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocentService {

    private final OdiiDataRepository odiiDataRepository;
    private final DocentRepository docentRepository;
    private final ChatClient chatClient;
    private final AmazonS3 amazonS3;
    private final TextToSpeechClient textToSpeechClient;
    private final ObjectMapper objectMapper;


    @Value("${api.key}")
    private String ApiKeys;
    @Value(("${cloud.aws.s3.bucket}"))
    private String bucketName;
    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudFrontDomain;

    List<String> voices = List.of("Kore", "Algieba", "Despina", "Enceladus");

    private String key;
    private int index;

    @Transactional
    public void initDocent(String lang) {

        int pageNo = 1;
        int numOfRows = 100;

        while (true) {
            JsonNode jsonNode = fetchOdiiApi(pageNo, numOfRows, lang);
            JsonNode body = jsonNode.path("response").path("body");

            if (body.path("numOfRows").asInt() == 0) {
                break;
            }

            List<OdiiData> odiiDataList = new ArrayList<>();

            if (body.path("items").path("item").isArray()) {
                for (JsonNode item : body.path("items").path("item")) {
                    odiiDataList.add(OdiiData.builder().title(item.path("title").asText()).script(item.path("script").asText()).build());
                }
            }

            odiiDataRepository.saveAll(odiiDataList);
            pageNo++;
        }
    }

    public void getDocent(String lang) {
        List<JoinItemDto> joinItems;
        if (lang.equals("en")) {
            joinItems = odiiDataRepository.joinWithMasterEn();
        } else {
            joinItems = odiiDataRepository.joinWithMasterKo();
        }

        int chunkSize = 10;
        for (int i = 0; i < joinItems.size(); i += chunkSize) {
            List<JoinItemDto> chunk = joinItems.subList(i, Math.min(i + chunkSize, joinItems.size()));

            try {
                processChunk(chunk, lang);
            } catch (Exception e) {
                return;
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processChunk(List<JoinItemDto> chunk, String lang) {
        List<Docent> docents = new ArrayList<>();

        for (JoinItemDto data : chunk) {
            log.info("current item id : " + data.id());
            String source = data.overview();

            if (data.script() != null) {
                source = data.script();
            }

            String prompt = String.format(DOCENT_SCRIPT.message, data.title(), source);
            try {
                String script = chatClient.prompt(prompt).call().content();
                String docentUrl = getDocentUrl(data.contentId(), script, lang);

                Docent docent = Docent.builder()
                        .contentId(data.contentId())
                        .script(script)
                        .docentUrl(docentUrl)
                        .build();

                docents.add(docent);
            } catch (Exception e) {
                log.error(e.getMessage());
                log.info("failed item id : " + data.id());
            }
        }

        docentRepository.saveAll(docents);
    }

    public String getDocentJson(){
        try{
            List<Docent> docents = docentRepository.findAll();

            List<DocentItemDto> exportData = docents.stream()
                    .map(docent -> new DocentItemDto(
                            docent.getContentId(),
                            docent.getScript(),
                            docent.getDocentUrl()
                    ))
                    .toList();

            File tempFile = File.createTempFile("docent_data_", ".json");

            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(tempFile, exportData);

            String s3Key = "core/docent/docent_data.json";

            objectMapper
                    .getFactory()
                    .setStreamWriteConstraints(
                            StreamWriteConstraints.builder()
                                    .maxNestingDepth(3000)
                                    .build()
                    );

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("application/json");
            metadata.setContentLength(tempFile.length());

            amazonS3.putObject(
                    new PutObjectRequest(bucketName, s3Key, tempFile)
            );

            tempFile.delete();

            return String.format("https://%s/%s", cloudFrontDomain, s3Key);
        }
        catch (Exception e){
            return null;
        }
    }

    public String getDocentUrl(String contentId, String script, String lang) {
        try {
            SynthesisInput input = SynthesisInput.newBuilder()
                    .setText(script)
                    .build();

            String langCode = switch (lang) {
                case "ko" -> "ko-KR";
                case "en" -> "en-US";
                default -> throw new IllegalStateException("Unexpected value: " + lang);
            };

            String voiceName = voices.get(Integer.parseInt(contentId) % voices.size());

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
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String fileName = contentId + "_" + lang + "_" + date + ".mp3";

            String s3Key = "core/docent/" + contentId + "/" + fileName;
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

            return String.format("https://%s/%s", cloudFrontDomain, s3Key);

        } catch (Exception e) {
            log.error("Failed to generate TTS for: {}", contentId, e);
            return null;
        }
    }

    public JsonNode fetchOdiiApi(int pageNo, int numOfRows, String lang) {
        this.key = ApiKeys.split(",")[0];
        this.index = 0;
        RestClient client = RestClient.create();

        URI uri = UriComponentsBuilder.fromHttpUrl("https://apis.data.go.kr/B551011/Odii/storyBasedList").queryParam("serviceKey", this.key).queryParam("MobileApp", "AppTest").queryParam("MobileOS", "ETC").queryParam("pageNo", pageNo).queryParam("numOfRows", numOfRows).queryParam("_type", "json").queryParam("langCode", lang).build(true).toUri();
        try {
            return client.get().uri(uri).retrieve().body(JsonNode.class);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            if (this.index + 1 < ApiKeys.split(",").length) {
                this.index++;
                this.key = ApiKeys.split(",")[this.index];
                return fetchOdiiApi(pageNo, numOfRows, lang);
            }
            return null;
        }
    }

}
