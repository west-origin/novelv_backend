package net.app.novelv.domain.user;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record UserMeResponse(
        Long userId,
        String email,
        String nickname,
        String profileImageUrl,
        Integer coinBalance,
        SocialProvider provider,
        UserStatus status,
        LocalDateTime createdAt,
        Set<RoleProfile> roles
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
                user.getRoles().stream()
                        .map(RoleProfile::from)
                        .collect(Collectors.toUnmodifiableSet())
        );
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