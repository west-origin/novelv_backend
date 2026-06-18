package net.app.novelv.global.security.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    // TODO: 실제 회원 도메인 추가 후 UserRepository를 주입해 provider + providerId 기준으로 upsert하세요.
    // private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfo.of(registrationId, oauth2User.getAttributes());

        if (userInfo.email() == null || userInfo.email().isBlank()) {
            throw new OAuth2AuthenticationException("OAuth2 provider did not return email.");
        }

        /*
         * TODO 예시:
         * User user = userRepository.findByProviderAndProviderId(userInfo.provider(), userInfo.providerId())
         *         .map(existing -> existing.updateSocialProfile(userInfo.email(), userInfo.nickname()))
         *         .orElseGet(() -> userRepository.save(User.createSocialUser(
         *                 userInfo.provider(),
         *                 userInfo.providerId(),
         *                 userInfo.email(),
         *                 userInfo.nickname()
         *         )));
         */
        Long userId = 1L;
        String role = "ROLE_USER";

        return new CustomOAuth2User(
                userId,
                userInfo.email(),
                userInfo.nickname(),
                oauth2User.getAttributes(),
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
