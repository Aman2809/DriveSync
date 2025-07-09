package com.project.cloudsync.service;

import com.project.cloudsync.entities.User;
import com.project.cloudsync.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Find user by Google Drive access token
     */
    public Optional<User> findByGoogleAccessToken(String googleAccessToken) {
        return userRepository.findByGoogleAccessToken(googleAccessToken);
    }

    /**
     * Find user by Dropbox access token
     */
    public Optional<User> findByDropboxAccessToken(String dropboxAccessToken) {
        return userRepository.findByDropboxAccessToken(dropboxAccessToken);
    }

    /**
     * Find user by both tokens - useful for bidirectional sync
     */
    public Optional<User> findByTokens(String gdriveToken, String dropboxToken) {
        // First try to find by Google Drive token
        Optional<User> userByGDrive = findByGoogleAccessToken(gdriveToken);
        if (userByGDrive.isPresent()) {
            User user = userByGDrive.get();
            // Verify this user also has the dropbox token
            if (dropboxToken.equals(user.getDropboxAccessToken())) {
                return userByGDrive;
            }
        }

        // Try to find by Dropbox token
        Optional<User> userByDropbox = findByDropboxAccessToken(dropboxToken);
        if (userByDropbox.isPresent()) {
            User user = userByDropbox.get();
            // Verify this user also has the gdrive token
            if (gdriveToken.equals(user.getGoogleAccessToken())) {
                return userByDropbox;
            }
        }

        return Optional.empty();
    }

    /**
     * Save user
     */
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Update last synced time
     */
    public void updateLastSyncedTime(User user) {
        user.setLastSyncedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }
}