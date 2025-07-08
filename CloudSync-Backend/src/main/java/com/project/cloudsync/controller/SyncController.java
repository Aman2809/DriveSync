package com.project.cloudsync.controller;

import com.project.cloudsync.service.CloudSyncService;
import com.project.cloudsync.service.GoogleDriveService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
public class SyncController {


    private final CloudSyncService syncService;
    private final GoogleDriveService googleDriveService;

    public SyncController(CloudSyncService syncService, GoogleDriveService googleDriveService) {
        this.syncService = syncService;
        this.googleDriveService=googleDriveService;
    }

    @PostMapping("/oneway/dropbox-to-gdrive")
    public ResponseEntity<String> syncDropboxToGDrive(@RequestParam String dropboxToken,
                                                      @RequestParam String gdriveToken) {
        try {
            syncService.oneWaySyncDropboxToGDrive(dropboxToken, gdriveToken);
            return ResponseEntity.ok("Sync from Dropbox to Google Drive completed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error during sync: " + e.getMessage());
        }
    }

    @PostMapping("/oneway/gdrive-to-dropbox")
    public ResponseEntity<String> syncGDriveToDropbox(@RequestParam String gdriveToken,
                                                      @RequestParam String dropboxToken) {
        try {
            syncService.oneWaySyncGDriveToDropbox(gdriveToken, dropboxToken);
            return ResponseEntity.ok("Sync from Google Drive to Dropbox completed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error during sync: " + e.getMessage());
        }
    }

    @PostMapping("/bidirectional")
    public ResponseEntity<String> syncBidirectional(@RequestParam String gdriveToken,
                                                    @RequestParam String dropboxToken) {
        try {
            syncService.bidirectionalSync(gdriveToken, dropboxToken);
            return ResponseEntity.ok("Bidirectional sync completed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error during bidirectional sync: " + e.getMessage());
        }
    }
}

