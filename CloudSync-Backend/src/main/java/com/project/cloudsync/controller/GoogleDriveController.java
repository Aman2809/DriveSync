package com.project.cloudsync.controller;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.project.cloudsync.service.GoogleDriveService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/sync-folder")
    public Map<String, String> getOrCreateSyncFolder(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient
    ) throws Exception {

        if (authorizedClient == null) {
            throw new RuntimeException("Not authenticated with Google");
        }

        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        return googleDriveService.getOrCreateSyncFolder(accessToken);
    }

    @PostMapping("/upload")
    public String uploadFileToSyncFolder(@RequestParam("file") MultipartFile multipartFile,
                                         @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) {

        System.out.println("Upload Endpoint Hit");
        try {
            // Check authentication
            if (authorizedClient == null) {
                return "Authentication required. Please authenticate via Google first.";
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();

            // Convert MultipartFile to java.io.File
            java.io.File tempFile = java.io.File.createTempFile("upload-", multipartFile.getOriginalFilename());
            multipartFile.transferTo(tempFile);

            // Get Drive service
            Drive driveService = googleDriveService.getDriveService(accessToken);

            // Get or create sync folder and extract the folder ID
            Map<String, String> syncFolderInfo = googleDriveService.getOrCreateSyncFolder(accessToken);
            String syncFolderId = syncFolderInfo.get("id");

            // Upload file to the sync folder
            File uploadedFile = googleDriveService.uploadFileToSyncFolder(driveService, tempFile, syncFolderId);

            // Clean up temporary file
            tempFile.delete();

            return "File uploaded successfully. File ID: " + uploadedFile.getId() +
                    ", File Name: " + uploadedFile.getName();

        } catch (Exception e) {
            e.printStackTrace();
            return "Upload failed: " + e.getMessage();
        }
    }





}