package net.app.novelv.domain.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/oauth/{provider}")
    public ResponseEntity<AuthResponse> oauthLogin(
            @PathVariable String provider,
            @Valid @RequestBody OAuthCodeRequest request
    ) {
        return ResponseEntity.ok(authService.loginWithOAuthCode(provider, request));
    }
}