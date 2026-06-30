package com.example.blogproject.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class ModerationService {

    private static final String SIGHTENGINE_ENDPOINT = "https://api.sightengine.com/1.0/check.json";

    private static final String MODELS =
            "nudity-2.1,weapon,alcohol,recreational_drug,tobacco,violence,self-harm,gore-2.0,gambling";

    @Value("${sightengine.api-user}")
    private String apiUser;

    @Value("${sightengine.api-secret}")
    private String apiSecret;

    private final RestClient restClient = RestClient.create();

    public String checkImage(String imageUrl) {

        URI uri = UriComponentsBuilder
                .fromHttpUrl(SIGHTENGINE_ENDPOINT)
                .queryParam("models", MODELS)
                .queryParam("api_user", apiUser)
                .queryParam("api_secret", apiSecret)
                .queryParam("url", imageUrl)
                .encode()
                .build()
                .toUri();

        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            System.out.println("Error al consultar Sightengine: " + e.getMessage());
            return null;
        }
    }
}