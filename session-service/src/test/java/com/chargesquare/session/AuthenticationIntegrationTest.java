package com.chargesquare.session;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtEncoder jwtEncoder;

    @AfterEach
    void enableDemoUsers() {
        jdbcTemplate.update("update session.auth_users set enabled = true");
    }

    @Test
    void adminCanLoginAndUseTheIssuedTokenForReads() throws Exception {
        JsonNode login = login("admin", "admin-demo", 200);

        mockMvc.perform(get("/users/7/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login.path("accessToken").asText()))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(login.path("tokenType").asText()).isEqualTo("Bearer");
        org.assertj.core.api.Assertions.assertThat(login.path("role").asText()).isEqualTo("ADMIN");
        org.assertj.core.api.Assertions.assertThat(login.path("expiresIn").asLong()).isPositive();
    }

    @Test
    void viewerCanLoginAndUseTheIssuedTokenForReads() throws Exception {
        JsonNode login = login("viewer", "viewer-demo", 200);

        mockMvc.perform(get("/users/7/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login.path("accessToken").asText()))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(login.path("role").asText()).isEqualTo("VIEWER");
    }

    @Test
    void badCredentialsReturnTheSameUnauthorizedError() throws Exception {
        login("admin", "wrong-password", 401);
        login("missing-user", "wrong-password", 401);
    }

    @Test
    void disabledUserCannotLogin() throws Exception {
        jdbcTemplate.update("update session.auth_users set enabled = false where username = 'viewer'");

        login("viewer", "viewer-demo", 401);
    }

    @Test
    void invalidAndExpiredTokensReturnJsonUnauthorizedResponses() throws Exception {
        mockMvc.perform(get("/users/7/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/users/7/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void viewerTokenCannotStartOrStopSessions() throws Exception {
        String viewerToken = login("viewer", "viewer-demo", 200).path("accessToken").asText();

        mockMvc.perform(post("/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + viewerToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"userId\":7,\"connectorId\":10}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));

        mockMvc.perform(post("/sessions/1/stop")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + viewerToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"energyKwh\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    private JsonNode login(String username, String password, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}"
                                .formatted(username, password)))
                .andExpect(status().is(expectedStatus))
                .andExpect(expectedStatus == 200
                        ? jsonPath("$.username").value(username)
                        : jsonPath("$.error").value("INVALID_CREDENTIALS"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String expiredToken() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("chargesquare-session-service")
                .audience(List.of("chargesquare-api"))
                .subject("admin")
                .issuedAt(now.minusSeconds(120))
                .expiresAt(now.minusSeconds(60))
                .claim("role", "ADMIN")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
