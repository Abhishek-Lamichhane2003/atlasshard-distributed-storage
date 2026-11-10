package com.atlasshard.storage.controller;

import com.atlasshard.storage.config.AppProperties;
import com.atlasshard.storage.model.UserAccount;
import com.atlasshard.storage.repo.UserRepository;
import com.atlasshard.storage.security.JwtFilter;
import com.atlasshard.storage.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    record Credentials(@Email @NotBlank String email, @NotBlank @Size(min = 8, max = 128) String password) {}

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AppProperties props;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt, AppProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.props = props;
    }

    @PostMapping("/register")
    public Map<String, String> register(@Valid @RequestBody Credentials credentials, HttpServletResponse response) {
        String email = credentials.email().trim().toLowerCase();
        if (users.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email exists");
        }
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(credentials.password()));
        users.save(user);
        setSessionCookie(response, jwt.create(user.getId(), user.getEmail()));
        return Map.of("email", user.getEmail());
    }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody Credentials credentials, HttpServletResponse response) {
        String email = credentials.email().trim().toLowerCase();
        UserAccount user = users.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials"));
        if (!encoder.matches(credentials.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
        }
        setSessionCookie(response, jwt.create(user.getId(), user.getEmail()));
        return Map.of("email", user.getEmail());
    }

    @GetMapping("/me")
    public Map<String, String> me(Authentication authentication) {
        UserAccount user = users.findById(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return Map.of("email", user.getEmail());
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(JwtFilter.COOKIE_NAME, "")
            .httpOnly(true)
            .secure(props.cookieSecure())
            .sameSite("Strict")
            .path("/")
            .maxAge(Duration.ZERO)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void setSessionCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(JwtFilter.COOKIE_NAME, token)
            .httpOnly(true)
            .secure(props.cookieSecure())
            .sameSite("Strict")
            .path("/")
            .maxAge(Duration.ofDays(1))
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
