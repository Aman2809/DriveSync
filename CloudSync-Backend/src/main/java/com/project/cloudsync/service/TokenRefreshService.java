//package com.project.cloudsync.service;
//
//import com.project.cloudsync.entities.User;
//import com.project.cloudsync.repositories.UserRepository;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.time.Instant;
//import java.util.*;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class TokenRefreshService {
//
//    private final UserRepository userRepository;
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    // Google refresh
//    @Transactional
//    public void refreshGoogleToken(User user) {
//        if (user.getGoogleRefreshToken() == null) {
//            log.warn("No Google refresh token for user {}", user.getEmail());
//            return;
//        }
//
//        String url = "https://oauth2.googleapis.com/token";
//
//        Map<String, String> params = new HashMap<>();
//        params.put("client_id", "YOUR_GOOGLE_CLIENT_ID");
//        params.put("client_secret", "YOUR_GOOGLE_CLIENT_SECRET");
//        params.put("refresh_token", user.getGoogleRefreshToken());
//        params.put("grant_type", "refresh_token");
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//
//        HttpEntity<Map<String, String>> entity = new HttpEntity<>(params, headers);
//
//        try {
//            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
//
//            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
//                Map body = response.getBody();
//                String newAccessToken = (String) body.get("access_token");
//                Integer expiresIn = (Integer) body.get("expires_in");
//
//                user.setGoogleAccessToken(newAccessToken);
//                userRepository.save(user);
//
//                log.info("✅ Refreshed Google access token for user {} (expires in {}s)", user.getEmail(), expiresIn);
//            } else {
//                log.error("Failed to refresh Google token: {}", response);
//            }
//        } catch (Exception e) {
//            log.error("Error refreshing Google token: {}", e.getMessage(), e);
//        }
//    }
//
//    // Dropbox refresh
//    @Transactional
//    public void refreshDropboxToken(User user) {
//        if (user.getDropboxRefreshToken() == null) {
//            log.warn("No Dropbox refresh token for user {}", user.getEmail());
//            return;
//        }
//
//        String url = "https://api.dropboxapi.com/oauth2/token";
//
//        Map<String, String> params = new HashMap<>();
//        params.put("client_id", "YOUR_DROPBOX_CLIENT_ID");
//        params.put("client_secret", "YOUR_DROPBOX_CLIENT_SECRET");
//        params.put("refresh_token", user.getDropboxRefreshToken());
//        params.put("grant_type", "refresh_token");
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//
//        HttpEntity<Map<String, String>> entity = new HttpEntity<>(params, headers);
//
//        try {
//            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
//
//            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
//                Map body = response.getBody();
//                String newAccessToken = (String) body.get("access_token");
//                Integer expiresIn = (Integer) body.get("expires_in");
//
//                user.setDropboxAccessToken(newAccessToken);
//                userRepository.save(user);
//
//                log.info("✅ Refreshed Dropbox access token for user {} (expires in {}s)", user.getEmail(), expiresIn);
//            } else {
//                log.error("Failed to refresh Dropbox token: {}", response);
//            }
//        } catch (Exception e) {
//            log.error("Error refreshing Dropbox token: {}", e.getMessage(), e);
//        }
//    }
//}
