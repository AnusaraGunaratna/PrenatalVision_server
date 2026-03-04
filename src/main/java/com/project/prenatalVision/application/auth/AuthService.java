package com.project.prenatalVision.application.auth;

import com.project.prenatalVision.domain.user.User;
import com.project.prenatalVision.infrastructure.persistence.UserRepository;
import com.project.prenatalVision.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResult register(String email, String password, String fullName) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName)
                .build();

        userRepository.save(user);
        log.info("User registered: {}", email);

        String token = jwtService.generateToken(email);
        return new AuthResult(token, email, fullName);
    }

    public AuthResult login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        log.info("User logged in: {}", email);
        String token = jwtService.generateToken(email);
        return new AuthResult(token, email, user.getFullName());
    }

    public record AuthResult(String token, String email, String fullName) {}
}
