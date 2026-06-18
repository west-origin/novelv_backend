package net.app.novelv.domain.auth;

import net.app.novelv.domain.user.SocialProvider;

public record SocialUserInfo(
        SocialProvider provider,
        String providerId,
        String email,
        String nickname
) {
}