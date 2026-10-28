package chronos.codereviewapi.services.settingsServices;

import chronos.codereviewapi.entities.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class JwtService {

    public static final long TOKEN_EXPIRATION_SECONDS = 900;

    private final JwtEncoder jwtEncoder;

    public String generateToken(UserEntity user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("codereview-api")
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_EXPIRATION_SECONDS, ChronoUnit.SECONDS))
                .claim("userId", user.getId())
                .claim(
                        "role",
                        user.getAuthorities()
                                .stream()
                                .findFirst()
                                .orElseThrow(() ->
                                        new IllegalStateException("User has no authority"))
                                .getAuthority()
                                .replace("ROLE_", "")
                )
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }
}
