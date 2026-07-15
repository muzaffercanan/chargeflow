package com.chargesquare.session.auth;

import com.chargesquare.session.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/login")
    @Operation(
            summary = "Log in",
            description = "Public endpoint. Verifies a BCrypt password and issues a short-lived VIEWER or ADMIN JWT.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(value =
                                    "{\"username\":\"admin\",\"password\":\"admin-demo\"}"))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login succeeded",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject(value = """
                                    {"accessToken":"<jwt>","tokenType":"Bearer","expiresIn":900,
                                     "username":"admin","role":"ADMIN"}
                                    """))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid fields",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or disabled user",
                    content = @Content(schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value =
                                    "{\"error\":\"INVALID_CREDENTIALS\",\"message\":\"Invalid username or password\"}")))
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
