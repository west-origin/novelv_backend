package net.app.novelv.domain.auth;

import net.app.novelv.domain.user.SocialProvider;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class OAuthProviderClient {

    private final OAuthClientProperties properties;
    private final RestClient restClient;

    public OAuthProviderClient(OAuthClientProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    public SocialUserInfo fetchUserInfo(SocialProvider provider, String code, String redirectUri, String state) {
        OAuthClientProperties.Client client = client(provider);
        String accessToken = requestAccessToken(provider, client, code, redirectUri, state);
        Map<String, Object> attributes = requestUserAttributes(client.userInfoUri(), accessToken);
        return parseUserInfo(provider, attributes);
    }

    @SuppressWarnings("unchecked")
    private String requestAccessToken(
            SocialProvider provider,
            OAuthClientProperties.Client client,
            String code,
            String redirectUri,
            String state
    ) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", client.clientId());
        form.add("redirect_uri", redirectUri);
        form.add("code", code);

        if (client.clientSecret() != null && !client.clientSecret().isBlank()) {
            form.add("client_secret", client.clientSecret());
        }
        if (provider == SocialProvider.NAVER && state != null && !state.isBlank()) {
            form.add("state", state);
        }

        Map<String, Object> response = restClient.post()
                .uri(client.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, httpResponse) -> {
                    throw new OAuthProviderException(provider + " token request failed: "
                            + httpResponse.getStatusCode() + " " + responseBody(httpResponse));
                })
                .body(Map.class);

        Object accessToken = response == null ? null : response.get("access_token");
        if (accessToken == null) {
            throw new OAuthProviderException(provider + " did not return access_token.");
        }
        return accessToken.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requestUserAttributes(String userInfoUri, String accessToken) {
        return restClient.get()
                .uri(userInfoUri)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new OAuthProviderException("OAuth user info request failed: "
                            + response.getStatusCode() + " " + responseBody(response));
                })
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    private SocialUserInfo parseUserInfo(SocialProvider provider, Map<String, Object> attributes) {
        if (attributes == null) {
            throw new OAuthProviderException(provider + " did not return user attributes.");
        }

        return switch (provider) {
            case GOOGLE -> {
                String providerId = stringValue(attributes.get("sub"));
                String email = stringValue(attributes.get("email"));
                yield new SocialUserInfo(provider, providerId, email, fallbackNickname(attributes.get("name"), email, providerId));
            }
            case KAKAO -> {
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.getOrDefault("kakao_account", Map.of());
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.getOrDefault("profile", Map.of());
                String providerId = stringValue(attributes.get("id"));
                String email = stringValue(kakaoAccount.get("email"));
                if (email == null || email.isBlank()) {
                    email = providerId + "@kakao.local";
                }
                yield new SocialUserInfo(provider, providerId, email, fallbackNickname(profile.get("nickname"), email, providerId));
            }
            case NAVER -> {
                Map<String, Object> response = (Map<String, Object>) attributes.getOrDefault("response", Map.of());
                String providerId = stringValue(response.get("id"));
                String email = stringValue(response.get("email"));
                yield new SocialUserInfo(provider, providerId, email, fallbackNickname(response.getOrDefault("nickname", response.get("name")), email, providerId));
            }
        };
    }

    private String responseBody(org.springframework.http.client.ClientHttpResponse response) {
        try {
            return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private OAuthClientProperties.Client client(SocialProvider provider) {
        return switch (provider) {
            case GOOGLE -> properties.google();
            case KAKAO -> properties.kakao();
            case NAVER -> properties.naver();
        };
    }

    private String fallbackNickname(Object nickname, String email, String providerId) {
        String value = stringValue(nickname);
        if (value != null && !value.isBlank()) {
            return value;
        }
        if (email != null && !email.isBlank()) {
            return email.split("@")[0];
        }
        return "user-" + providerId;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}