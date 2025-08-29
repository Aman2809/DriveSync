package com.project.cloudsync.configurations;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;

    public CustomOAuth2AuthorizationRequestResolver(ClientRegistrationRepository repo) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
        return customizeAuthorizationRequest(authorizationRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request, clientRegistrationId);
        return customizeAuthorizationRequest(authorizationRequest);
    }

    private OAuth2AuthorizationRequest customizeAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest) {
        if (authorizationRequest == null) return null;

        Map<String, Object> additionalParameters = new HashMap<>(authorizationRequest.getAdditionalParameters());

        System.out.println("=== AUTHORIZATION REQUEST DEBUG ===");
        System.out.println("Original URI: " + authorizationRequest.getAuthorizationUri());
        System.out.println("Existing parameters: " + additionalParameters);
        System.out.println("Client ID: " + authorizationRequest.getClientId());
        System.out.println("Scopes: " + authorizationRequest.getScopes());

        // For Google: Add offline access and force consent
        if (authorizationRequest.getAuthorizationUri().contains("accounts.google.com")) {
            // Only add if not already present to avoid duplicates
            if (!additionalParameters.containsKey("access_type")) {
                additionalParameters.put("access_type", "offline");
            }
            if (!additionalParameters.containsKey("prompt")) {
                additionalParameters.put("prompt", "consent");
            }
            if (!additionalParameters.containsKey("include_granted_scopes")) {
                additionalParameters.put("include_granted_scopes", "true");
            }
            System.out.println("✓ Added Google offline parameters");
        }

        // For Dropbox: Add offline access
        if (authorizationRequest.getAuthorizationUri().contains("dropbox.com")) {
            if (!additionalParameters.containsKey("token_access_type")) {
                additionalParameters.put("token_access_type", "offline");
            }
            System.out.println("✓ Added Dropbox offline parameters");
        }

        System.out.println("Final parameters: " + additionalParameters);

        OAuth2AuthorizationRequest customRequest = OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(additionalParameters)
                .build();

        // Log the final authorization URL that will be used
        String finalUrl = buildAuthorizationUrl(customRequest);
        System.out.println("Final Authorization URL: " + finalUrl);
        System.out.println("=====================================");

        return customRequest;
    }

    private String buildAuthorizationUrl(OAuth2AuthorizationRequest request) {
        StringBuilder url = new StringBuilder(request.getAuthorizationUri());
        url.append("?response_type=").append(request.getResponseType());
        url.append("&client_id=").append(request.getClientId());
        url.append("&scope=").append(String.join("%20", request.getScopes()));
        url.append("&redirect_uri=").append(request.getRedirectUri());
        url.append("&state=").append(request.getState());

        request.getAdditionalParameters().forEach((key, value) ->
                url.append("&").append(key).append("=").append(value)
        );

        return url.toString();
    }
}