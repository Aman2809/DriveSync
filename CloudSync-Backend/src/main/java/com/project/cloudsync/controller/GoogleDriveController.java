package com.project.cloudsync.controller;

import com.project.cloudsync.service.GoogleDriveService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drive")
@RequiredArgsConstructor
public class GoogleDriveController {

    private final GoogleDriveService googleDriveService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @GetMapping("/files")
    public List<Map<String, String>> listFiles(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) throws Exception {

        if (authorizedClient == null) {
            throw new RuntimeException("Not authenticated with Google");
        }

        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        return googleDriveService.listFiles(accessToken);
    }
}