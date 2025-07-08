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
public class SyncHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String operationType; // UPLOAD, DOWNLOAD, DELETE, UPDATE
    private String sourcePlatform;
    private String destinationPlatform;
    private String fileName;
    private String status; // SUCCESS, FAILED, PENDING

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime createdAt = LocalDateTime.now();
}
