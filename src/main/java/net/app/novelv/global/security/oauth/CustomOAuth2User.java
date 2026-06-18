package net.app.novelv.global.security.oauth;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
public class CustomOAuth2User implements OAuth2User {

    private final Long userId;
    private final String email;
    private final String nickname;
    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomOAuth2User(
            Long userId,
            String email,
            String nickname,
            Map<String, Object> attributes,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
        this.attributes = attributes;
        this.authorities = authorities;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
