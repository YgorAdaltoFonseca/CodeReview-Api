package chronos.codereviewapi.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalControllerAdviceTest {

    private final GlobalControllerAdvice advice = new GlobalControllerAdvice();

    @Test
    void invalidCredentialsReturnUnauthorizedWithoutRevealingDetails() {
        var response = advice.handleAuthenticationFailure(
                new BadCredentialsException("private authentication detail")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid email or password.", response.getBody().message());
    }

    @Test
    void duplicateEmailReturnsConflict() {
        var response = advice.handleEmailAlreadyExists(
                new EmailAlreadyExistsException("Email is already registered")
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }
}
