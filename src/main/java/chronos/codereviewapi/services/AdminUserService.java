
package chronos.codereviewapi.services;

import chronos.codereviewapi.dtos.requests.admin.ChangeUserRoleRequest;
import chronos.codereviewapi.dtos.responses.UserSummaryResponse;
import chronos.codereviewapi.entities.UserEntity;
import chronos.codereviewapi.enums.Role;
import chronos.codereviewapi.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserSummaryResponse changeUserRole(
            Long userId,
            ChangeUserRoleRequest request
    ) {

        if (request.role() != Role.REVIEWER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This endpoint only allows promotion to REVIEWER."
            );
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));

        if (user.getRole() != Role.DEVELOPER) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only DEVELOPER users can be promoted to REVIEWER."
            );
        }

        user.setRole(Role.REVIEWER);

        UserEntity updatedUser = userRepository.save(user);

        return new UserSummaryResponse(
                updatedUser.getName(),
                updatedUser.getEmail(),
                updatedUser.getRole()
        );
    }
}