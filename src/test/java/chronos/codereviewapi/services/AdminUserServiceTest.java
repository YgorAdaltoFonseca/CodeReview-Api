package chronos.codereviewapi.services;

import chronos.codereviewapi.dtos.requests.admin.ChangeUserRoleRequest;
import chronos.codereviewapi.entities.UserEntity;
import chronos.codereviewapi.enums.Role;
import chronos.codereviewapi.repositories.UserRepository;
import chronos.codereviewapi.services.settingsServices.AuthenticatedUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuthenticatedUserService authenticatedUserService =
            mock(AuthenticatedUserService.class);
    private final AdminUserService adminUserService = new AdminUserService(
            userRepository,
            authenticatedUserService
    );

    @BeforeEach
    void setUpAuthenticatedAdmin() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(
                user(1L, Role.ADMIN)
        );
    }

    @Test
    void listUsersReturnsPagedPublicUserData() {
        UserEntity user = user(12L, Role.REVIEWER);
        user.setPasswordHash("must-not-be-returned");
        PageRequest pageRequest = PageRequest.of(
                1, 5, Sort.by(Sort.Direction.ASC, "id")
        );
        when(userRepository.findAll(pageRequest)).thenReturn(
                new PageImpl<>(List.of(user), pageRequest, 6)
        );

        var response = adminUserService.listUsers(1, 5);

        assertEquals(1, response.page());
        assertEquals(5, response.size());
        assertEquals(6, response.totalElements());
        assertEquals(2, response.totalPages());
        assertEquals(12L, response.content().getFirst().id());
        assertEquals("maria@example.com", response.content().getFirst().email());
        verify(userRepository).findAll(pageRequest);
    }

    @Test
    void listUsersRejectsInvalidPageSize() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> adminUserService.listUsers(0, 101)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void administratorCanAssignAnyDefinedRole() {
        UserEntity developer = user(8L, Role.DEVELOPER);
        when(userRepository.findAllByRoleForUpdate(Role.ADMIN))
                .thenReturn(List.of(user(1L, Role.ADMIN)));
        when(userRepository.findById(8L)).thenReturn(Optional.of(developer));
        when(userRepository.save(developer)).thenReturn(developer);

        var response = adminUserService.changeUserRole(
                8L,
                new ChangeUserRoleRequest(Role.ADMIN)
        );

        assertEquals(8L, response.id());
        assertEquals(Role.ADMIN, response.role());
        verify(userRepository).save(developer);
    }

    @Test
    void administratorCannotChangeTheirOwnRole() {
        when(userRepository.findAllByRoleForUpdate(Role.ADMIN))
                .thenReturn(List.of(user(1L, Role.ADMIN)));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user(1L, Role.ADMIN)));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> adminUserService.changeUserRole(
                        1L,
                        new ChangeUserRoleRequest(Role.REVIEWER)
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void lastAdministratorCannotBeDemoted() {
        when(userRepository.findAllByRoleForUpdate(Role.ADMIN))
                .thenReturn(List.of(user(2L, Role.ADMIN)));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user(2L, Role.ADMIN)));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> adminUserService.changeUserRole(
                        2L,
                        new ChangeUserRoleRequest(Role.DEVELOPER)
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    private UserEntity user(Long id, Role role) {
        return UserEntity.builder()
                .id(id)
                .name("Maria")
                .email("maria@example.com")
                .passwordHash("encoded-password")
                .role(role)
                .registrationDate(LocalDateTime.parse("2026-10-27T10:00:00"))
                .build();
    }
}
