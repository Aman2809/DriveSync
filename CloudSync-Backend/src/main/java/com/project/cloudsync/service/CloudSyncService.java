//package com.project.cloudsync.service;
//
//import com.project.cloudsync.helper.InMemoryMultipartFile;
//import com.project.cloudsync.service.DropboxService;
//import com.project.cloudsync.service.GoogleDriveService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.time.Instant;
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.time.ZonedDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//public class CloudSyncService {
//
//
//    private final GoogleDriveService googleDriveService;
//
//
//    private final DropboxService dropboxService;
//
//    public CloudSyncService(GoogleDriveService googleDriveService, DropboxService dropboxService) {
//        this.googleDriveService = googleDriveService;
//        this.dropboxService = dropboxService;
//    }
//
//    // -------------------- ONE-WAY SYNC: Dropbox → Google Drive --------------------
//    public void oneWaySyncDropboxToGDrive(String dropboxToken, String gDriveToken) throws Exception {
//        List<Map<String, Object>> dropboxFiles = dropboxService.listFilesInSyncFolder(dropboxToken);
//        List<Map<String, Object>> gDriveFiles = googleDriveService.listFilesInSyncFolder(gDriveToken);
//
//        Map<String, Map<String, Object>> gDriveMap = gDriveFiles.stream()
//                .collect(Collectors.toMap(f -> (String) f.get("name"), f -> f));
//
//        for (Map<String, Object> dropboxFile : dropboxFiles) {
//            String fileName = (String) dropboxFile.get("name");
//            String dropboxFileId = (String) dropboxFile.get("id");
//
//            Map<String, Object> gFile = gDriveMap.get(fileName);
//
//            if (gFile == null || isNewer(dropboxFile, gFile)) {
//                ByteArrayOutputStream content = dropboxService.downloadFile(dropboxToken, dropboxFileId);
//                MultipartFile multipartFile = new InMemoryMultipartFile(fileName, content.toByteArray());
//                googleDriveService.uploadFileToSyncFolder(gDriveToken, multipartFile);
//                System.out.println("Synced from Dropbox → Google Drive: " + fileName);
//            }
//        }
//    }
//
//    // -------------------- ONE-WAY SYNC: Google Drive → Dropbox --------------------
//    public void oneWaySyncGDriveToDropbox(String gDriveToken, String dropboxToken) throws Exception {
//        List<Map<String, Object>> gDriveFiles = googleDriveService.listFilesInSyncFolder(gDriveToken);
//        List<Map<String, Object>> dropboxFiles = dropboxService.listFilesInSyncFolder(dropboxToken);
//
//        Map<String, Map<String, Object>> dropboxMap = dropboxFiles.stream()
//                .collect(Collectors.toMap(f -> (String) f.get("name"), f -> f));
//
//        for (Map<String, Object> gFile : gDriveFiles) {
//            String fileName = (String) gFile.get("name");
//            String gDriveFileId = (String) gFile.get("id");
//
//            Map<String, Object> dFile = dropboxMap.get(fileName);
//
//            if (dFile == null || isNewer(gFile, dFile)) {
//                ByteArrayOutputStream content = googleDriveService.downloadFile(gDriveToken, gDriveFileId);
//                MultipartFile multipartFile = new InMemoryMultipartFile(fileName, content.toByteArray());
//                dropboxService.uploadFileToSyncFolder(dropboxToken, multipartFile);
//                System.out.println("Synced from Google Drive → Dropbox: " + fileName);
//            }
//        }
//    }
//
//    // -------------------- BIDIRECTIONAL SYNC --------------------
//    public void bidirectionalSync(String gDriveToken, String dropboxToken) throws Exception {
//        List<Map<String, Object>> gFiles = googleDriveService.listFilesInSyncFolder(gDriveToken);
//        List<Map<String, Object>> dFiles = dropboxService.listFilesInSyncFolder(dropboxToken);
//
//        Map<String, Map<String, Object>> gMap = gFiles.stream()
//                .collect(Collectors.toMap(f -> (String) f.get("name"), f -> f));
//        Map<String, Map<String, Object>> dMap = dFiles.stream()
//                .collect(Collectors.toMap(f -> (String) f.get("name"), f -> f));
//
//        Set<String> allFileNames = new HashSet<>();
//        allFileNames.addAll(gMap.keySet());
//        allFileNames.addAll(dMap.keySet());
//
//        for (String fileName : allFileNames) {
//            Map<String, Object> gFile = gMap.get(fileName);
//            Map<String, Object> dFile = dMap.get(fileName);
//
//            if (gFile == null && dFile != null) {
////                System.out.println("Attempting: Dropbox → GDrive");
//                try {
//                    ByteArrayOutputStream content = dropboxService.downloadFile(dropboxToken, (String) dFile.get("id"));
//                    MultipartFile multipartFile = new InMemoryMultipartFile(fileName, content.toByteArray());
//                    googleDriveService.uploadFileToSyncFolder(gDriveToken, multipartFile);
////                    System.out.println("SUCCESS: Dropbox → GDrive: " + fileName);
//                } catch (Exception e) {
////                    System.err.println("FAILED: Dropbox → GDrive: " + fileName);
//                    e.printStackTrace();
//                }
//            } else if (dFile == null && gFile != null) {
////                System.out.println("Attempting: GDrive → Dropbox");
//                try {
//                    ByteArrayOutputStream content = googleDriveService.downloadFile(gDriveToken, (String) gFile.get("id"));
//                    MultipartFile multipartFile = new InMemoryMultipartFile(fileName, content.toByteArray());
//                    dropboxService.uploadFileToSyncFolder(dropboxToken, multipartFile);
////                    System.out.println("SUCCESS: GDrive → Dropbox: " + fileName);
//                } catch (Exception e) {
////                    System.err.println("FAILED: GDrive → Dropbox: " + fileName);
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//
//    // -------------------- CONFLICT RESOLUTION --------------------
//    private void resolveConflictAndSync(String gDriveToken, String dropboxToken,
//                                        Map<String, Object> gFile, Map<String, Object> dFile) throws Exception {
//        String gId = (String) gFile.get("id");
//        String dId = (String) dFile.get("id");
//        String fileName = (String) gFile.get("name");
//
//        Instant gTime = parseToInstant((String) gFile.get("modifiedTime"));
//        Instant dTime = parseToInstant((String) dFile.get("modifiedTime"));
//
//        if (gTime.isAfter(dTime)) {
//            ByteArrayOutputStream content = googleDriveService.downloadFile(gDriveToken, gId);
//            MultipartFile multipartFile = new InMemoryMultipartFile(fileName, content.toByteArray());
//            dropboxService.uploadFileToSyncFolder(dropboxToken, multipartFile);
//            System.out.println("Conflict: GDrive newer → Dropbox updated");
//        } else if (dTime.isAfter(gTime)) {
//            ByteArrayOutputStream content = dropboxService.downloadFile(dropboxToken, dId);
//            MultipartFile multipartFile = new InMemoryMultipartFile(fileName, content.toByteArray());
//            googleDriveService.uploadFileToSyncFolder(gDriveToken, multipartFile);
//            System.out.println("Conflict: Dropbox newer → GDrive updated");
//        } else {
//            System.out.println("No sync needed: " + fileName);
//        }
//    }
//
//    // -------------------- UTILITY --------------------
//    private boolean isNewer(Map<String, Object> source, Map<String, Object> target) {
//        Instant sourceTime = parseToInstant((String) source.get("modifiedTime"));
//        Instant targetTime = parseToInstant((String) target.get("modifiedTime"));
//        return sourceTime.isAfter(targetTime);
//    }
//
//    /**
//     * Parse date strings from both Google Drive and Dropbox APIs
//     */
//    private Instant parseToInstant(String dateString) {
//        if (dateString == null || dateString.trim().isEmpty()) {
//            return Instant.now(); // fallback to current time
//        }
//
//        try {
//            // Try ISO format first (Google Drive format)
//            return Instant.parse(dateString);
//        } catch (Exception e1) {
//            try {
//                // Try RFC format (Dropbox format): "Sat Jun 07 12:05:34 IST 2025"
//                DateTimeFormatter rfc = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy");
//                ZonedDateTime zdt = ZonedDateTime.parse(dateString, rfc);
//                return zdt.toInstant();
//            } catch (Exception e2) {
//                try {
//                    // Try another common format: "2025-06-07T12:05:34.123Z"
//                    DateTimeFormatter iso = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
//                    LocalDateTime ldt = LocalDateTime.parse(dateString, iso);
//                    return ldt.atZone(ZoneId.of("UTC")).toInstant();
//                } catch (Exception e3) {
//                    System.err.println("Could not parse date: " + dateString);
//                    return Instant.now(); // fallback to current time
//                }
//            }
//        }
//    }
//
//
//
//
//}






