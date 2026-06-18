package net.app.novelv.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RoleDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<RoleSeed> seeds = List.of(
                new RoleSeed("ROLE_GUEST", "비회원"),
                new RoleSeed("ROLE_USER", "일반 회원"),
                new RoleSeed("ROLE_CREATOR", "크리에이터"),
                new RoleSeed("ROLE_MODERATOR", "매니저"),
                new RoleSeed("ROLE_ADMIN", "관리자")
        );

        for (RoleSeed seed : seeds) {
            roleRepository.findByRoleName(seed.roleName())
                    .ifPresentOrElse(
                            role -> role.setDescription(seed.description()),
                            () -> roleRepository.save(new Role(seed.roleName(), seed.description()))
                    );
        }
    }

    private record RoleSeed(String roleName, String description) {
    }
}