package com.project.cloudsync.repositories;

import com.project.cloudsync.entities.SyncFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SyncFileRepository extends JpaRepository<SyncFile, Long> {
    Optional<SyncFile> findByFileName(String fileName);
    Optional<SyncFile> findByGoogleDriveFileId(String googleDriveFileId);
    Optional<SyncFile> findByDropboxFileId(String dropboxFileId);

}
