
package chronos.codereviewapi.controllers;

import chronos.codereviewapi.dtos.requests.admin.ChangeUserRoleRequest;
import chronos.codereviewapi.dtos.responses.AdminUserResponse;
import chronos.codereviewapi.dtos.responses.PageResponse;
import chronos.codereviewapi.dtos.responses.UserSummaryResponse;
import chronos.codereviewapi.services.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<AdminUserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminUserService.listUsers(page, size));
    }

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
