
package chronos.codereviewapi.controllers;

import chronos.codereviewapi.dtos.requests.auth.LoginRequest;
import chronos.codereviewapi.dtos.requests.auth.RegisterRequest;
import chronos.codereviewapi.dtos.responses.auth.LoginResponse;
import chronos.codereviewapi.dtos.responses.auth.RegisterResponse;
import chronos.codereviewapi.services.settingsServices.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RegisterResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }
}
