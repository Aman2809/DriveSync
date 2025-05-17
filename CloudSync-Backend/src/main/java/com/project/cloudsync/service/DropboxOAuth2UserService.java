package com.project.cloudsync.service;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.users.FullAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class DropboxOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        String accessToken = userRequest.getAccessToken().getTokenValue();
        DbxRequestConfig config = DbxRequestConfig.newBuilder("cloudsync").build();

        try {
            DbxClientV2 client = new DbxClientV2(config, accessToken);
            FullAccount account = client.users().getCurrentAccount();

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("account_id", account.getAccountId());
            attributes.put("email", account.getEmail());
            attributes.put("name", account.getName().getDisplayName());

            return new DefaultOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                    attributes,
                    "email"
            );

        } catch (DbxException e) {
            OAuth2Error oauth2Error = new OAuth2Error(
                    "dropbox_error",
                    "Failed to fetch Dropbox user info: " + e.getMessage(),
                    null
            );
            throw new OAuth2AuthenticationException(oauth2Error, e);
        }
    }
}
