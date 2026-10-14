package chronos.codereviewapi.dtos;

public record LoginResponse(
        String accessToken,
        String tokenType
) {}
