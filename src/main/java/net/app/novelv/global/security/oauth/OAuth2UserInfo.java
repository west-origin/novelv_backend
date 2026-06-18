package net.app.novelv.global.security.oauth;

import java.util.Map;

public record OAuth2UserInfo(
        OAuth2Provider provider,
        String providerId,
        String email,
        String nickname
) {

    @SuppressWarnings("unchecked")
    public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        OAuth2Provider provider = OAuth2Provider.from(registrationId);

        return switch (provider) {
            case GOOGLE -> new OAuth2UserInfo(
                    provider,
                    String.valueOf(attributes.get("sub")),
                    (String) attributes.get("email"),
                    (String) attributes.get("name")
            );
            case KAKAO -> {
                Map<String, Object> kakaoAccount =
                        (Map<String, Object>) attributes.getOrDefault("kakao_account", Map.of());
                Map<String, Object> profile =
                        (Map<String, Object>) kakaoAccount.getOrDefault("profile", Map.of());

                yield new OAuth2UserInfo(
                        provider,
                        String.valueOf(attributes.get("id")),
                        (String) kakaoAccount.get("email"),
                        (String) profile.get("nickname")
                );
            }
            case NAVER -> {
                Map<String, Object> response =
                        (Map<String, Object>) attributes.getOrDefault("response", Map.of());

                yield new OAuth2UserInfo(
                        provider,
                        (String) response.get("id"),
                        (String) response.get("email"),
                        (String) response.get("nickname")
                );
            }
        };
    }
}
