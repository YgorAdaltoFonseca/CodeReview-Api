package chronos.codereviewapi.dtos.responses;

import chronos.codereviewapi.enums.Role;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String name,
        String email,
        Role role,
        LocalDateTime createdAt
) {
}
