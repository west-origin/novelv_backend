package net.app.novelv.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<UserMeResponse> me(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found."));
        return ResponseEntity.ok(UserMeResponse.from(user));
    }
}