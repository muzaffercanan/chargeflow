package com.chargesquare.session.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenService implements ServiceTokenProvider {

    private final JwtEncoder jwtEncoder;
    private final Duration accessTokenTtl;
    private final Duration serviceTokenTtl;
    private final String issuer;
    private final String audience;
    private final Clock clock;

    @Autowired
    public JwtTokenService(
            JwtEncoder jwtEncoder,
            @Value("${security.jwt.access-token-ttl}") Duration accessTokenTtl,
            @Value("${security.jwt.service-token-ttl}") Duration serviceTokenTtl,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.audience}") String audience) {
        this(jwtEncoder, accessTokenTtl, serviceTokenTtl, issuer, audience, Clock.systemUTC());
    }

    JwtTokenService(
            JwtEncoder jwtEncoder,
            Duration accessTokenTtl,
            Duration serviceTokenTtl,
            String issuer,
            String audience,
            Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.accessTokenTtl = accessTokenTtl;
        this.serviceTokenTtl = serviceTokenTtl;
        this.issuer = issuer;
        this.audience = audience;
        this.clock = clock;
    }

    public IssuedAccessToken issueAccessToken(AuthUser user) {
        String token = encode(user.getUsername(), user.getRole().name(), accessTokenTtl, "access");
        return new IssuedAccessToken(token, accessTokenTtl.toSeconds());
    }

    @Override
    public String issueServiceToken() {
        return encode("session-service", "SERVICE", serviceTokenTtl, "service");
    }

    private String encode(String subject, String role, Duration ttl, String tokenType) {
        Instant issuedAt = Instant.now(clock);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(ttl))
                .claim("role", role)
                .claim("tokenType", tokenType)
                .build();
        JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
    }

    public record IssuedAccessToken(String value, long expiresInSeconds) {
    }
}
