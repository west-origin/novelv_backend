package net.app.novelv.domain.auth;

import lombok.RequiredArgsConstructor;
import net.app.novelv.domain.user.Role;
import net.app.novelv.domain.user.RoleRepository;
import net.app.novelv.domain.user.SocialProvider;
import net.app.novelv.domain.user.User;
import net.app.novelv.domain.user.UserRepository;
import net.app.novelv.global.security.jwt.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final OAuthProviderClient oAuthProviderClient;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse loginWithOAuthCode(String providerName, OAuthCodeRequest request) {
        SocialProvider provider = SocialProvider.from(providerName);
        SocialUserInfo userInfo = oAuthProviderClient.fetchUserInfo(
                provider,
                request.code(),
                request.redirectUri(),
                request.state()
        );

        validateUserInfo(userInfo);

        User user = userRepository.findByProviderAndProviderId(provider, userInfo.providerId())
                .map(existing -> {
                    existing.updateSocialProfile(userInfo.email(), userInfo.nickname());
                    ensureDefaultRole(existing);
                    return existing;
                })
                .orElseGet(() -> {
                    User newUser = User.createSocialUser(
                            userInfo.email(),
                            userInfo.nickname(),
                            provider,
                            userInfo.providerId()
                    );
                    ensureDefaultRole(newUser);
                    return userRepository.save(newUser);
                });

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                user.getPrimaryRoleName()
        );
        return AuthResponse.of(accessToken, user);
    }

    private void ensureDefaultRole(User user) {
        boolean alreadyHasDefaultRole = user.getRoles().stream()
                .anyMatch(role -> DEFAULT_ROLE.equals(role.getRoleName()));

        if (alreadyHasDefaultRole) {
            return;
        }

        Role defaultRole = roleRepository.findByRoleName(DEFAULT_ROLE)
                .orElseGet(() -> roleRepository.save(new Role(DEFAULT_ROLE, "일반 회원")));
        user.addRole(defaultRole);
    }

    private void validateUserInfo(SocialUserInfo userInfo) {
        if (userInfo.providerId() == null || userInfo.providerId().isBlank()) {
            throw new IllegalStateException("OAuth provider did not return provider id.");
        }
        if (userInfo.email() == null || userInfo.email().isBlank()) {
            throw new IllegalStateException("OAuth provider did not return email.");
        }
        if (userInfo.nickname() == null || userInfo.nickname().isBlank()) {
            throw new IllegalStateException("OAuth provider did not return nickname.");
        }
    }
}