package com.chargesquare.session.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$12$ZeFOjiE.MkSC3hwCbx20AOuEdGnJKcHJInZJkdlmksPvsiCcz3JSC";

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            AuthUserRepository authUserRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim();
        AuthUser user = authUserRepository.findByUsername(username).orElse(null);
        String passwordHash = user == null ? DUMMY_PASSWORD_HASH : user.getPasswordHash();
        boolean passwordMatches = passwordEncoder.matches(request.password(), passwordHash);
        if (user == null || !user.isEnabled() || !passwordMatches) {
            LOGGER.warn("event=login_failed username={} reason=invalid_credentials", safeForLog(username));
            throw new InvalidCredentialsException();
        }

        JwtTokenService.IssuedAccessToken issuedToken = jwtTokenService.issueAccessToken(user);
        LOGGER.info("event=login_success actor={} role={}", user.getUsername(), user.getRole());
        return new LoginResponse(
                issuedToken.value(),
                "Bearer",
                issuedToken.expiresInSeconds(),
                user.getUsername(),
                user.getRole());
    }

    private String safeForLog(String value) {
        return value.replaceAll("[\\r\\n\\t]", "_");
    }
}
