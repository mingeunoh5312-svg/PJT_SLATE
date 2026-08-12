package com.slate.matching;

import java.net.URI;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slate.common.SlateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private static final int MAX_ERROR_BODY_LENGTH = 1000;

    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiClient(OpenAiProperties properties, ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
    }

    public String model() {
        return properties.model();
    }

    public JsonNode postJson(String path, Object payload) {
        String apiKey = properties.requireApiKey();
        try {
            String body = restClient.post()
                    .uri(endpoint(path))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(Objects.toString(body, "{}"));
        } catch (SlateException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.warn(
                    "OpenAI API responded with an error. status={}, body={}",
                    ex.getStatusCode().value(),
                    truncate(ex.getResponseBodyAsString(), MAX_ERROR_BODY_LENGTH)
            );
            throw new SlateException(HttpStatus.BAD_GATEWAY, "OpenAI API 호출에 실패했습니다.");
        } catch (Exception ex) {
            log.warn("OpenAI API call failed before receiving a response. reason={}", ex.toString());
            throw new SlateException(HttpStatus.BAD_GATEWAY, "OpenAI API 호출에 실패했습니다.");
        }
    }

    private URI endpoint(String path) {
        String cleanPath = StringUtils.hasText(path) ? path.trim() : "";
        if (!cleanPath.startsWith("/")) {
            cleanPath = "/" + cleanPath;
        }
        return UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path(cleanPath)
                .build()
                .toUri();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
