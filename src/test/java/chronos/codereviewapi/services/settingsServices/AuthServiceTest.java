package chronos.codereviewapi.services.settingsServices;

import chronos.codereviewapi.dtos.requests.auth.RegisterRequest;
import chronos.codereviewapi.exceptions.EmailAlreadyExistsException;
import chronos.codereviewapi.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerRejectsAnEmailThatIsAlreadyRegistered() {
        RegisterRequest request = new RegisterRequest(
                "Ygor", "ygor@example.com", "safe-password"
        );
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(request)
        );

        verify(passwordEncoder, never()).encode(request.password());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
