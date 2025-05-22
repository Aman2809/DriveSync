package com.project.cloudsync.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
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
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.*;

@Service
public class GoogleDriveService {

    public static final String APPLICATION_NAME = "CloudSync";
    public static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public Drive getDriveService(String accessToken) throws GeneralSecurityException, IOException {
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
//        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        // Create a credential using the access token
        Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setTransport(transport)
                .setJsonFactory(JSON_FACTORY)
                .build();

        // Set the access token
        credential.setAccessToken(accessToken);

        // Build the Drive service
        return new Drive.Builder(transport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    //getOrCreateFolder
    public Map<String, String> getOrCreateSyncFolder(String accessToken) throws Exception {
        Drive driveService = getDriveService(accessToken);

        // Search for the folder
        String folderName = "CloudSyncFolder";
        String query = String.format("mimeType = 'application/vnd.google-apps.folder' and name = '%s' and trashed = false", folderName);
        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        if (!result.getFiles().isEmpty()) {
            File folder = result.getFiles().get(0);
            return Map.of("id", folder.getId(), "name", folder.getName());
        }

        // Folder not found, so create it
        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        File folder = driveService.files().create(fileMetadata)
                .setFields("id, name")
                .execute();

        return Map.of("id", folder.getId(), "name", folder.getName());
    }

    public File uploadFileToSyncFolder(Drive driveService , java.io.File filePath, String syncFolderId) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(filePath.getName());
        fileMetadata.setParents(Collections.singletonList(syncFolderId));

        FileContent mediaContent=new FileContent("application/octet-stream", filePath);

        return driveService.files().create(fileMetadata,mediaContent)
                .setFields("id, name")
                .execute();
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