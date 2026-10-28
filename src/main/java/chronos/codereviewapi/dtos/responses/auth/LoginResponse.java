package chronos.codereviewapi.dtos.responses.auth;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn
) {
}
