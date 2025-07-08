package com.project.cloudsync.service;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import com.dropbox.core.v2.sharing.CreateSharedLinkWithSettingsErrorException;
import com.dropbox.core.v2.sharing.SharedLinkMetadata;
import com.dropbox.core.v2.users.SpaceUsage;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class DropboxService {

    private static final String SYNC_FOLDER_PATH = "/CloudSyncFolder";
    private static final String APPLICATION_NAME = "CloudSync";

    private DbxClientV2 getDropboxClient(String accessToken) {
        DbxRequestConfig config = DbxRequestConfig.newBuilder(APPLICATION_NAME).build();
        return new DbxClientV2(config, accessToken);
    }

    // ============= SYNC FOLDER OPERATIONS =============

    public Map<String, Object> getOrCreateSyncFolder(String accessToken) throws DbxException {
        DbxClientV2 client = getDropboxClient(accessToken);

        try {
            // Try to get the folder metadata
            Metadata metadata = client.files().getMetadata(SYNC_FOLDER_PATH);

            if (metadata instanceof FolderMetadata) {
                FolderMetadata folder = (FolderMetadata) metadata;
                return Map.of(
                        "path", folder.getPathLower(),
                        "name", folder.getName(),
                        "id", folder.getId(),
                        "status", "found"
                );
            }
        } catch (GetMetadataErrorException e) {
            // Folder doesn't exist, create it
            if (e.errorValue.isPath() && e.errorValue.getPathValue().isNotFound()) {
                try {
                    FolderMetadata createdFolder = client.files().createFolderV2(SYNC_FOLDER_PATH).getMetadata();
                    return Map.of(
                            "path", createdFolder.getPathLower(),
                            "name", createdFolder.getName(),
                            "id", createdFolder.getId(),
                            "status", "created"
                    );
                } catch (CreateFolderErrorException createEx) {
                    throw new RuntimeException("Failed to create sync folder", createEx);
                }
            } else {
                throw new RuntimeException("Error checking sync folder", e);
            }
        }

        throw new RuntimeException("Unexpected error with sync folder");
    }

    public Map<String, Object> findSyncFolder(String accessToken) throws DbxException {
        DbxClientV2 client = getDropboxClient(accessToken);

        try {
            Metadata metadata = client.files().getMetadata(SYNC_FOLDER_PATH);

            if (metadata instanceof FolderMetadata) {
                FolderMetadata folder = (FolderMetadata) metadata;
                return Map.of(
                        "path", folder.getPathLower(),
                        "name", folder.getName(),
                        "id", folder.getId(),
                        "status", "found"
                );
            }
        } catch (GetMetadataErrorException e) {
            if (e.errorValue.isPath() && e.errorValue.getPathValue().isNotFound()) {
                return null; // Folder not found
            }
            throw new RuntimeException("Error checking sync folder", e);
        }

        return null;
    }

    // ============= FILE LISTING OPERATIONS =============

    public List<Map<String, Object>> listFilesInSyncFolder(String accessToken) throws DbxException {
        DbxClientV2 client = getDropboxClient(accessToken);

        // First ensure sync folder exists
        Map<String, Object> syncFolder = findSyncFolder(accessToken);
        if (syncFolder == null) {
            throw new RuntimeException("Sync folder not found. Please create it first.");
        }

        try {
            ListFolderResult result = client.files().listFolder(SYNC_FOLDER_PATH);
            List<Map<String, Object>> files = new ArrayList<>();

            for (Metadata metadata : result.getEntries()) {
                if (metadata instanceof FileMetadata) {
                    FileMetadata file = (FileMetadata) metadata;
                    Map<String, Object> fileInfo = createFileInfoMap(file);
                    files.add(fileInfo);
                }
            }

            // Handle pagination if there are more files
            while (result.getHasMore()) {
                result = client.files().listFolderContinue(result.getCursor());
                for (Metadata metadata : result.getEntries()) {
                    if (metadata instanceof FileMetadata) {
                        FileMetadata file = (FileMetadata) metadata;
                        Map<String, Object> fileInfo = createFileInfoMap(file);
                        files.add(fileInfo);
                    }
                }
            }

            return files;

        } catch (ListFolderErrorException e) {
            throw new RuntimeException("Failed to list files in sync folder", e);
        }
    }

    public List<Map<String, Object>> listAllFiles(String accessToken) throws DbxException {
        DbxClientV2 client = getDropboxClient(accessToken);

        try {
            ListFolderResult result = client.files().listFolder("");
            List<Map<String, Object>> files = new ArrayList<>();

            for (Metadata metadata : result.getEntries()) {
                if (metadata instanceof FileMetadata) {
                    FileMetadata file = (FileMetadata) metadata;
                    Map<String, Object> fileInfo = createFileInfoMap(file);
                    files.add(fileInfo);
                } else if (metadata instanceof FolderMetadata) {
                    FolderMetadata folder = (FolderMetadata) metadata;
                    Map<String, Object> folderInfo = new HashMap<>();
                    folderInfo.put("id", folder.getId());
                    folderInfo.put("name", folder.getName());
                    folderInfo.put("path", folder.getPathLower());
                    folderInfo.put("type", "folder");
                    files.add(folderInfo);
                }
            }

            // Handle pagination
            while (result.getHasMore()) {
                result = client.files().listFolderContinue(result.getCursor());
                for (Metadata metadata : result.getEntries()) {
                    if (metadata instanceof FileMetadata) {
                        FileMetadata file = (FileMetadata) metadata;
                        Map<String, Object> fileInfo = createFileInfoMap(file);
                        files.add(fileInfo);
                    }
                }
            }

            return files;

        } catch (ListFolderErrorException e) {
            throw new RuntimeException("Failed to list files", e);
        }
    }

    // ============= FILE UPLOAD OPERATIONS =============

    public Map<String, Object> uploadFileToSyncFolder(String accessToken, MultipartFile file) throws DbxException, IOException {
        DbxClientV2 client = getDropboxClient(accessToken);

        // Ensure sync folder exists
        getOrCreateSyncFolder(accessToken);

        String filePath = SYNC_FOLDER_PATH + "/" + file.getOriginalFilename();

        try (InputStream inputStream = file.getInputStream()) {
            FileMetadata uploadedFile;

            if (file.getSize() <= 150 * 1024 * 1024) { // 150MB - use simple upload
                uploadedFile = client.files().uploadBuilder(filePath)
                        .withMode(WriteMode.ADD)
                        .withAutorename(true)
                        .uploadAndFinish(inputStream);
            } else {
                // For larger files, use upload session (chunked upload)
                uploadedFile = uploadLargeFile(client, filePath, inputStream, file.getSize());
            }

            return createFileInfoMap(uploadedFile);

        } catch (UploadErrorException e) {
            throw new RuntimeException("Failed to upload file to Dropbox", e);
        }
    }

    // Fixed uploadLargeFile method for DropboxService.java
    private FileMetadata uploadLargeFile(DbxClientV2 client, String filePath, InputStream inputStream, long fileSize) throws DbxException, IOException {
        int chunkSize = 8 * 1024 * 1024; // 8MB chunks
        byte[] buffer = new byte[chunkSize];

        // Read first chunk
        int bytesRead = inputStream.read(buffer);
        if (bytesRead == -1) {
            throw new RuntimeException("Cannot read from input stream");
        }

        // Start upload session with first chunk
        byte[] firstChunk = Arrays.copyOf(buffer, bytesRead);
        UploadSessionStartResult sessionStart = client.files().uploadSessionStart()
                .uploadAndFinish(new ByteArrayInputStream(firstChunk));

        String sessionId = sessionStart.getSessionId();
        long uploaded = bytesRead;

        // Continue uploading remaining chunks
        while (uploaded < fileSize) {
            bytesRead = inputStream.read(buffer);
            if (bytesRead == -1) break;

            byte[] chunk = Arrays.copyOf(buffer, bytesRead);

            if (uploaded + bytesRead < fileSize) {
                // Append intermediate chunk
                UploadSessionCursor cursor = new UploadSessionCursor(sessionId, uploaded);
                client.files().uploadSessionAppendV2(cursor)
                        .uploadAndFinish(new ByteArrayInputStream(chunk));
            } else {
                // Final chunk - finish the session
                UploadSessionCursor cursor = new UploadSessionCursor(sessionId, uploaded);
                CommitInfo commitInfo = CommitInfo.newBuilder(filePath)
                        .withMode(WriteMode.ADD)
                        .withAutorename(true)
                        .build();

                return client.files().uploadSessionFinish(cursor, commitInfo)
                        .uploadAndFinish(new ByteArrayInputStream(chunk));
            }

            uploaded += bytesRead;
        }

        // If we reach here, something went wrong
        throw new RuntimeException("Upload session completed unexpectedly");
    }

    // ============= FILE DOWNLOAD OPERATIONS =============

    public ByteArrayOutputStream downloadFile(String accessToken, String filePath) throws DbxException, IOException {
        DbxClientV2 client = getDropboxClient(accessToken);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            client.files().downloadBuilder(filePath).download(outputStream);
            return outputStream;
        } catch (DownloadErrorException e) {
            throw new RuntimeException("Failed to download file from Dropbox", e);
        }
    }

    // ============= FILE METADATA OPERATIONS =============

    public Map<String, Object> getFileMetadata(String accessToken, String filePath) throws DbxException {
        DbxClientV2 client = getDropboxClient(accessToken);

        try {
            Metadata metadata = client.files().getMetadata(filePath);

            if (metadata instanceof FileMetadata) {
                FileMetadata file = (FileMetadata) metadata;
                return createFileInfoMap(file);
            } else {
                throw new RuntimeException("Path does not point to a file");
            }
        } catch (GetMetadataErrorException e) {
            throw new RuntimeException("Failed to get file metadata", e);
        }
    }

    public Map<String, Object> findFileByNameInSyncFolder(String accessToken, String fileName) throws DbxException {
        DbxClientV2 client = getDropboxClient(accessToken);

        // Check if sync folder exists
        if (findSyncFolder(accessToken) == null) {
            return null;
        }

        String filePath = SYNC_FOLDER_PATH + "/" + fileName;

        try {
            Metadata metadata = client.files().getMetadata(filePath);

            if (metadata instanceof FileMetadata) {
                FileMetadata file = (FileMetadata) metadata;
                Map<String, Object> fileInfo = createFileInfoMap(file);
                fileInfo.put("found", true);
                return fileInfo;
            }
        } catch (GetMetadataErrorException e) {
            if (e.errorValue.isPath() && e.errorValue.getPathValue().isNotFound()) {
                return null; // File not found
            }
            throw new RuntimeException("Error searching for file", e);
        }

        return null;
    }

    // ============= FILE UPDATE OPERATIONS =============

    public Map<String, Object> updateFile(String accessToken, String filePath, MultipartFile newFile) throws DbxException, IOException {
        DbxClientV2 client = getDropboxClient(accessToken);

        try (InputStream inputStream = newFile.getInputStream()) {
            FileMetadata updatedFile;

            if (newFile.getSize() <= 150 * 1024 * 1024) { // 150MB - use simple upload
                updatedFile = client.files().uploadBuilder(filePath)
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(inputStream);
            } else {
                // For larger files, use upload session
                updatedFile = uploadLargeFile(client, filePath, inputStream, newFile.getSize());
            }

            Map<String, Object> fileInfo = createFileInfoMap(updatedFile);
            fileInfo.put("status", "updated");
            return fileInfo;

        } catch (UploadErrorException e) {
            throw new RuntimeException("Failed to update file in Dropbox", e);
        }
    }

    // ============= FILE DELETE OPERATIONS =============

    public Map<String, Object> deleteFile(String accessToken, String filePath) throws DbxException {
        DbxClientV2 client = getDropboxClient(accessToken);

        try {
            // Get file info before deletion
            Metadata metadata = client.files().getMetadata(filePath);
            String fileName = metadata.getName();

            // Delete the file
            client.files().deleteV2(filePath);

            return Map.of(
                    "path", filePath,
                    "name", fileName,
                    "status", "deleted",
                    "message", "File deleted successfully"
            );

        } catch (DeleteErrorException e) {
            throw new RuntimeException("Failed to delete file from Dropbox", e);
        } catch (GetMetadataErrorException e) {
            throw new RuntimeException("File not found for deletion", e);
        }
    }

    // ============= STORAGE INFO OPERATIONS =============

    public Map<String, Object> getStorageInfo(String accessToken) throws DbxException {
        DbxClientV2 client = getDropboxClient(accessToken);

        try {
            SpaceUsage spaceUsage = client.users().getSpaceUsage();

            Map<String, Object> storageInfo = new HashMap<>();
            storageInfo.put("used", spaceUsage.getUsed());
            storageInfo.put("allocated", spaceUsage.getAllocation().getIndividualValue().getAllocated());

            long available = spaceUsage.getAllocation().getIndividualValue().getAllocated() - spaceUsage.getUsed();
            storageInfo.put("available", available);

            double usagePercentage = (double) spaceUsage.getUsed() / spaceUsage.getAllocation().getIndividualValue().getAllocated() * 100;
            storageInfo.put("usagePercentage", Math.round(usagePercentage * 100.0) / 100.0);

            return storageInfo;

        } catch (Exception e) {
            throw new RuntimeException("Failed to get storage information", e);
        }
    }

    // ============= HELPER METHODS =============

    private Map<String, Object> createFileInfoMap(FileMetadata file) {
        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("id", file.getId());
        fileInfo.put("name", file.getName());
        fileInfo.put("path", file.getPathLower());
        fileInfo.put("size", file.getSize());
        fileInfo.put("modifiedTime", file.getClientModified().toString());
        fileInfo.put("serverModifiedTime", file.getServerModified().toString());
        fileInfo.put("contentHash", file.getContentHash());
        fileInfo.put("type", "file");

        // Extract parent folder path
        String parentPath = file.getPathLower().contains("/") ?
                file.getPathLower().substring(0, file.getPathLower().lastIndexOf("/")) : "/";
        fileInfo.put("parentPath", parentPath);

        return fileInfo;
    }

    // ============= SYNC HELPER METHODS =============

    public boolean fileExists(String accessToken, String filePath) {
        try {
            DbxClientV2 client = getDropboxClient(accessToken);
            Metadata metadata = client.files().getMetadata(filePath);
            return metadata instanceof FileMetadata;
        } catch (DbxException e) {
            return false;
        }
    }

    public String getFileContentHash(String accessToken, String filePath) throws DbxException {
        Map<String, Object> metadata = getFileMetadata(accessToken, filePath);
        return (String) metadata.get("contentHash");
    }



    public Map<String, Object> renameFileOrFolder(String accessToken, String fileId, String newName) throws Exception {
        DbxClientV2 client = getDropboxClient(accessToken);

        // Step 1: Fetch metadata from file ID
        ListFolderResult result = client.files().listFolder(SYNC_FOLDER_PATH);

        String currentPath = null;

        // Search for the file inside the sync folder
        for (Metadata metadata : result.getEntries()) {
            if (metadata instanceof FileMetadata && ((FileMetadata) metadata).getId().equals(fileId)) {
                currentPath = metadata.getPathLower();
                break;
            }
        }

        // If not found, check deeper in subfolders (pagination + recursion)
        while (result.getHasMore()) {
            result = client.files().listFolderContinue(result.getCursor());
            for (Metadata metadata : result.getEntries()) {
                if (metadata instanceof FileMetadata && ((FileMetadata) metadata).getId().equals(fileId)) {
                    currentPath = metadata.getPathLower();
                    break;
                }
            }
        }

        if (currentPath == null) {
            throw new RuntimeException("File with ID " + fileId + " not found in Dropbox sync folder");
        }

        // Step 2: Build new path using new name
        String newPath = currentPath.contains("/") ?
                currentPath.substring(0, currentPath.lastIndexOf("/")) + "/" + newName :
                "/" + newName;

        // Step 3: Move (rename) the file
        try {
            Metadata metadata = client.files().moveV2(currentPath, newPath).getMetadata();

            Map<String, Object> response = new HashMap<>();
            response.put("oldPath", currentPath);
            response.put("newPath", newPath);
            response.put("name", metadata.getName());
            response.put("status", "renamed");
            return response;

        } catch (RelocationErrorException e) {
            throw new RuntimeException("Failed to rename file or folder", e);
        }
    }

    public Map<String, Object> createSubFolder(String accessToken, String subFolderName) throws DbxException {
        DbxClientV2 client = getDropboxClient(accessToken);

        String path = SYNC_FOLDER_PATH + "/" + subFolderName;

        try {
            FolderMetadata folder = client.files().createFolderV2(path).getMetadata();

            return Map.of(
                    "path", folder.getPathLower(),
                    "name", folder.getName(),
                    "id", folder.getId(),
                    "status", "created"
            );

        } catch (CreateFolderErrorException e) {
            throw new RuntimeException("Failed to create subfolder", e);
        }
    }

    public Map<String, Object> moveFile(String accessToken, String sourcePath, String destinationPath) throws DbxException {
        DbxClientV2 client = getDropboxClient(accessToken);

        try {
            Metadata metadata = client.files().moveV2(sourcePath, destinationPath).getMetadata();

            return Map.of(
                    "from", sourcePath,
                    "to", destinationPath,
                    "status", "moved",
                    "name", metadata.getName()
            );

        } catch (RelocationErrorException e) {
            throw new RuntimeException("Failed to move file", e);
        }
    }

    public String createShareableLink(String accessToken, String filePath) throws DbxException {
        DbxClientV2 client = getDropboxClient(accessToken);

        try {
            SharedLinkMetadata sharedLink = client.sharing().createSharedLinkWithSettings(filePath);
            return sharedLink.getUrl();
        } catch (CreateSharedLinkWithSettingsErrorException e) {
            throw new RuntimeException("Failed to create shared link", e);
        }
    }

    public byte[] downloadFilePartial(String accessToken, String filePath, int startByte, int endByte) throws DbxException, IOException {
        ByteArrayOutputStream fullStream = downloadFile(accessToken, filePath);
        byte[] fullData = fullStream.toByteArray();

        if (startByte < 0 || endByte >= fullData.length || startByte > endByte) {
            throw new IllegalArgumentException("Invalid byte range");
        }

        return Arrays.copyOfRange(fullData, startByte, endByte + 1);
    }



}
