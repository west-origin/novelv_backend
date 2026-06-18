package net.app.novelv.domain.user;

public enum SocialProvider {
    GOOGLE,
    KAKAO,
    NAVER;

    public static SocialProvider from(String value) {
        return switch (value.toLowerCase()) {
            case "google" -> GOOGLE;
            case "kakao" -> KAKAO;
            case "naver" -> NAVER;
            default -> throw new IllegalArgumentException("Unsupported social provider: " + value);
        };
    }
}