package com.project.cloudsync.service;




import com.project.cloudsync.entities.SyncFile;
import com.project.cloudsync.entities.SyncHistory;
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

    public void updateSyncFile(String fileName, String dropboxId, String gdriveId, String dropboxHash, String gdriveHash, String status) {
//        Optional<SyncFile> optional = syncFileRepository.findByFileName(fileName);
//        SyncFile file = optional.orElseGet(SyncFile::new);

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
}

