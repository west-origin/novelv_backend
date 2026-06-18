package net.app.novelv.domain.auth;

import jakarta.validation.constraints.NotBlank;

public record OAuthCodeRequest(
        @NotBlank String code,
        @NotBlank String redirectUri,
        String state
) {
}