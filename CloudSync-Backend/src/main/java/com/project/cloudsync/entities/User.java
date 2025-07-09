package com.project.cloudsync.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email; // from Google/Dropbox profile

    private String name;  // optional

    // Google Drive tokens
    @Column(columnDefinition = "TEXT")
    private String googleAccessToken;

    @Column(columnDefinition = "TEXT")
    private String googleRefreshToken;

    // Dropbox tokens
    @Column(columnDefinition = "TEXT")
    private String dropboxAccessToken;

    @Column(columnDefinition = "TEXT")
    private String dropboxRefreshToken;

    private String provider; // "GOOGLE" / "DROPBOX" / "BOTH"

    // For auditing
    private LocalDateTime registeredAt;
    private LocalDateTime lastSyncedAt;

    @PrePersist
    protected void onCreate() {
        if (registeredAt == null) {
            registeredAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastSyncedAt = LocalDateTime.now();
    }
}