package net.app.novelv.domain.user;

import java.time.LocalDateTime;
import java.util.Set;

public record UserMeResponse(
        Long userId,
        String email,
        String nickname,
        String profileImageUrl,
        Integer coinBalance,
        SocialProvider provider,
        UserStatus status,
        LocalDateTime createdAt,
        Set<String> roles
) {
    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getCoinBalance(),
                user.getProvider(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getRoleNames()
        );
    }
}