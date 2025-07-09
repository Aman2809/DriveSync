package com.project.cloudsync.controller;

import com.project.cloudsync.entities.User;
import com.project.cloudsync.service.CloudSyncService;
import com.project.cloudsync.service.GoogleDriveService;
import com.project.cloudsync.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final CloudSyncService syncService;
    private final GoogleDriveService googleDriveService;
    private final UserService userService;

    public SyncController(CloudSyncService syncService, GoogleDriveService googleDriveService, UserService userService) {
        this.syncService = syncService;
        this.googleDriveService = googleDriveService;
        this.userService = userService;
    }

    @PostMapping("/oneway/dropbox-to-gdrive")
    public ResponseEntity<String> syncDropboxToGDrive(@RequestParam String dropboxToken,
                                                      @RequestParam String gdriveToken) {
        try {
            // Find user by tokens
            Optional<User> userOpt = userService.findByTokens(gdriveToken, dropboxToken);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body("Invalid tokens - user not found");
            }

            User user = userOpt.get();
            syncService.oneWaySyncDropboxToGDrive(dropboxToken, gdriveToken, user);

            // Update last synced time
            userService.updateLastSyncedTime(user);

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
            // Find user by tokens
            Optional<User> userOpt = userService.findByTokens(gdriveToken, dropboxToken);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body("Invalid tokens - user not found");
            }

            User user = userOpt.get();
            syncService.oneWaySyncGDriveToDropbox(gdriveToken, dropboxToken, user);

            // Update last synced time
            userService.updateLastSyncedTime(user);

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
            // Find user by tokens
            Optional<User> userOpt = userService.findByTokens(gdriveToken, dropboxToken);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body("Invalid tokens - user not found");
            }

            User user = userOpt.get();
            syncService.bidirectionalSync(gdriveToken, dropboxToken, user);

            // Update last synced time
            userService.updateLastSyncedTime(user);

            return ResponseEntity.ok("Bidirectional sync completed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error during bidirectional sync: " + e.getMessage());
        }
    }
}