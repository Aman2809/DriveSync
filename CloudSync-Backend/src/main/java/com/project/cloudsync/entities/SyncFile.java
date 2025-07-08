package com.project.cloudsync.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    @Column(unique = true)
    private String dropboxFileId;

    @Column(unique = true)
    private String googleDriveFileId;

    private String dropboxHash;
    private String googleHash;

    private LocalDateTime lastSyncTime;

    private String syncStatus; // SYNCED, PENDING, CONFLICT

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;




}
