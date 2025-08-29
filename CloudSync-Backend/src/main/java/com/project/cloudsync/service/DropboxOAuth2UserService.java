package com.project.cloudsync.service;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.users.FullAccount;
import com.project.cloudsync.entities.User;
import com.project.cloudsync.repositories.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class DropboxOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public DropboxOAuth2UserService(UserRepository userRepository,
                                    OAuth2AuthorizedClientService authorizedClientService) {
        this.userRepository = userRepository;
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        System.out.println("=== DropboxOAuth2UserService.loadUser() called ===");

        String accessToken = userRequest.getAccessToken().getTokenValue();
        System.out.println("Dropbox access token: " + accessToken);

        // Debug token details
        System.out.println("Dropbox token scopes: " + userRequest.getAccessToken().getScopes());
        System.out.println("Dropbox token type: " + userRequest.getAccessToken().getTokenType().getValue());

        DbxRequestConfig config = DbxRequestConfig.newBuilder("cloudsync").build();

        try {
            DbxClientV2 client = new DbxClientV2(config, accessToken);
            FullAccount account = client.users().getCurrentAccount();

            System.out.println("Dropbox user: " + account.getEmail());
            System.out.println("Dropbox account ID: " + account.getAccountId());

            // Save access token, refresh token will be saved later in event
            saveOrUpdateUser(account, accessToken, null);

            // Build Spring Security OAuth2User
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("id", account.getAccountId());
            attributes.put("email", account.getEmail());
            attributes.put("name", account.getName().getDisplayName());

            System.out.println("Dropbox user processed successfully");

            return new DefaultOAuth2User(
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                    attributes,
                    "email"
            );

        } catch (DbxException e) {
            System.err.println("Dropbox error: " + e.getMessage());
            OAuth2Error oauth2Error = new OAuth2Error(
                    "dropbox_error",
                    "Failed to fetch Dropbox user info: " + e.getMessage(),
                    null
            );
            throw new OAuth2AuthenticationException(oauth2Error, e);
        }
    }

    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        System.out.println("=== DropboxOAuth2UserService.handleAuthenticationSuccess() called ===");
        System.out.println("Authentication type: " + event.getAuthentication().getClass().getSimpleName());

        // Handle both OAuth2AuthenticationToken and OAuth2LoginAuthenticationToken
        if (event.getAuthentication() instanceof OAuth2AuthenticationToken auth) {
            processDropboxAuth(auth);
        } else if (event.getAuthentication().getClass().getSimpleName().equals("OAuth2LoginAuthenticationToken")) {
            // Cast to the login authentication token type
            try {
                var loginAuth = (org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken) event.getAuthentication();
                String registrationId = loginAuth.getClientRegistration().getRegistrationId();

                if ("dropbox".equals(registrationId)) {
                    String userEmail = loginAuth.getPrincipal().getAttribute("email");
                    String userName = loginAuth.getName();

                    System.out.println("Processing Dropbox LoginAuthenticationToken for user: " + userEmail);
                    tryMultipleRefreshTokenApproaches(userEmail, userName, null);
                }
            } catch (Exception e) {
                System.err.println("Error processing Dropbox OAuth2LoginAuthenticationToken: " + e.getMessage());
            }
        } else {
            System.out.println("Unhandled authentication type: " + event.getAuthentication().getClass());
        }
    }

    private void processDropboxAuth(OAuth2AuthenticationToken auth) {
        String registrationId = auth.getAuthorizedClientRegistrationId();
        System.out.println("Registration ID: " + registrationId);

        if ("dropbox".equals(registrationId)) {
            String userEmail = auth.getPrincipal().getAttribute("email");
            String userName = auth.getName();

            System.out.println("Processing refresh token for Dropbox user: " + userEmail);
            System.out.println("Authentication name: " + userName);

            tryMultipleRefreshTokenApproaches(userEmail, userName, auth);
        }
    }

    private void tryMultipleRefreshTokenApproaches(String userEmail, String userName, OAuth2AuthenticationToken auth) {
        // Approach 1: Immediate check
        checkRefreshTokenImmediate(userEmail, userName);

        // Approach 2: Delayed check with different delays
        CompletableFuture.runAsync(() -> {
            for (int delay : new int[]{500, 1000, 2000, 5000}) {
                try {
                    Thread.sleep(delay);
                    System.out.println("=== Checking Dropbox refresh token after " + delay + "ms delay ===");

                    OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("dropbox", userName);
                    logClientDetails(client, delay);

                    if (client != null && client.getRefreshToken() != null) {
                        String refreshToken = client.getRefreshToken().getTokenValue();
                        updateUserRefreshToken(userEmail, refreshToken, "dropbox");
                        System.out.println("✅ SUCCESS: Dropbox refresh token found and updated after " + delay + "ms");
                        return; // Exit early on success
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            System.out.println("❌ FAILED: No Dropbox refresh token found after all attempts");

            // Final attempt: Check all authorized clients
            checkAllAuthorizedClients(userEmail);
        });
    }

    private void checkRefreshTokenImmediate(String userEmail, String userName) {
        System.out.println("=== Immediate Dropbox refresh token check ===");
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("dropbox", userName);
        logClientDetails(client, 0);
    }

    private void logClientDetails(OAuth2AuthorizedClient client, int delay) {
        System.out.println("--- Dropbox OAuth2AuthorizedClient Details (delay: " + delay + "ms) ---");
        if (client == null) {
            System.out.println("❌ Dropbox client is NULL");
            return;
        }

        System.out.println("✓ Dropbox client found");
        System.out.println("Registration ID: " + client.getClientRegistration().getRegistrationId());
        System.out.println("Principal name: " + client.getPrincipalName());

        if (client.getAccessToken() != null) {
            System.out.println("✓ Dropbox access token exists");
            System.out.println("Access token scopes: " + client.getAccessToken().getScopes());
            System.out.println("Access token expires at: " + client.getAccessToken().getExpiresAt());
        } else {
            System.out.println("❌ Dropbox access token is NULL");
        }

        if (client.getRefreshToken() != null) {
            System.out.println("✅ DROPBOX REFRESH TOKEN EXISTS!");
            System.out.println("Refresh token value: " + client.getRefreshToken().getTokenValue());
        } else {
            System.out.println("❌ Dropbox refresh token is NULL");
        }
        System.out.println("--------------------------------------------------------------");
    }

    private void checkAllAuthorizedClients(String userEmail) {
        System.out.println("=== Checking all possible Dropbox authorized clients ===");

        // Try different principal name variations
        String[] principalNames = {
                userEmail,
                userEmail.toLowerCase(),
                userEmail.substring(0, userEmail.indexOf('@'))  // username part
        };

        for (String principalName : principalNames) {
            System.out.println("Trying Dropbox principal name: " + principalName);
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("dropbox", principalName);

            if (client != null) {
                System.out.println("✓ Found Dropbox client with principal: " + principalName);
                logClientDetails(client, -1);

                if (client.getRefreshToken() != null) {
                    String refreshToken = client.getRefreshToken().getTokenValue();
                    updateUserRefreshToken(userEmail, refreshToken, "dropbox");
                    System.out.println("✅ SUCCESS: Found Dropbox refresh token with alternative principal name");
                    return;
                }
            } else {
                System.out.println("❌ No Dropbox client found with principal: " + principalName);
            }
        }
    }

    private void saveOrUpdateUser(FullAccount account, String accessToken, String refreshToken) {
        String email = account.getEmail();
        String name = account.getName().getDisplayName();

        System.out.println("Saving/updating Dropbox user: " + email);

        Optional<User> optionalUser = userRepository.findByEmail(email);
        User user;

        if (optionalUser.isPresent()) {
            user = optionalUser.get();
            System.out.println("Existing user found for Dropbox");
        } else {
            user = new User();
            user.setEmail(email);
            System.out.println("Creating new user for Dropbox");
        }

        user.setName(name);
        user.setDropboxAccessToken(accessToken);

        if (refreshToken != null) {
            user.setDropboxRefreshToken(refreshToken);
            System.out.println("✅ Dropbox refresh token saved to database");
        }

        // Provider logic
        if (user.getProvider() != null && user.getProvider().contains("GOOGLE")) {
            user.setProvider("BOTH");
        } else {
            user.setProvider("DROPBOX");
        }

        userRepository.save(user);
        System.out.println("Dropbox user saved with provider: " + user.getProvider());
    }

    private void updateUserRefreshToken(String email, String refreshToken, String provider) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if ("dropbox".equals(provider)) {
                user.setDropboxRefreshToken(refreshToken);
            }

            // Provider logic
            if (user.getProvider() != null && user.getProvider().contains("GOOGLE")) {
                user.setProvider("BOTH");
            } else {
                user.setProvider("DROPBOX");
            }

            userRepository.save(user);
            System.out.println("✅ Dropbox refresh token updated for user: " + email);
        } else {
            System.out.println("❌ User not found for Dropbox refresh token update: " + email);
        }
    }
}