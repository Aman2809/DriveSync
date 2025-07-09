package com.project.cloudsync.service;

import com.project.cloudsync.entities.User;
import com.project.cloudsync.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service("googleOAuth2UserService")
public class GoogleOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        System.out.println("=== GoogleOAuth2UserService.loadUser called ===");

        OAuth2User oauthUser = super.loadUser(userRequest);

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String accessToken = userRequest.getAccessToken().getTokenValue();

//        System.out.println("=== GoogleOAuth2UserService.loadUser called ===");

        // Try to get refresh token if available
        String refreshToken = null;
//        if (userRequest.getAccessToken().getRefreshToken() != null) {
//            refreshToken = userRequest.getAccessToken().getRefreshToken().getTokenValue();
//        }

        // Save user to database
        saveOrUpdateUser(email, name, accessToken, refreshToken);

        // Create consistent attributes map (similar to Dropbox service)
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", email);
        attributes.put("name", name);
        attributes.put("sub", oauthUser.getAttribute("sub")); // Google's user ID

        // Add all original attributes
        attributes.putAll(oauthUser.getAttributes());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "email"
        );
    }

    private void saveOrUpdateUser(String email, String name, String accessToken, String refreshToken) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        User user;

        if (optionalUser.isPresent()) {
            user = optionalUser.get();
        } else {
            user = new User();
            user.setEmail(email);
        }

        user.setName(name);
        user.setGoogleAccessToken(accessToken);
        if (refreshToken != null) {
            user.setGoogleRefreshToken(refreshToken);
        }

        // Handle provider logic
        if (user.getProvider() != null && user.getProvider().contains("DROPBOX")) {
            user.setProvider("BOTH");
        } else {
            user.setProvider("GOOGLE");
        }

        userRepository.save(user);
    }
}