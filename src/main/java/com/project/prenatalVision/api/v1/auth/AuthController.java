package com.project.prenatalVision.api.v1.auth;

import com.project.prenatalVision.api.configuration.ApiResponse;
import com.project.prenatalVision.application.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthService.AuthResult result = authService.register(
                    request.getEmail(), request.getPassword(), request.getFullName());

            AuthResponse response = AuthResponse.builder()
                    .token(result.token())
                    .email(result.email())
                    .fullName(result.fullName())
                    .build();

            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("BAD_REQUEST", e.getMessage());
        } catch (Exception e) {
            log.error("Registration failed", e);
            return ApiResponse.error("INTERNAL_SERVER_ERROR", "Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthService.AuthResult result = authService.login(
                    request.getEmail(), request.getPassword());

            AuthResponse response = AuthResponse.builder()
                    .token(result.token())
                    .email(result.email())
                    .fullName(result.fullName())
                    .build();

            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("UNAUTHORIZED", e.getMessage());
        } catch (Exception e) {
            log.error("Login failed", e);
            return ApiResponse.error("INTERNAL_SERVER_ERROR", "Login failed: " + e.getMessage());
        }
    }
}
