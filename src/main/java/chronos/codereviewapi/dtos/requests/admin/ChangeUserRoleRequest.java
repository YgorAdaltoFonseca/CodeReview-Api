
package chronos.codereviewapi.dtos.requests.admin;

import chronos.codereviewapi.enums.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeUserRoleRequest(
        @NotNull(message = "Role is required")
        Role role
) {
}
