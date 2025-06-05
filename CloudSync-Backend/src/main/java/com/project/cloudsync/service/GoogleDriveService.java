package com.project.cloudsync.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.File;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.*;

@Service
public class GoogleDriveService {

    public static final String APPLICATION_NAME = "CloudSync";
    public static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String SYNC_FOLDER_NAME = "CloudSyncFolder";

    public Drive getDriveService(String accessToken) throws GeneralSecurityException, IOException {
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();

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

    public Map<String, String> getOrCreateSyncFolder(String accessToken) throws Exception {
        Drive driveService = getDriveService(accessToken);

        // Search for the folder
        String query = String.format("mimeType = 'application/vnd.google-apps.folder' and name = '%s' and trashed = false", SYNC_FOLDER_NAME);
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
        fileMetadata.setName(SYNC_FOLDER_NAME);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        File folder = driveService.files().create(fileMetadata)
                .setFields("id, name")
                .execute();

        return Map.of("id", folder.getId(), "name", folder.getName());
    }

    public Map<String, String> findSyncFolder(String accessToken) throws Exception {
        Drive driveService = getDriveService(accessToken);

        String query = String.format("mimeType = 'application/vnd.google-apps.folder' and name = '%s' and trashed = false", SYNC_FOLDER_NAME);
        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        if (!result.getFiles().isEmpty()) {
            File folder = result.getFiles().get(0);
            return Map.of(
                    "id", folder.getId(),
                    "name", folder.getName(),
                    "status", "found"
            );
        }

        return null;
    }

    // NEW: List files specifically in the sync folder
    public List<Map<String, Object>> listFilesInSyncFolder(String accessToken) throws Exception {
        Drive driveService = getDriveService(accessToken);

        // First, get the sync folder
        Map<String, String> syncFolder = findSyncFolder(accessToken);
        if (syncFolder == null) {
            throw new RuntimeException("Sync folder not found. Please create it first.");
        }

        String folderId = syncFolder.get("id");

        // Query files in the sync folder
        String query = String.format("'%s' in parents and trashed = false", folderId);
        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, mimeType, modifiedTime, size, md5Checksum, parents)")
                .execute();

        List<Map<String, Object>> files = new ArrayList<>();
        for (File file : result.getFiles()) {
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("id", file.getId());
            fileInfo.put("name", file.getName());
            fileInfo.put("mimeType", file.getMimeType());
            fileInfo.put("parentFolderId", folderId);

            if (file.getModifiedTime() != null) {
                fileInfo.put("modifiedTime", file.getModifiedTime().toString());
            }

            if (file.getSize() != null) {
                fileInfo.put("size", file.getSize());
            }

            if (file.getMd5Checksum() != null) {
                fileInfo.put("md5Checksum", file.getMd5Checksum());
            }

            files.add(fileInfo);
        }

        return files;
    }

    // NEW: Download file from Google Drive
    public ByteArrayOutputStream downloadFile(String accessToken, String fileId) throws Exception {
        Drive driveService = getDriveService(accessToken);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);

