package com.project.cloudsync.controller;

import com.project.cloudsync.service.GoogleDriveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drive")
@RequiredArgsConstructor
public class GoogleDriveController {

    private final GoogleDriveService googleDriveService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    // ============= SYNC FOLDER OPERATIONS =============

    @GetMapping("/sync-folder")
    public ResponseEntity<?> checkSyncFolder(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            Map<String, String> folder = googleDriveService.findSyncFolder(accessToken);

            if (folder != null) {
                return ResponseEntity.ok(Map.of(
                        "found", true,
                        "folder", folder,
                        "message", "Sync folder exists"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "found", false,
                        "message", "Sync folder does not exist",
                        "suggestion", "Use POST /sync-folder to create it"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to check folder",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/sync-folder")
    public ResponseEntity<?> ensureSyncFolder(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            Map<String, String> folder = googleDriveService.getOrCreateSyncFolder(accessToken);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "folder", folder,
                    "message", "Sync folder ready"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to create/get folder",
                    "message", e.getMessage()
            ));
        }
    }

    // ============= FILE LISTING OPERATIONS =============

    @GetMapping("/files")
    public ResponseEntity<?> listAllFiles(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            List<Map<String, String>> files = googleDriveService.listFiles(accessToken);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "files", files,
                    "count", files.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to list files",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/sync-folder/files")
    public ResponseEntity<?> listSyncFolderFiles(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            List<Map<String, Object>> files = googleDriveService.listFilesInSyncFolder(accessToken);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "files", files,
                    "count", files.size(),
                    "message", "Files in sync folder"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to list sync folder files",
                    "message", e.getMessage()
            ));
        }
    }

    // ============= FILE UPLOAD OPERATIONS =============

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            Map<String, String> uploadedFile = googleDriveService.uploadFileToSyncFolder(accessToken, file);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "file", uploadedFile,
                    "message", "File uploaded successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to upload file",
                    "message", e.getMessage()
            ));
        }
    }

    // ============= FILE METADATA OPERATIONS =============

    @GetMapping("/files/{fileId}")
    public ResponseEntity<?> getFileMetadata(
            @PathVariable String fileId,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            Map<String, Object> fileMetadata = googleDriveService.getFileMetadata(accessToken, fileId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "file", fileMetadata
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to get file metadata",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/sync-folder/files/search")
    public ResponseEntity<?> searchFileByName(
            @RequestParam String fileName,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            Map<String, Object> file = googleDriveService.findFileByNameInSyncFolder(accessToken, fileName);

            if (file != null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "found", true,
                        "file", file
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "found", false,
                        "message", "File not found in sync folder"
                ));
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to search file",
                    "message", e.getMessage()
            ));
        }
    }

    // ============= FILE DOWNLOAD OPERATIONS =============

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<?> downloadFile(
            @PathVariable String fileId,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();

            // Get file metadata first to get the file name
            Map<String, Object> fileMetadata = googleDriveService.getFileMetadata(accessToken, fileId);
            String fileName = (String) fileMetadata.get("name");

            // Download the file content
            ByteArrayOutputStream fileContent = googleDriveService.downloadFile(accessToken, fileId);

            // Determine content type
            String contentType = (String) fileMetadata.get("mimeType");
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(fileContent.toByteArray());

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to download file",
                    "message", e.getMessage()
            ));
        }
    }

    // ============= FILE UPDATE OPERATIONS =============

    @PutMapping("/files/{fileId}")
    public ResponseEntity<?> updateFile(
            @PathVariable String fileId,
            @RequestParam("file") MultipartFile newFile,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            if (newFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "New file is empty"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            Map<String, Object> updatedFile = googleDriveService.updateFile(accessToken, fileId, newFile);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "file", updatedFile,
                    "message", "File updated successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to update file",
                    "message", e.getMessage()
            ));
        }
    }

    // ============= FILE DELETE OPERATIONS =============

    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<?> deleteFile(
            @PathVariable String fileId,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            Map<String, String> result = googleDriveService.deleteFile(accessToken, fileId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "result", result,
                    "message", "File deleted successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to delete file",
                    "message", e.getMessage()
            ));
        }
    }

    // ============= UTILITY ENDPOINTS =============

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok(Map.of(
                "message", "Google Drive Controller is working!",
                "timestamp", System.currentTimeMillis()
        ));
    }

    @GetMapping("/storage-info")
    public ResponseEntity<?> getStorageInfo(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            // Note: You'll need to implement this method in the service if you want storage info
            // For now, returning a placeholder response
            return ResponseEntity.ok(Map.of(
                    "message", "Storage info endpoint - to be implemented",
                    "suggestion", "Use Google Drive API's about.get() method to get storage quota"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to get storage info",
                    "message", e.getMessage()
            ));
        }
    }
}