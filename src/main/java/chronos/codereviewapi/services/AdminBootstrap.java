package chronos.codereviewapi.services;

import chronos.codereviewapi.entities.UserEntity;
import chronos.codereviewapi.enums.Role;
import chronos.codereviewapi.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.bootstrap-admin.enabled",
        havingValue = "true"
)
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.name}")
    private String adminName;

    @Value("${app.bootstrap-admin.email}")
    private String adminEmail;

    @Value("${app.bootstrap-admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        if (adminName.isBlank()
                || adminEmail.isBlank()
                || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "Bootstrap do ADMIN exige nome, email e senha."
            );
        }

        if (adminPassword.getBytes(
                java.nio.charset.StandardCharsets.UTF_8
        ).length > 72) {
            throw new IllegalStateException(
                    "A senha inicial não pode ultrapassar 72 bytes UTF-8."
            );
        }

        if (userRepository.existsByEmail(adminEmail)) {
            throw new IllegalStateException(
                    "O email configurado para o ADMIN já pertence a um usuário."
            );
        }

        UserEntity admin = UserEntity.builder()
                .name(adminName)
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .active(true)
                .build();

        userRepository.save(admin);
    }
}
