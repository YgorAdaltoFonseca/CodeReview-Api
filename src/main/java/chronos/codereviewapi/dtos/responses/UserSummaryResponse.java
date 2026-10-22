package chronos.codereviewapi.dtos.responses;

import chronos.codereviewapi.enums.Role;

public record UserSummaryResponse(
        String name,
        String email ,
        Role role
) {}
