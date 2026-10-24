
package chronos.codereviewapi.controllers;

import chronos.codereviewapi.dtos.requests.admin.ChangeUserRoleRequest;
import chronos.codereviewapi.dtos.responses.UserSummaryResponse;
import chronos.codereviewapi.services.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserSummaryResponse> changeUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody ChangeUserRoleRequest request
    ) {
        UserSummaryResponse response =
                adminUserService.changeUserRole(userId, request);

        return ResponseEntity.ok(response);
    }
}
