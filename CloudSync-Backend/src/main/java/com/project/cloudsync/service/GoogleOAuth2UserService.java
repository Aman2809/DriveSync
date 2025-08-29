package com.project.cloudsync.service;

import com.project.cloudsync.entities.User;
import com.project.cloudsync.repositories.UserRepository;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.context.event.EventListener;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class GoogleOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public GoogleOAuth2UserService(UserRepository userRepository,
                                   OAuth2AuthorizedClientService authorizedClientService) {
        this.userRepository = userRepository;
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        System.out.println("=== GoogleOAuth2UserService.loadUser() called ===");

        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oauthUser = delegate.loadUser(userRequest);

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String accessToken = userRequest.getAccessToken().getTokenValue();

        System.out.println("Google user email: " + email);
        System.out.println("Google access token: " + accessToken);

        // Debug token details
        System.out.println("Google token scopes: " + userRequest.getAccessToken().getScopes());
        System.out.println("Google token type: " + userRequest.getAccessToken().getTokenType().getValue());

        // Save user with access token initially
        saveOrUpdateUser(email, name, accessToken, null);

        System.out.println("User saved/updated successfully for Google");

        return oauthUser;
    }

    // Public method to be called from OIDC service
    public void saveGoogleUser(String email, String name, String accessToken) {
        System.out.println("=== GoogleOAuth2UserService.saveGoogleUser() called via OIDC ===");
        System.out.println("Google OIDC user email: " + email);
        System.out.println("Google OIDC access token: " + accessToken);

        saveOrUpdateUser(email, name, accessToken, null);

        System.out.println("Google OIDC user saved/updated successfully");
    }

    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        System.out.println("=== GoogleOAuth2UserService.handleAuthenticationSuccess() called ===");
        System.out.println("Authentication type: " + event.getAuthentication().getClass().getSimpleName());

        // Handle both OAuth2AuthenticationToken and OAuth2LoginAuthenticationToken
        if (event.getAuthentication() instanceof OAuth2AuthenticationToken auth) {
            processGoogleAuth(auth);
        } else if (event.getAuthentication().getClass().getSimpleName().equals("OAuth2LoginAuthenticationToken")) {
            // Cast to the login authentication token type
            try {
                var loginAuth = (org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken) event.getAuthentication();
                String registrationId = loginAuth.getClientRegistration().getRegistrationId();

                if ("google".equals(registrationId)) {
                    String userEmail = loginAuth.getPrincipal().getAttribute("email");
                    String userName = loginAuth.getName();

                    System.out.println("Processing Google LoginAuthenticationToken for user: " + userEmail);
                    tryMultipleRefreshTokenApproaches(userEmail, userName, null);
                }
            } catch (Exception e) {
                System.err.println("Error processing OAuth2LoginAuthenticationToken: " + e.getMessage());
            }
        } else {
            System.out.println("Unhandled authentication type: " + event.getAuthentication().getClass());
        }
    }

    private void processGoogleAuth(OAuth2AuthenticationToken auth) {
        String registrationId = auth.getAuthorizedClientRegistrationId();
        System.out.println("Registration ID: " + registrationId);

        if ("google".equals(registrationId)) {
            String userEmail = auth.getPrincipal().getAttribute("email");
            String userName = auth.getName();

            System.out.println("Processing refresh token for Google user: " + userEmail);
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
                    System.out.println("=== Checking refresh token after " + delay + "ms delay ===");

                    OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", userName);
                    logClientDetails(client, delay);

                    if (client != null && client.getRefreshToken() != null) {
                        String refreshToken = client.getRefreshToken().getTokenValue();
                        updateUserRefreshToken(userEmail, refreshToken, "google");
                        System.out.println("✅ SUCCESS: Google refresh token found and updated after " + delay + "ms");
                        return; // Exit early on success
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            System.out.println("❌ FAILED: No Google refresh token found after all attempts");

            // Final attempt: Check all authorized clients
            checkAllAuthorizedClients(userEmail);
        });
    }

    private void checkRefreshTokenImmediate(String userEmail, String userName) {
        System.out.println("=== Immediate refresh token check ===");
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", userName);
        logClientDetails(client, 0);
    }

    private void logClientDetails(OAuth2AuthorizedClient client, int delay) {
        System.out.println("--- OAuth2AuthorizedClient Details (delay: " + delay + "ms) ---");
        if (client == null) {
            System.out.println("❌ Client is NULL");
            return;
        }

        System.out.println("✓ Client found");
        System.out.println("Registration ID: " + client.getClientRegistration().getRegistrationId());
        System.out.println("Principal name: " + client.getPrincipalName());

        if (client.getAccessToken() != null) {
            System.out.println("✓ Access token exists");
            System.out.println("Access token scopes: " + client.getAccessToken().getScopes());
        } else {
            System.out.println("❌ Access token is NULL");
        }

        if (client.getRefreshToken() != null) {
            System.out.println("✅ REFRESH TOKEN EXISTS!");
            System.out.println("Refresh token value: " + client.getRefreshToken().getTokenValue());
        } else {
            System.out.println("❌ Refresh token is NULL");
        }
        System.out.println("--------------------------------------------------");
    }

    private void checkAllAuthorizedClients(String userEmail) {
        System.out.println("=== Checking all possible authorized clients ===");

        // Try different principal name variations
        String[] principalNames = {
                userEmail,
                userEmail.toLowerCase(),
                userEmail.substring(0, userEmail.indexOf('@'))  // username part
        };

        for (String principalName : principalNames) {
            System.out.println("Trying principal name: " + principalName);
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", principalName);

            if (client != null) {
                System.out.println("✓ Found client with principal: " + principalName);
                logClientDetails(client, -1);

                if (client.getRefreshToken() != null) {
                    String refreshToken = client.getRefreshToken().getTokenValue();
                    updateUserRefreshToken(userEmail, refreshToken, "google");
                    System.out.println("✅ SUCCESS: Found refresh token with alternative principal name");
                    return;
                }
            } else {
                System.out.println("❌ No client found with principal: " + principalName);
            }
        }
    }

    private void saveOrUpdateUser(String email, String name, String accessToken, String refreshToken) {
        System.out.println("Saving/updating user: " + email);

        Optional<User> optionalUser = userRepository.findByEmail(email);
        User user;

        if (optionalUser.isPresent()) {
            user = optionalUser.get();
            System.out.println("Existing user found");
        } else {
            user = new User();
            user.setEmail(email);
            System.out.println("Creating new user");
        }

        user.setName(name);
        user.setGoogleAccessToken(accessToken);

        if (refreshToken != null) {
            user.setGoogleRefreshToken(refreshToken);
            System.out.println("✅ Refresh token saved to database");
        }

        // Provider logic
        if (user.getProvider() != null && user.getProvider().contains("DROPBOX")) {
            user.setProvider("BOTH");
        } else {
            user.setProvider("GOOGLE");
        }

        userRepository.save(user);
        System.out.println("User saved with provider: " + user.getProvider());
    }

    private void updateUserRefreshToken(String email, String refreshToken, String provider) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if ("google".equals(provider)) {
                user.setGoogleRefreshToken(refreshToken);
            }

            // Provider logic
            if (user.getProvider() != null && user.getProvider().contains("DROPBOX")) {
                user.setProvider("BOTH");
            } else {
                user.setProvider("GOOGLE");
            }

            userRepository.save(user);
            System.out.println("✅ Refresh token updated for user: " + email);
        } else {
            System.out.println("❌ User not found for refresh token update: " + email);
        }
    }
}