        return outputStream;
    }

    // NEW: Get file metadata
    public Map<String, Object> getFileMetadata(String accessToken, String fileId) throws Exception {
        Drive driveService = getDriveService(accessToken);

        File file = driveService.files().get(fileId)
                .setFields("id, name, mimeType, modifiedTime, size, md5Checksum, parents")
                .execute();

        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("id", file.getId());
        fileInfo.put("name", file.getName());
        fileInfo.put("mimeType", file.getMimeType());

        if (file.getModifiedTime() != null) {
            fileInfo.put("modifiedTime", file.getModifiedTime().toString());
        }

        if (file.getSize() != null) {
            fileInfo.put("size", file.getSize());
        }

        if (file.getMd5Checksum() != null) {
            fileInfo.put("md5Checksum", file.getMd5Checksum());
        }

        if (file.getParents() != null && !file.getParents().isEmpty()) {
            fileInfo.put("parentFolderId", file.getParents().get(0));
        }

        return fileInfo;
    }

    public Map<String, String> uploadFileToSyncFolder(String accessToken, MultipartFile multipartFile) throws Exception {
        Drive driveService = getDriveService(accessToken);

        // First, get or create the sync folder
        Map<String, String> syncFolder = getOrCreateSyncFolder(accessToken);
        String folderId = syncFolder.get("id");

        // Prepare file metadata
        File fileMetadata = new File();
        fileMetadata.setName(multipartFile.getOriginalFilename());
        fileMetadata.setParents(Collections.singletonList(folderId));

        // Prepare file content
        InputStream inputStream = multipartFile.getInputStream();
        InputStreamContent mediaContent = new InputStreamContent(
                multipartFile.getContentType(),
                inputStream
        );
        mediaContent.setLength(multipartFile.getSize());

        // Upload the file
        File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, name, size, mimeType, modifiedTime, md5Checksum")
                .execute();

        // Close the input stream
        try {
            inputStream.close();
        } catch (IOException e) {
            System.err.println("Warning: Could not close input stream: " + e.getMessage());
        }

        // Return file information
        Map<String, String> fileInfo = new HashMap<>();
        fileInfo.put("id", uploadedFile.getId());
        fileInfo.put("name", uploadedFile.getName());
        fileInfo.put("mimeType", uploadedFile.getMimeType());
        fileInfo.put("parentFolderId", folderId);
        fileInfo.put("status", "uploaded");

        if (uploadedFile.getSize() != null) {
            fileInfo.put("size", uploadedFile.getSize().toString());
        }

        if (uploadedFile.getModifiedTime() != null) {
            fileInfo.put("modifiedTime", uploadedFile.getModifiedTime().toString());
        }

        if (uploadedFile.getMd5Checksum() != null) {
            fileInfo.put("md5Checksum", uploadedFile.getMd5Checksum());
        }

        return fileInfo;
    }

    // NEW: Update/Replace an existing file
    public Map<String, Object> updateFile(String accessToken, String fileId, MultipartFile newFile) throws Exception {
        Drive driveService = getDriveService(accessToken);

        // Prepare file metadata (optional - you can update name if needed)
        File fileMetadata = new File();
        // Uncomment if you want to change the file name
        // fileMetadata.setName(newFile.getOriginalFilename());

        // Prepare file content
        InputStream inputStream = newFile.getInputStream();
        InputStreamContent mediaContent = new InputStreamContent(
                newFile.getContentType(),
                inputStream
        );
        mediaContent.setLength(newFile.getSize());

        // Update the file
        File updatedFile = driveService.files().update(fileId, fileMetadata, mediaContent)
                .setFields("id, name, size, mimeType, modifiedTime, md5Checksum, parents")
                .execute();

        // Close the input stream
        try {
            inputStream.close();
        } catch (IOException e) {
            System.err.println("Warning: Could not close input stream: " + e.getMessage());
        }

        // Return updated file information
        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("id", updatedFile.getId());
        fileInfo.put("name", updatedFile.getName());
        fileInfo.put("mimeType", updatedFile.getMimeType());
        fileInfo.put("status", "updated");

        if (updatedFile.getSize() != null) {
            fileInfo.put("size", updatedFile.getSize());
        }

        if (updatedFile.getModifiedTime() != null) {
            fileInfo.put("modifiedTime", updatedFile.getModifiedTime().toString());
        }

        if (updatedFile.getMd5Checksum() != null) {
            fileInfo.put("md5Checksum", updatedFile.getMd5Checksum());
        }

        if (updatedFile.getParents() != null && !updatedFile.getParents().isEmpty()) {
            fileInfo.put("parentFolderId", updatedFile.getParents().get(0));
        }

        return fileInfo;
    }

    // NEW: Delete a file
    public Map<String, String> deleteFile(String accessToken, String fileId) throws Exception {
        Drive driveService = getDriveService(accessToken);

        // Get file info before deletion (for confirmation)
        File fileToDelete = driveService.files().get(fileId)
                .setFields("id, name")
                .execute();

        // Delete the file
        driveService.files().delete(fileId).execute();

        // Return confirmation
        Map<String, String> result = new HashMap<>();
        result.put("id", fileToDelete.getId());
        result.put("name", fileToDelete.getName());
        result.put("status", "deleted");
        result.put("message", "File deleted successfully");

        return result;
    }

    // NEW: Check if file exists by name in sync folder
    public Map<String, Object> findFileByNameInSyncFolder(String accessToken, String fileName) throws Exception {
        Drive driveService = getDriveService(accessToken);

        // Get sync folder
        Map<String, String> syncFolder = findSyncFolder(accessToken);
        if (syncFolder == null) {
            return null;
        }

        String folderId = syncFolder.get("id");

        // Search for file by name in the sync folder
        String query = String.format("'%s' in parents and name = '%s' and trashed = false", folderId, fileName);
        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, mimeType, modifiedTime, size, md5Checksum)")
                .execute();

        if (!result.getFiles().isEmpty()) {
            File file = result.getFiles().get(0);
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("id", file.getId());
            fileInfo.put("name", file.getName());
            fileInfo.put("mimeType", file.getMimeType());
            fileInfo.put("parentFolderId", folderId);
            fileInfo.put("found", true);

            if (file.getModifiedTime() != null) {
                fileInfo.put("modifiedTime", file.getModifiedTime().toString());
            }

            if (file.getSize() != null) {
                fileInfo.put("size", file.getSize());
            }

            if (file.getMd5Checksum() != null) {
                fileInfo.put("md5Checksum", file.getMd5Checksum());
            }

            return fileInfo;
        }

        return null;
    }

    // Original methods (keeping for backward compatibility)
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