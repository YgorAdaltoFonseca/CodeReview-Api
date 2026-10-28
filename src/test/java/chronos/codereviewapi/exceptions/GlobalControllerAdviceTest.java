package chronos.codereviewapi.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.server.ResponseStatusException;

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
    void responseStatusExceptionPreservesItsHttpStatusAndSafeReason() {
        var response = advice.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.")
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found.", response.getBody().message());
    }

    @Test
    void duplicateEmailReturnsConflict() {
        var response = advice.handleEmailAlreadyExists(
                new EmailAlreadyExistsException("Email is already registered")
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }
}
