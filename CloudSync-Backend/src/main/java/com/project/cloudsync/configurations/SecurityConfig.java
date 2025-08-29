package com.project.cloudsync.configurations;

import com.project.cloudsync.service.DropboxOAuth2UserService;
import com.project.cloudsync.service.GoogleOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final DropboxOAuth2UserService dropboxOAuth2UserService;
    private final CustomOAuth2AuthorizationRequestResolver authorizationRequestResolver;

    public SecurityConfig(GoogleOAuth2UserService googleOAuth2UserService,
                          DropboxOAuth2UserService dropboxOAuth2UserService,
                          CustomOAuth2AuthorizationRequestResolver authorizationRequestResolver) {
        this.googleOAuth2UserService = googleOAuth2UserService;
        this.dropboxOAuth2UserService = dropboxOAuth2UserService;
        this.authorizationRequestResolver = authorizationRequestResolver;
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        return userRequest -> {
            String registrationId = userRequest.getClientRegistration().getRegistrationId();

            System.out.println("=== OAuth2UserService called for provider: " + registrationId + " ===");

            if ("google".equals(registrationId)) {
                System.out.println("Delegating to GoogleOAuth2UserService");
                return googleOAuth2UserService.loadUser(userRequest);
            } else if ("dropbox".equals(registrationId)) {
                System.out.println("Delegating to DropboxOAuth2UserService");
                return dropboxOAuth2UserService.loadUser(userRequest);
            } else {
                throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + registrationId);
            }
        };
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        return new OAuth2UserService<OidcUserRequest, OidcUser>() {
            private final OidcUserService delegate = new OidcUserService();

            @Override
            public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
                String registrationId = userRequest.getClientRegistration().getRegistrationId();

                System.out.println("=== OidcUserService called for provider: " + registrationId + " ===");

                if ("google".equals(registrationId)) {
                    System.out.println("Processing Google OIDC authentication");

                    // First get the OIDC user from the default service
                    OidcUser oidcUser = delegate.loadUser(userRequest);

                    // Extract user info and save to database
                    String email = oidcUser.getAttribute("email");
                    String name = oidcUser.getAttribute("name");
                    String accessToken = userRequest.getAccessToken().getTokenValue();

                    System.out.println("Google OIDC user email: " + email);
                    System.out.println("Google OIDC access token: " + accessToken);

                    // Call your Google service to save user data
                    googleOAuth2UserService.saveGoogleUser(email, name, accessToken);

                    return oidcUser;
                } else {
                    // For non-Google OIDC providers, use default behavior
                    return delegate.loadUser(userRequest);
                }
            }
        };
    }

//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable())
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/", "/error", "/api/**").permitAll()
//                        .anyRequest().authenticated()
//                )
//                .oauth2Login(oauth2 -> oauth2
//                        .defaultSuccessUrl("/user", true)
//                        .userInfoEndpoint(userInfo -> userInfo
//                                .userService(oauth2UserService())
//                                .oidcUserService(oidcUserService())
//                        )
//                );
//
//        return http.build();
//    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/error", "/api/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/user", true)
                        .authorizationEndpoint(authorization ->
                                authorization.authorizationRequestResolver(authorizationRequestResolver)) // ADD THIS LINE
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oauth2UserService())
                                .oidcUserService(oidcUserService())
                        )
                );
        return http.build();
    }




}