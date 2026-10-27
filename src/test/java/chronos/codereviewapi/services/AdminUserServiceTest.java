package chronos.codereviewapi.services;

import chronos.codereviewapi.entities.UserEntity;
import chronos.codereviewapi.enums.Role;
import chronos.codereviewapi.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AdminUserService adminUserService = new AdminUserService(userRepository);

    @Test
    void listUsersReturnsPagedPublicUserData() {
        UserEntity user = UserEntity.builder()
                .id(12L)
                .name("Maria")
                .email("maria@example.com")
                .passwordHash("must-not-be-returned")
                .role(Role.REVIEWER)
                .registrationDate(LocalDateTime.parse("2026-10-27T10:00:00"))
                .build();
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
        assertThrows(ResponseStatusException.class,
                () -> adminUserService.listUsers(0, 101));
    }
}
