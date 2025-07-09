package com.project.cloudsync.configurations;

import com.project.cloudsync.service.DropboxOAuth2UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> googleOAuth2UserService;
    private final DropboxOAuth2UserService dropboxOAuth2UserService;

    public SecurityConfig(
            @Qualifier("googleOAuth2UserService") OAuth2UserService<OAuth2UserRequest, OAuth2User> googleOAuth2UserService,
            DropboxOAuth2UserService dropboxOAuth2UserService
    ) {
        this.googleOAuth2UserService = googleOAuth2UserService;
        this.dropboxOAuth2UserService = dropboxOAuth2UserService;
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService() {
        return new OAuth2UserService<OAuth2UserRequest, OAuth2User>() {
            @Override
            public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
                String registrationId = userRequest.getClientRegistration().getRegistrationId();

                switch (registrationId.toLowerCase()) {
                    case "google":
                        return googleOAuth2UserService.loadUser(userRequest);
                    case "dropbox":
                        return dropboxOAuth2UserService.loadUser(userRequest);
                    default:
                        throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
                }
            }
        };
    }

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
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService())
                        )
                );

        return http.build();
    }
}