package net.app.novelv.domain.auth;

import net.app.novelv.domain.user.SocialProvider;
import net.app.novelv.domain.user.User;
import net.app.novelv.domain.user.UserStatus;

import java.util.Set;

public record AuthResponse(
        String tokenType,
        String accessToken,
        UserProfile user
) {
    public static AuthResponse of(String accessToken, User user) {
        return new AuthResponse("Bearer", accessToken, UserProfile.from(user));
    }

    public record UserProfile(
            Long userId,
            String email,
            String nickname,
            String profileImageUrl,
            Integer coinBalance,
            SocialProvider provider,
            UserStatus status,
            Set<String> roles
    ) {
        public static UserProfile from(User user) {
            return new UserProfile(
                    user.getId(),
                    user.getEmail(),
                    user.getNickname(),
                    user.getProfileImageUrl(),
                    user.getCoinBalance(),
                    user.getProvider(),
                    user.getStatus(),
                    user.getRoleNames()
            );
        }
    }
}