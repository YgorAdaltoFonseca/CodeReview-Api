package chronos.codereviewapi.dtos.responses;

public record LoginResponse(
        String accessToken,
        String tokenType
) {}
