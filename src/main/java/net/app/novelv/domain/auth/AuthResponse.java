package net.app.novelv.domain.auth;

import net.app.novelv.domain.user.Role;
import net.app.novelv.domain.user.SocialProvider;
import net.app.novelv.domain.user.User;
import net.app.novelv.domain.user.UserStatus;

import java.util.Set;
import java.util.stream.Collectors;

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
            Set<RoleProfile> roles
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
                    user.getRoles().stream()
                            .map(RoleProfile::from)
                            .collect(Collectors.toUnmodifiableSet())
            );
        }
    }

    public record RoleProfile(
            String roleName,
            String description
    ) {
        public static RoleProfile from(Role role) {
            return new RoleProfile(role.getRoleName(), role.getDescription());
        }
    }
}