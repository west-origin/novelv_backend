package net.app.novelv.domain.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OAuthClientProperties {

    @Value("${app.oauth2.google.client-id}")
    private String googleClientId;

    @Value("${app.oauth2.google.client-secret}")
    private String googleClientSecret;

    @Value("${app.oauth2.kakao.client-id}")
    private String kakaoClientId;

    @Value("${app.oauth2.kakao.client-secret:}")
    private String kakaoClientSecret;

    @Value("${app.oauth2.naver.client-id}")
    private String naverClientId;

    @Value("${app.oauth2.naver.client-secret}")
    private String naverClientSecret;

    @Value("${app.oauth2.google.token-uri}")
    private String googleTokenUri;

    @Value("${app.oauth2.google.user-info-uri}")
    private String googleUserInfoUri;

    @Value("${app.oauth2.kakao.token-uri}")
    private String kakaoTokenUri;

    @Value("${app.oauth2.kakao.user-info-uri}")
    private String kakaoUserInfoUri;

    @Value("${app.oauth2.naver.token-uri}")
    private String naverTokenUri;

    @Value("${app.oauth2.naver.user-info-uri}")
    private String naverUserInfoUri;

    public Client google() {
        return new Client(googleClientId, googleClientSecret, googleTokenUri, googleUserInfoUri);
    }

    public Client kakao() {
        return new Client(kakaoClientId, kakaoClientSecret, kakaoTokenUri, kakaoUserInfoUri);
    }

    public Client naver() {
        return new Client(naverClientId, naverClientSecret, naverTokenUri, naverUserInfoUri);
    }

    public record Client(String clientId, String clientSecret, String tokenUri, String userInfoUri) {
    }
}