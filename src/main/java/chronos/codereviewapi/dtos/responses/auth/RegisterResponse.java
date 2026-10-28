package chronos.codereviewapi.dtos.responses.auth;

import chronos.codereviewapi.entities.UserEntity;
import chronos.codereviewapi.enums.Role;

import java.time.LocalDateTime;

public record RegisterResponse(
        Long id,
        String name,
        String email,
        Role role,
        LocalDateTime createdAt
) {
    public static RegisterResponse from(UserEntity user) {
        return new RegisterResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getRegistrationDate()
        );
    }
}
