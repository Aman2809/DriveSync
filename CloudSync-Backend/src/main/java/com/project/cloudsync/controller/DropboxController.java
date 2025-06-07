package com.project.cloudsync.controller;

import com.project.cloudsync.service.DropboxService;
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
@RequestMapping("/api/dropbox")
@RequiredArgsConstructor
public class DropboxController {

    private final DropboxService dropboxService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    // ============= SYNC FOLDER OPERATIONS =============

    @GetMapping("/sync-folder")
    public ResponseEntity<?> checkSyncFolder(
            @RegisteredOAuth2AuthorizedClient("dropbox") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated with Dropbox"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            Map<String, Object> folder = dropboxService.findSyncFolder(accessToken);

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
                    "error", "Failed to check sync folder",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/sync-folder")
    public ResponseEntity<?> ensureSyncFolder(
            @RegisteredOAuth2AuthorizedClient("dropbox") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated with Dropbox"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            Map<String, Object> folder = dropboxService.getOrCreateSyncFolder(accessToken);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "folder", folder,
                    "message", "Sync folder ready"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to create/get sync folder",
                    "message", e.getMessage()
            ));
        }
    }

    // ============= FILE LISTING OPERATIONS =============

    @GetMapping("/files")
    public ResponseEntity<?> listAllFiles(
            @RegisteredOAuth2AuthorizedClient("dropbox") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated with Dropbox"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            List<Map<String, Object>> files = dropboxService.listAllFiles(accessToken);

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
            @RegisteredOAuth2AuthorizedClient("dropbox") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated with Dropbox"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            List<Map<String, Object>> files = dropboxService.listFilesInSyncFolder(accessToken);

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
            @RegisteredOAuth2AuthorizedClient("dropbox") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated with Dropbox"));
            }

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            Map<String, Object> uploadedFile = dropboxService.uploadFileToSyncFolder(accessToken, file);

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

    @GetMapping("/files/metadata")
    public ResponseEntity<?> getFileMetadata(
            @RequestParam String filePath,
            @RegisteredOAuth2AuthorizedClient("dropbox") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated with Dropbox"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            Map<String, Object> fileMetadata = dropboxService.getFileMetadata(accessToken, filePath);

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
            @RegisteredOAuth2AuthorizedClient("dropbox") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated with Dropbox"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            Map<String, Object> file = dropboxService.findFileByNameInSyncFolder(accessToken, fileName);

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

    @GetMapping("/files/download")
    public ResponseEntity<?> downloadFile(
            @RequestParam String filePath,
            @RegisteredOAuth2AuthorizedClient("dropbox") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated with Dropbox"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();

            // Get file metadata first to get the file name
            Map<String, Object> fileMetadata = dropboxService.getFileMetadata(accessToken, filePath);
            String fileName = (String) fileMetadata.get("name");

            // Download the file content
            ByteArrayOutputStream fileContent = dropboxService.downloadFile(accessToken, filePath);

            // Determine content type (Dropbox doesn't provide MIME type, so we'll use a generic one)
            String contentType = "application/octet-stream";

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

    @PutMapping("/files/update")
    public ResponseEntity<?> updateFile(
            @RequestParam String filePath,
            @RequestParam("file") MultipartFile newFile,
            @RegisteredOAuth2AuthorizedClient("dropbox") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated with Dropbox"));
            }

            if (newFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "New file is empty"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            Map<String, Object> updatedFile = dropboxService.updateFile(accessToken, filePath, newFile);

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

    @DeleteMapping("/files/delete")
    public ResponseEntity<?> deleteFile(
            @RequestParam String filePath,
            @RegisteredOAuth2AuthorizedClient("dropbox") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated with Dropbox"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            Map<String, Object> result = dropboxService.deleteFile(accessToken, filePath);

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

    // ============= STORAGE INFO OPERATIONS =============

    @GetMapping("/storage-info")
    public ResponseEntity<?> getStorageInfo(
            @RegisteredOAuth2AuthorizedClient("dropbox") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated with Dropbox"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            Map<String, Object> storageInfo = dropboxService.getStorageInfo(accessToken);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "storage", storageInfo
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to get storage info",
                    "message", e.getMessage()
            ));
        }
    }

    // ============= UTILITY ENDPOINTS =============

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok(Map.of(
                "message", "Dropbox Controller is working!",
                "timestamp", System.currentTimeMillis()
        ));
    }

    @GetMapping("/file-exists")
    public ResponseEntity<?> checkFileExists(
            @RequestParam String filePath,
            @RegisteredOAuth2AuthorizedClient("dropbox") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated with Dropbox"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            boolean exists = dropboxService.fileExists(accessToken, filePath);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "exists", exists,
                    "filePath", filePath
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to check file existence",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/file-hash")
    public ResponseEntity<?> getFileContentHash(
            @RequestParam String filePath,
            @RegisteredOAuth2AuthorizedClient("dropbox") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated with Dropbox"));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();
            String contentHash = dropboxService.getFileContentHash(accessToken, filePath);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "filePath", filePath,
                    "contentHash", contentHash
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to get file content hash",
                    "message", e.getMessage()
            ));
        }
    }

    // ============= HEALTH CHECK =============

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck(
            @RegisteredOAuth2AuthorizedClient("dropbox") OAuth2AuthorizedClient authorizedClient) {
        try {
            if (authorizedClient == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "status", "unauthorized",
                        "message", "Not authenticated with Dropbox"
                ));
            }

            String accessToken = authorizedClient.getAccessToken().getTokenValue();

            // Try to get storage info as a health check
            Map<String, Object> storageInfo = dropboxService.getStorageInfo(accessToken);

            return ResponseEntity.ok(Map.of(
                    "status", "healthy",
                    "authenticated", true,
                    "message", "Dropbox connection is working",
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                    "status", "unhealthy",
                    "authenticated", true,
                    "error", "Dropbox service unavailable",
                    "message", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }
}