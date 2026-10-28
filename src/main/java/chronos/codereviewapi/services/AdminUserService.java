
package chronos.codereviewapi.services;

import chronos.codereviewapi.dtos.requests.admin.ChangeUserRoleRequest;
import chronos.codereviewapi.dtos.responses.AdminUserResponse;
import chronos.codereviewapi.dtos.responses.PageResponse;
import chronos.codereviewapi.dtos.responses.UserSummaryResponse;
import chronos.codereviewapi.entities.UserEntity;
import chronos.codereviewapi.enums.Role;
import chronos.codereviewapi.repositories.UserRepository;
import chronos.codereviewapi.services.settingsServices.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;

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
        List<UserEntity> administrators = userRepository
                .findAllByRoleForUpdate(Role.ADMIN);
        UserEntity authenticatedUser =
                authenticatedUserService.getAuthenticatedUser();
        if (authenticatedUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Administrator role is no longer active.");
        }

        UserEntity targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found."
                ));

        if (authenticatedUser.getId().equals(userId)
                && targetUser.getRole() != request.role()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You cannot change your own role."
            );
        }

        if (targetUser.getRole() == Role.ADMIN
                && request.role() != Role.ADMIN
                && administrators.size() <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The last administrator cannot be demoted."
            );
        }

        if (targetUser.getRole() == request.role()) {
            return toUserSummary(targetUser);
        }

        targetUser.setRole(request.role());
        return toUserSummary(userRepository.save(targetUser));
    }

    private UserSummaryResponse toUserSummary(UserEntity user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
