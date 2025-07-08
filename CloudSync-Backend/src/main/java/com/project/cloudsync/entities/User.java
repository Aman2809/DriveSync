package com.project.cloudsync.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name="users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email; // from Google/Dropbox profile

    private String name;  // optional

    // Google Drive tokens
    private String googleAccessToken;
    private String googleRefreshToken;

    // Dropbox tokens
    private String dropboxAccessToken;
    private String dropboxRefreshToken;

    private String provider; // "GOOGLE" / "DROPBOX"

    // For auditing
    private LocalDateTime registeredAt;
    private LocalDateTime lastSyncedAt;

    // Constructors, getters, setters...
}