package com.project.cloudsync.service;

import com.project.cloudsync.helper.InMemoryMultipartFile;
import com.project.cloudsync.service.DropboxService;
import com.project.cloudsync.service.GoogleDriveService;
import com.project.cloudsync.service.SyncTrackerService;
import com.project.cloudsync.entities.SyncFile;
import com.project.cloudsync.repositories.SyncFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CloudSyncService {

    private final GoogleDriveService googleDriveService;
    private final DropboxService dropboxService;
    private final SyncTrackerService syncTrackerService;
    private final SyncFileRepository syncFileRepository;

    public CloudSyncService(GoogleDriveService googleDriveService,
                            DropboxService dropboxService,
                            SyncTrackerService syncTrackerService,
                            SyncFileRepository syncFileRepository) {
        this.googleDriveService = googleDriveService;
        this.dropboxService = dropboxService;
        this.syncTrackerService = syncTrackerService;
        this.syncFileRepository = syncFileRepository;
    }


    public void bidirectionalSync(String gDriveToken, String dropboxToken) throws Exception {
        List<Map<String, Object>> gFiles = googleDriveService.listFilesInSyncFolder(gDriveToken);
        List<Map<String, Object>> dFiles = dropboxService.listFilesInSyncFolder(dropboxToken);

        // Get all tracked files from database
        List<SyncFile> trackedFiles = syncFileRepository.findAll();

        // Create maps for efficient lookups by platform IDs
        Map<String, SyncFile> trackedByGDriveId = trackedFiles.stream()
                .filter(sf -> sf.getGoogleDriveFileId() != null)
                .collect(Collectors.toMap(SyncFile::getGoogleDriveFileId, sf -> sf));

        Map<String, SyncFile> trackedByDropboxId = trackedFiles.stream()
                .filter(sf -> sf.getDropboxFileId() != null)
                .collect(Collectors.toMap(SyncFile::getDropboxFileId, sf -> sf));

        // Create maps for current files by platform IDs
        Map<String, Map<String, Object>> gFileMap = gFiles.stream()
                .collect(Collectors.toMap(f -> (String) f.get("id"), f -> f));

        Map<String, Map<String, Object>> dFileMap = dFiles.stream()
                .collect(Collectors.toMap(f -> (String) f.get("id"), f -> f));

        // Track files that were renamed in this sync cycle to avoid double processing
        Set<String> filesRenamedFromGDrive = new HashSet<>();
        Set<String> filesRenamedFromDropbox = new HashSet<>();

        // Process Google Drive files
        for (Map<String, Object> gFile : gFiles) {
            String gDriveFileId = (String) gFile.get("id");
            SyncFile tracked = trackedByGDriveId.get(gDriveFileId);

            if (tracked == null) {
                // New file in Google Drive
                handleNewGoogleDriveFile(gFile, dFileMap, gDriveToken, dropboxToken);
            } else {
                // File is tracked - check for updates/renames
                boolean wasRenamed = handleTrackedGoogleDriveFile(tracked, gFile, dFileMap, gDriveToken, dropboxToken);
                if (wasRenamed && tracked.getDropboxFileId() != null) {
                    filesRenamedFromGDrive.add(tracked.getDropboxFileId());
                }
            }
        }

        // Process Dropbox files
        for (Map<String, Object> dFile : dFiles) {
            String dropboxFileId = (String) dFile.get("id");
            SyncFile tracked = trackedByDropboxId.get(dropboxFileId);

            if (tracked == null) {
                // New file in Dropbox (might already be processed from GDrive side)
                handleNewDropboxFile(dFile, gFileMap, dropboxToken, gDriveToken);
            } else {
                // Skip if this file was already renamed from Google Drive in this cycle
                if (filesRenamedFromGDrive.contains(dropboxFileId)) {
                    System.out.println("Skipping " + dFile.get("name") + " - already renamed from Google Drive in this cycle");
                    continue;
                }

                // File is tracked - check for updates/renames
                boolean wasRenamed = handleTrackedDropboxFile(tracked, dFile, gFileMap, dropboxToken, gDriveToken);
                if (wasRenamed && tracked.getGoogleDriveFileId() != null) {
                    filesRenamedFromDropbox.add(tracked.getGoogleDriveFileId());
                }
            }
        }

        // Handle deleted files (files in database but not in current listings)
        handleDeletedFiles(trackedFiles, gFileMap, dFileMap, gDriveToken, dropboxToken);
    }

    // Modified method to return boolean indicating if rename occurred
    private boolean handleTrackedGoogleDriveFile(SyncFile tracked, Map<String, Object> gFile,
                                                 Map<String, Map<String, Object>> dFileMap,
                                                 String gDriveToken, String dropboxToken) {
        String gDriveFileId = (String) gFile.get("id");
        String currentName = (String) gFile.get("name");
        String currentHash = (String) gFile.get("md5Checksum");
        boolean wasRenamed = false;

        try {
            // Check if file was renamed
            if (!currentName.equals(tracked.getFileName())) {
                System.out.println("File renamed in Google Drive: " + tracked.getFileName() + " → " + currentName);

                // Update Dropbox file name if it exists
                if (tracked.getDropboxFileId() != null) {
                    Map<String, Object> dropboxFile = dFileMap.get(tracked.getDropboxFileId());
                    if (dropboxFile != null) {
                        renameDropboxFile(tracked.getDropboxFileId(), currentName, dropboxToken);
                        syncTrackerService.logHistory("RENAME", "GOOGLE_DRIVE", "DROPBOX",
                                currentName, "SUCCESS", "Renamed due to GDrive change");
                    }
                }

                // Update tracked file name
                tracked.setFileName(currentName);
                tracked.setUpdatedAt(LocalDateTime.now());
                syncFileRepository.save(tracked);
                wasRenamed = true;
            }

            // Check if content changed
            if (!Objects.equals(currentHash, tracked.getGoogleHash())) {
                System.out.println("Content changed in Google Drive: " + currentName);

                // Check if corresponding Dropbox file exists
                if (tracked.getDropboxFileId() != null) {
                    Map<String, Object> dropboxFile = dFileMap.get(tracked.getDropboxFileId());
                    if (dropboxFile != null) {
                        resolveContentConflict(tracked, gFile, dropboxFile, gDriveToken, dropboxToken);
                    } else {
                        // Dropbox file was deleted - upload new version
                        uploadFromGoogleDriveToDropbox(gFile, gDriveToken, dropboxToken);
                    }
                } else {
                    // No Dropbox counterpart - upload to Dropbox
                    uploadFromGoogleDriveToDropbox(gFile, gDriveToken, dropboxToken);
                }
            }

        } catch (Exception e) {
            syncTrackerService.logHistory("UPDATE", "GOOGLE_DRIVE", "DROPBOX",
                    currentName, "FAILED", e.getMessage());
            System.err.println("Failed to handle tracked Google Drive file " + currentName + ": " + e.getMessage());
        }

        return wasRenamed;
    }

    // Similarly modify handleTrackedDropboxFile to return boolean
    private boolean handleTrackedDropboxFile(SyncFile tracked, Map<String, Object> dFile,
                                             Map<String, Map<String, Object>> gFileMap,
                                             String dropboxToken, String gDriveToken) {
        String dropboxFileId = (String) dFile.get("id");
        String currentName = (String) dFile.get("name");
        String currentHash = (String) dFile.get("contentHash");
        boolean wasRenamed = false;

        try {
            // Check if file was renamed
            if (!currentName.equals(tracked.getFileName())) {
                System.out.println("File renamed in Dropbox: " + tracked.getFileName() + " → " + currentName);

                // Update Google Drive file name if it exists
                if (tracked.getGoogleDriveFileId() != null) {
                    Map<String, Object> gDriveFile = gFileMap.get(tracked.getGoogleDriveFileId());
                    if (gDriveFile != null) {
                        renameGoogleDriveFile(tracked.getGoogleDriveFileId(), currentName, gDriveToken);
                        syncTrackerService.logHistory("RENAME", "DROPBOX", "GOOGLE_DRIVE",
                                currentName, "SUCCESS", "Renamed due to Dropbox change");
                    }
                }

                // Update tracked file name
                tracked.setFileName(currentName);
                tracked.setUpdatedAt(LocalDateTime.now());
                syncFileRepository.save(tracked);
                wasRenamed = true;
            }

            // Check if content changed
            if (!Objects.equals(currentHash, tracked.getDropboxHash())) {
                System.out.println("Content changed in Dropbox: " + currentName);

                // Check if corresponding Google Drive file exists
                if (tracked.getGoogleDriveFileId() != null) {
                    Map<String, Object> gDriveFile = gFileMap.get(tracked.getGoogleDriveFileId());
                    if (gDriveFile != null) {
                        resolveContentConflict(tracked, gDriveFile, dFile, gDriveToken, dropboxToken);
                    } else {
                        // Google Drive file was deleted - upload new version
                        uploadFromDropboxToGoogleDrive(dFile, dropboxToken, gDriveToken);
                    }
                } else {
                    // No Google Drive counterpart - upload to Google Drive
                    uploadFromDropboxToGoogleDrive(dFile, dropboxToken, gDriveToken);
                }
            }

        } catch (Exception e) {
            syncTrackerService.logHistory("UPDATE", "DROPBOX", "GOOGLE_DRIVE",
                    currentName, "FAILED", e.getMessage());
            System.err.println("Failed to handle tracked Dropbox file " + currentName + ": " + e.getMessage());
        }

        return wasRenamed;
    }

    // -------------------- CONFLICT RESOLUTION --------------------
    private void resolveContentConflict(SyncFile tracked, Map<String, Object> gFile,
                                        Map<String, Object> dFile, String gDriveToken, String dropboxToken) {
        String fileName = (String) gFile.get("name");
        String gDriveHash = (String) gFile.get("md5Checksum");
        String dropboxHash = (String) dFile.get("contentHash");

        try {
            Instant gTime = parseToInstant((String) gFile.get("Modified time"));
            Instant dTime = parseToInstant((String) dFile.get("modifiedTime"));

            if (gTime.isAfter(dTime)) {
                // Google Drive file is newer - update Dropbox
                System.out.println("Resolving conflict: Google Drive newer → updating Dropbox");

                ByteArrayOutputStream content = googleDriveService.downloadFile(gDriveToken, (String) gFile.get("id"));
                MultipartFile multipartFile = new InMemoryMultipartFile(fileName, content.toByteArray());
                dropboxService.uploadFileToSyncFolder(dropboxToken, multipartFile);

                syncTrackerService.updateSyncFile(fileName, tracked.getDropboxFileId(),
                        tracked.getGoogleDriveFileId(), gDriveHash, gDriveHash, "SYNCED");

                syncTrackerService.logHistory("CONFLICT_RESOLUTION", "GOOGLE_DRIVE", "DROPBOX",
                        fileName, "SUCCESS", "GDrive newer - updated Dropbox");

            } else if (dTime.isAfter(gTime)) {
                // Dropbox file is newer - update Google Drive
                System.out.println("Resolving conflict: Dropbox newer → updating Google Drive");

                ByteArrayOutputStream content = dropboxService.downloadFile(dropboxToken, (String) dFile.get("id"));
                MultipartFile multipartFile = new InMemoryMultipartFile(fileName, content.toByteArray());
                googleDriveService.uploadFileToSyncFolder(gDriveToken, multipartFile);

                syncTrackerService.updateSyncFile(fileName, tracked.getDropboxFileId(),
                        tracked.getGoogleDriveFileId(), dropboxHash, dropboxHash, "SYNCED");

                syncTrackerService.logHistory("CONFLICT_RESOLUTION", "DROPBOX", "GOOGLE_DRIVE",
                        fileName, "SUCCESS", "Dropbox newer - updated GDrive");

            } else {
                // Same timestamp but different content - manual resolution needed
                syncTrackerService.updateSyncFile(fileName, tracked.getDropboxFileId(),
                        tracked.getGoogleDriveFileId(), dropboxHash, gDriveHash, "CONFLICT");

                syncTrackerService.logHistory("CONFLICT", "BOTH", "BOTH",
                        fileName, "PENDING", "Same timestamp, different content - manual resolution needed");

                System.out.println("Manual conflict resolution needed: " + fileName);
            }

        } catch (Exception e) {
            syncTrackerService.updateSyncFile(fileName, tracked.getDropboxFileId(),
                    tracked.getGoogleDriveFileId(), dropboxHash, gDriveHash, "FAILED");

            syncTrackerService.logHistory("CONFLICT_RESOLUTION", "BOTH", "BOTH",
                    fileName, "FAILED", e.getMessage());
            throw new RuntimeException("Failed to resolve conflict for " + fileName, e);
        }
    }

    // -------------------- UPLOAD OPERATIONS --------------------
    private void uploadFromGoogleDriveToDropbox(Map<String, Object> gFile, String gDriveToken, String dropboxToken) throws Exception {
        String fileName = (String) gFile.get("name");
        String gDriveFileId = (String) gFile.get("id");
        String gDriveHash = (String) gFile.get("md5Checksum");

        ByteArrayOutputStream content = googleDriveService.downloadFile(gDriveToken, gDriveFileId);
        MultipartFile multipartFile = new InMemoryMultipartFile(fileName, content.toByteArray());

        // Upload to Dropbox and get file metadata
        dropboxService.uploadFileToSyncFolder(dropboxToken, multipartFile);

        // Get the uploaded file metadata to get Dropbox ID and hash
        Map<String, Object> uploadedFile = dropboxService.getFileMetadata(dropboxToken, "/CloudSync/" + fileName);
        String dropboxFileId = (String) uploadedFile.get("id");
        String dropboxHash = (String) uploadedFile.get("contentHash");

        syncTrackerService.updateSyncFile(fileName, dropboxFileId, gDriveFileId,
                dropboxHash, gDriveHash, "SYNCED");

        syncTrackerService.logHistory("UPLOAD", "GOOGLE_DRIVE", "DROPBOX",
                fileName, "SUCCESS", null);

        System.out.println("Uploaded from Google Drive to Dropbox: " + fileName);
    }

    private void uploadFromDropboxToGoogleDrive(Map<String, Object> dFile, String dropboxToken, String gDriveToken) throws Exception {
        String fileName = (String) dFile.get("name");
        String dropboxFileId = (String) dFile.get("id");
        String dropboxHash = (String) dFile.get("contentHash");

        ByteArrayOutputStream content = dropboxService.downloadFile(dropboxToken, dropboxFileId);
        MultipartFile multipartFile = new InMemoryMultipartFile(fileName, content.toByteArray());

        // Upload to Google Drive and get file metadata
        googleDriveService.uploadFileToSyncFolder(gDriveToken, multipartFile);

        // Get the uploaded file metadata to get Google Drive ID and hash
        Map<String, String> syncFolder = googleDriveService.findSyncFolder(gDriveToken);
        String query = String.format("name='%s' and '%s' in parents and trashed = false", fileName, syncFolder.get("id"));

        // You'll need to add this method to GoogleDriveService or modify existing one
        Map<String, Object> uploadedFile = googleDriveService.findFileByQuery(gDriveToken, query);
        String gDriveFileId = (String) uploadedFile.get("id");
        String gDriveHash = (String) uploadedFile.get("md5Checksum");

        syncTrackerService.updateSyncFile(fileName, dropboxFileId, gDriveFileId,
                dropboxHash, gDriveHash, "SYNCED");

        syncTrackerService.logHistory("UPLOAD", "DROPBOX", "GOOGLE_DRIVE",
                fileName, "SUCCESS", null);

        System.out.println("Uploaded from Dropbox to Google Drive: " + fileName);
    }

    // -------------------- RENAME OPERATIONS --------------------
    private void renameGoogleDriveFile(String fileId, String newName, String gDriveToken) throws Exception {
        // You'll need to implement this method in GoogleDriveService
        googleDriveService.renameFile(gDriveToken, fileId, newName);
    }

    private void renameDropboxFile(String fileId, String newName, String dropboxToken) throws Exception {
        // You'll need to implement this method in DropboxService
        dropboxService.renameFileOrFolder(dropboxToken, fileId, newName);
    }

    private void handleDeletedFiles(List<SyncFile> trackedFiles,
                                    Map<String, Map<String, Object>> gFileMap,
                                    Map<String, Map<String, Object>> dFileMap,
                                    String gDriveToken, String dropboxToken) {
        for (SyncFile tracked : trackedFiles) {
            boolean gDriveExists = tracked.getGoogleDriveFileId() != null &&
                    gFileMap.containsKey(tracked.getGoogleDriveFileId());
            boolean dropboxExists = tracked.getDropboxFileId() != null &&
                    dFileMap.containsKey(tracked.getDropboxFileId());

            if (!gDriveExists && !dropboxExists) {
                // File deleted from both platforms - remove from tracking
                syncFileRepository.delete(tracked);
                syncTrackerService.logHistory("DELETE", "BOTH", "BOTH",
                        tracked.getFileName(), "SUCCESS", "File deleted from both platforms");

            } else if (!gDriveExists && dropboxExists) {
                // File deleted from Google Drive - delete from Dropbox
                try {
                    dropboxService.deleteFile(dropboxToken, tracked.getDropboxFileId());
                    syncFileRepository.delete(tracked);
                    syncTrackerService.logHistory("DELETE", "GOOGLE_DRIVE", "DROPBOX",
                            tracked.getFileName(), "SUCCESS", "Deleted from Dropbox due to GDrive deletion");
                } catch (Exception e) {
                    syncTrackerService.logHistory("DELETE", "GOOGLE_DRIVE", "DROPBOX",
                            tracked.getFileName(), "FAILED", e.getMessage());
                }

            } else if (gDriveExists && !dropboxExists) {
                // File deleted from Dropbox - delete from Google Drive
                try {
                    googleDriveService.deleteFile(gDriveToken, tracked.getGoogleDriveFileId());
                    syncFileRepository.delete(tracked);
                    syncTrackerService.logHistory("DELETE", "DROPBOX", "GOOGLE_DRIVE",
                            tracked.getFileName(), "SUCCESS", "Deleted from GDrive due to Dropbox deletion");
                } catch (Exception e) {
                    syncTrackerService.logHistory("DELETE", "DROPBOX", "GOOGLE_DRIVE",
                            tracked.getFileName(), "FAILED", e.getMessage());
                }
            }
        }
    }

    // -------------------- UTILITY METHODS --------------------
    private Instant parseToInstant(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return Instant.now();
        }

        try {
            return Instant.parse(dateString);
        } catch (Exception e1) {
            try {
                DateTimeFormatter rfc = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy");
                ZonedDateTime zdt = ZonedDateTime.parse(dateString, rfc);
                return zdt.toInstant();
            } catch (Exception e2) {
                try {
                    DateTimeFormatter iso = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    LocalDateTime ldt = LocalDateTime.parse(dateString, iso);
                    return ldt.atZone(ZoneId.of("UTC")).toInstant();
                } catch (Exception e3) {
                    System.err.println("Could not parse date: " + dateString);
                    return Instant.now();
                }
            }
        }
    }

    // -------------------- ONE-WAY SYNC METHODS (Updated) --------------------
    public void oneWaySyncDropboxToGDrive(String dropboxToken, String gDriveToken) throws Exception {
        List<Map<String, Object>> dropboxFiles = dropboxService.listFilesInSyncFolder(dropboxToken);

        for (Map<String, Object> dFile : dropboxFiles) {
            String fileName = (String) dFile.get("name");
            String dropboxFileId = (String) dFile.get("id");
            String dropboxHash = (String) dFile.get("contentHash");

            // Check if file is already tracked
            Optional<SyncFile> tracked = syncFileRepository.findByDropboxFileId(dropboxFileId);

            if (tracked.isEmpty()) {
                // New file - upload to Google Drive
                uploadFromDropboxToGoogleDrive(dFile, dropboxToken, gDriveToken);
            } else {
                // Check if content changed
                if (!Objects.equals(dropboxHash, tracked.get().getDropboxHash())) {
                    // Content changed - update Google Drive
                    uploadFromDropboxToGoogleDrive(dFile, dropboxToken, gDriveToken);
                }
            }
        }
    }

    public void oneWaySyncGDriveToDropbox(String gDriveToken, String dropboxToken) throws Exception {
        List<Map<String, Object>> gDriveFiles = googleDriveService.listFilesInSyncFolder(gDriveToken);

        for (Map<String, Object> gFile : gDriveFiles) {
            String fileName = (String) gFile.get("name");
            String gDriveFileId = (String) gFile.get("id");
            String gDriveHash = (String) gFile.get("md5Checksum");

            // Check if file is already tracked
            Optional<SyncFile> tracked = syncFileRepository.findByGoogleDriveFileId(gDriveFileId);

            if (tracked.isEmpty()) {
                // New file - upload to Dropbox
                uploadFromGoogleDriveToDropbox(gFile, gDriveToken, dropboxToken);
            } else {
                // Check if content changed
                if (!Objects.equals(gDriveHash, tracked.get().getGoogleHash())) {
                    // Content changed - update Dropbox
                    uploadFromGoogleDriveToDropbox(gFile, gDriveToken, dropboxToken);
                }
            }
        }
    }


    private Map<String, Object> findDropboxFileByNameAndSize(String fileName, Long fileSize,
                                                             Map<String, Map<String, Object>> dFileMap) {
        for (Map<String, Object> dFile : dFileMap.values()) {
            String dFileName = (String) dFile.get("name");
            Long dFileSize = (Long) dFile.get("size");

            if (fileName != null && dFileName != null
                    && fileName.equalsIgnoreCase(dFileName)
                    && Objects.equals(fileSize, dFileSize)) {
                return dFile;
            }
        }
        return null;
    }

    private Map<String, Object> findGoogleFileByNameAndSize(String fileName, Long fileSize,
                                                            Map<String, Map<String, Object>> gFileMap) {
        for (Map<String, Object> gFile : gFileMap.values()) {
            String gFileName = (String) gFile.get("name");
            Long gFileSize = (Long) gFile.get("size");

            if (fileName != null && gFileName != null
                    && fileName.equalsIgnoreCase(gFileName)
                    && Objects.equals(fileSize, gFileSize)) {
                return gFile;
            }
        }
        return null;
    }

    // -------------------- UPDATED HANDLE NEW FILE METHODS --------------------
    private void handleNewGoogleDriveFile(Map<String, Object> gFile,
                                          Map<String, Map<String, Object>> dFileMap,
                                          String gDriveToken, String dropboxToken) {
        String gDriveFileId = (String) gFile.get("id");
        String fileName = (String) gFile.get("name");
        String gDriveHash = (String) gFile.get("md5Checksum");
        Long fileSize = (Long) gFile.get("size");

        try {
            // First check if this file is already tracked
            Optional<SyncFile> existingTracked = syncFileRepository.findByGoogleDriveFileId(gDriveFileId);
            if (existingTracked.isPresent()) {
                SyncFile syncFile = existingTracked.get();
                String trackedName = syncFile.getFileName();

                if (!trackedName.equals(fileName)) {
                    // Rename happened in Google Drive, reflect in Dropbox
                    dropboxService.renameFileOrFolder(dropboxToken, syncFile.getDropboxFileId(), fileName);
                    syncFile.setFileName(fileName); // update name in DB
                    syncFileRepository.save(syncFile);
                    syncTrackerService.logHistory("RENAME", "GOOGLE_DRIVE", "DROPBOX", fileName, "SUCCESS", "Renamed Dropbox to match GDrive");
                return;
                }

                return; // Skip further sync, already tracked
            }

            // Check if file with same name and size already exists in Dropbox
            Map<String, Object> matchingDropboxFile = findDropboxFileByNameAndSize(fileName, fileSize, dFileMap);

            if (matchingDropboxFile != null) {
                // Check if the Dropbox file is already tracked
                String dropboxFileId = (String) matchingDropboxFile.get("id");
                Optional<SyncFile> dropboxTracked = syncFileRepository.findByDropboxFileId(dropboxFileId);

                if (dropboxTracked.isEmpty()) {
                    // File exists in both platforms but not tracked - create tracking entry
                    String dropboxHash = (String) matchingDropboxFile.get("contentHash");

                    syncTrackerService.updateSyncFile(fileName, dropboxFileId, gDriveFileId,
                            dropboxHash, gDriveHash, "SYNCED");

                    syncTrackerService.logHistory("LINK", "GOOGLE_DRIVE", "DROPBOX",
                            fileName, "SUCCESS", "Linked existing files with same name and size");

                    System.out.println("Linked existing files: " + fileName);
                }
            } else {
                // New file - upload to Dropbox
                uploadFromGoogleDriveToDropbox(gFile, gDriveToken, dropboxToken);
            }

        } catch (Exception e) {
            syncTrackerService.logHistory("UPLOAD", "GOOGLE_DRIVE", "DROPBOX",
                    fileName, "FAILED", e.getMessage());
            System.err.println("Failed to handle new Google Drive file " + fileName + ": " + e.getMessage());
        }
    }

    private void handleNewDropboxFile(Map<String, Object> dFile,
                                      Map<String, Map<String, Object>> gFileMap,
                                      String dropboxToken, String gDriveToken) {
        String dropboxFileId = (String) dFile.get("id");
        String fileName = (String) dFile.get("name");
        String dropboxHash = (String) dFile.get("contentHash");
        Long fileSize = (Long) dFile.get("size");

        try {
            // First check if this file is already tracked
            Optional<SyncFile> existingTracked = syncFileRepository.findByDropboxFileId(dropboxFileId);
            if (existingTracked.isPresent()) {
                SyncFile syncFile = existingTracked.get();
                String trackedName = syncFile.getFileName();

                if (!trackedName.equals(fileName)) {
                    // Rename happened in Dropbox, reflect in GDrive
                    googleDriveService.renameFile(gDriveToken, syncFile.getGoogleDriveFileId(), fileName);
                    syncFile.setFileName(fileName);
                    syncFileRepository.save(syncFile);
                    syncTrackerService.logHistory("RENAME", "DROPBOX", "GOOGLE_DRIVE", fileName, "SUCCESS", "Renamed GDrive to match Dropbox");
                }

                return;
            }


            // Check if file with same name and size already exists in Google Drive
            Map<String, Object> matchingGoogleFile = findGoogleFileByNameAndSize(fileName, fileSize, gFileMap);

            if (matchingGoogleFile != null) {
                // Check if the Google Drive file is already tracked
                String gDriveFileId = (String) matchingGoogleFile.get("id");
                Optional<SyncFile> gDriveTracked = syncFileRepository.findByGoogleDriveFileId(gDriveFileId);

                if (gDriveTracked.isEmpty()) {
                    // File exists in both platforms but not tracked - create tracking entry
                    String gDriveHash = (String) matchingGoogleFile.get("md5Checksum");

                    syncTrackerService.updateSyncFile(fileName, dropboxFileId, gDriveFileId,
                            dropboxHash, gDriveHash, "SYNCED");

                    syncTrackerService.logHistory("LINK", "DROPBOX", "GOOGLE_DRIVE",
                            fileName, "SUCCESS", "Linked existing files with same name and size");

                    System.out.println("Linked existing files: " + fileName);
                }
            } else {
                // New file - upload to Google Drive
                uploadFromDropboxToGoogleDrive(dFile, dropboxToken, gDriveToken);
            }

        } catch (Exception e) {
            syncTrackerService.logHistory("UPLOAD", "DROPBOX", "GOOGLE_DRIVE",
                    fileName, "FAILED", e.getMessage());
            System.err.println("Failed to handle new Dropbox file " + fileName + ": " + e.getMessage());
        }
    }

}



