
package chronos.codereviewapi.services.settingsServices;

import chronos.codereviewapi.dtos.requests.auth.LoginRequest;
import chronos.codereviewapi.dtos.requests.auth.RegisterRequest;
import chronos.codereviewapi.dtos.responses.auth.LoginResponse;
import chronos.codereviewapi.entities.UserEntity;
import chronos.codereviewapi.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserEntity register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        UserEntity user = new UserEntity(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password())
        );

        return userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {

        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        var user = (UserEntity) authentication.getPrincipal();

        String token = jwtService.generateToken(user);

        return new LoginResponse(token, "Bearer");
    }
}
