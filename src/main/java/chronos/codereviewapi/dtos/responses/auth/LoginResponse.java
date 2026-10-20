package chronos.codereviewapi.dtos.responses.auth;

public record LoginResponse(
        String accessToken,
        String tokenType
) {}
