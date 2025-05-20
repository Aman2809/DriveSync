package com.project.cloudsync.controller;


import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping
public class UserController {

    private final OAuth2AuthorizedClientService authorizedClientService;

    public UserController(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    @GetMapping("/user")
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal OAuth2User user,
                                           OAuth2AuthenticationToken token) {
        OAuth2AuthorizedClient client = authorizedClientService.
                loadAuthorizedClient(token.getAuthorizedClientRegistrationId(), token.getName());
        String accessToken = client.getAccessToken().getTokenValue();
        System.out.println("Google AccessToken: " + accessToken);
        return user.getAttributes();
    }
}
