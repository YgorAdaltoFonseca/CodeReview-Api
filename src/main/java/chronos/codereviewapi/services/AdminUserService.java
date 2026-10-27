
package chronos.codereviewapi.services;

import chronos.codereviewapi.dtos.requests.admin.ChangeUserRoleRequest;
import chronos.codereviewapi.dtos.responses.AdminUserResponse;
import chronos.codereviewapi.dtos.responses.PageResponse;
import chronos.codereviewapi.dtos.responses.UserSummaryResponse;
import chronos.codereviewapi.entities.UserEntity;
import chronos.codereviewapi.enums.Role;
import chronos.codereviewapi.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> listUsers(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100."
            );
        }

        Page<UserEntity> users = userRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"))
        );

        return new PageResponse<>(
                users.getContent().stream()
                        .map(user -> new AdminUserResponse(
                                user.getId(),
                                user.getName(),
                                user.getEmail(),
                                user.getRole(),
                                user.getRegistrationDate()
                        ))
                        .toList(),
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages()
        );
    }

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