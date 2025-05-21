package com.project.cloudsync.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.File;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GoogleDriveService {

    public Drive getDriveService(String accessToken) throws GeneralSecurityException, IOException {
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        // Create a credential using the access token
        Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setTransport(transport)
                .setJsonFactory(jsonFactory)
                .build();

        // Set the access token
        credential.setAccessToken(accessToken);

        // Build the Drive service
        return new Drive.Builder(transport, jsonFactory, credential)
                .setApplicationName("CloudSync")
                .build();
    }

    public List<Map<String, String>> listFiles(String accessToken) throws IOException, GeneralSecurityException {
        Drive driveService = getDriveService(accessToken);
        FileList result = driveService.files().list()
                .setPageSize(10)
                .setFields("files(id, name, mimeType, modifiedTime, size)")
                .execute();

        List<Map<String, String>> files = new ArrayList<>();

        for (File file : result.getFiles()) {
            Map<String, String> fileInfo = new HashMap<>();
            fileInfo.put("id", file.getId());
            fileInfo.put("name", file.getName());
            fileInfo.put("mimeType", file.getMimeType());

            if (file.getModifiedTime() != null) {
                fileInfo.put("modifiedTime", file.getModifiedTime().toString());
            }

            if (file.getSize() != null) {
                fileInfo.put("size", file.getSize().toString());
            }

            files.add(fileInfo);
        }

        return files;
    }
}