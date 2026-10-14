
package chronos.codereviewapi.controllers;

import chronos.codereviewapi.dtos.requests.LoginRequest;
import chronos.codereviewapi.dtos.requests.RegisterRequest;
import chronos.codereviewapi.dtos.responses.LoginResponse;
import chronos.codereviewapi.dtos.responses.RegisterResponse;
import chronos.codereviewapi.services.AuthService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        var user = authService.register(request);

        URI location = URI.create("/api/users/" + user.getId());

        return ResponseEntity
                .created(location)
                .body(RegisterResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }
}
