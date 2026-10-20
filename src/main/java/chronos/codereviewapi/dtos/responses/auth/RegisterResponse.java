package chronos.codereviewapi.dtos.responses.auth;

import chronos.codereviewapi.entities.UserEntity;
import chronos.codereviewapi.enums.Role;

import java.time.LocalDateTime;

public record RegisterResponse(
        String name,
        String email,
        LocalDateTime registrationDate,
        boolean active,
        Role role
) {
    public static RegisterResponse from(UserEntity user) {
        return new RegisterResponse(
                user.getName(),
                user.getEmail(),
                user.getRegistrationDate(),
                user.isActive(),
                user.getRole()
        );
    }
}
