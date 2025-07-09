package com.project.cloudsync.service;

import com.project.cloudsync.entities.SyncFile;
import com.project.cloudsync.entities.SyncHistory;
import com.project.cloudsync.entities.User;
import com.project.cloudsync.repositories.SyncFileRepository;
import com.project.cloudsync.repositories.SyncHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SyncTrackerService {

    private final SyncFileRepository syncFileRepository;
    private final SyncHistoryRepository syncHistoryRepository;

    public SyncTrackerService(SyncFileRepository syncFileRepository, SyncHistoryRepository syncHistoryRepository) {
        this.syncFileRepository = syncFileRepository;
        this.syncHistoryRepository = syncHistoryRepository;
    }

    // Updated method with User parameter
    public void updateSyncFile(String fileName, String dropboxId, String gdriveId,
                               String dropboxHash, String gdriveHash, String status, User user) {
        Optional<SyncFile> optional = Optional.empty();

        if (gdriveId != null) {
            optional = syncFileRepository.findByGoogleDriveFileId(gdriveId);
        } else if (dropboxId != null) {
            optional = syncFileRepository.findByDropboxFileId(dropboxId);
        }

        SyncFile file = optional.orElseGet(SyncFile::new);

        file.setFileName(fileName);
        file.setDropboxFileId(dropboxId);
        file.setGoogleDriveFileId(gdriveId);
        file.setDropboxHash(dropboxHash);
        file.setGoogleHash(gdriveHash);
        file.setSyncStatus(status);
        file.setLastSyncTime(LocalDateTime.now());
        file.setUpdatedAt(LocalDateTime.now());

        // Set the user - this was missing!
        file.setUser(user);

        syncFileRepository.save(file);
    }

    // Keep the old method for backward compatibility (temporarily)
    @Deprecated
    public void updateSyncFile(String fileName, String dropboxId, String gdriveId,
                               String dropboxHash, String gdriveHash, String status) {
        // This method should not be used anymore, but kept for backward compatibility
        // Log a warning or throw an exception
        System.err.println("WARNING: updateSyncFile called without User parameter. User will be null!");

        Optional<SyncFile> optional = Optional.empty();

        if (gdriveId != null) {
            optional = syncFileRepository.findByGoogleDriveFileId(gdriveId);
        } else if (dropboxId != null) {
            optional = syncFileRepository.findByDropboxFileId(dropboxId);
        }

        SyncFile file = optional.orElseGet(SyncFile::new);

        file.setFileName(fileName);
        file.setDropboxFileId(dropboxId);
        file.setGoogleDriveFileId(gdriveId);
        file.setDropboxHash(dropboxHash);
        file.setGoogleHash(gdriveHash);
        file.setSyncStatus(status);
        file.setLastSyncTime(LocalDateTime.now());
        file.setUpdatedAt(LocalDateTime.now());
        // user remains null here

        syncFileRepository.save(file);
    }

    public void logHistory(String operationType, String src, String dest, String fileName, String status, String errorMessage) {
        SyncHistory history = SyncHistory.builder()
                .operationType(operationType)
                .sourcePlatform(src)
                .destinationPlatform(dest)
                .fileName(fileName)
                .status(status)
                .errorMessage(errorMessage)
                .createdAt(LocalDateTime.now())
                .build();
        syncHistoryRepository.save(history);
    }

    // New method to log history with user information
    public void logHistory(String operationType, String src, String dest, String fileName,
                           String status, String errorMessage, User user) {
        SyncHistory history = SyncHistory.builder()
                .operationType(operationType)
                .sourcePlatform(src)
                .destinationPlatform(dest)
                .fileName(fileName)
                .status(status)
                .errorMessage(errorMessage)
                .createdAt(LocalDateTime.now())
                // You might want to add user to SyncHistory entity too
                .build();
        syncHistoryRepository.save(history);
    }
}