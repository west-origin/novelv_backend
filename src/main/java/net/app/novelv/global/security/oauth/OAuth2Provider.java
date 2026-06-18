package net.app.novelv.global.security.oauth;

public enum OAuth2Provider {
    GOOGLE,
    KAKAO,
    NAVER;

    public static OAuth2Provider from(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> GOOGLE;
            case "kakao" -> KAKAO;
            case "naver" -> NAVER;
            default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + registrationId);
        };
    }
}
