package com.project.cloudsync.configurations;

import com.project.cloudsync.service.DropboxOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final DropboxOAuth2UserService dropboxOAuth2UserService;

    public SecurityConfig(DropboxOAuth2UserService dropboxOAuth2UserService) {
        this.dropboxOAuth2UserService = dropboxOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/user", true)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(dropboxOAuth2UserService)
                        )
                );

        return http.build();
    }
}