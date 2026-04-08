package com.example.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "test-secret-key-that-is-at-least-32-bytes-long-for-hmac");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L);
    }

    @Test
    void generateToken_returnsNonEmptyString() {
        String token = jwtService.generateToken("user@test.com", "USER");

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String token = jwtService.generateToken("user@test.com", "USER");

        assertEquals("user@test.com", jwtService.extractEmail(token));
    }

    @Test
    void extractRole_returnsCorrectRole() {
        String token = jwtService.generateToken("user@test.com", "ADMIN");

        assertEquals("ADMIN", jwtService.extractRole(token));
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtService.generateToken("user@test.com", "USER");

        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        // создаём сервис с нулевым временем жизни
        JwtService expiredService = new JwtService();
        ReflectionTestUtils.setField(expiredService, "secret",
                "test-secret-key-that-is-at-least-32-bytes-long-for-hmac");
        ReflectionTestUtils.setField(expiredService, "expirationMs", 0L);

        String token = expiredService.generateToken("user@test.com", "USER");

        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_garbageString_returnsFalse() {
        assertFalse(jwtService.isTokenValid("not-a-valid-token"));
    }
}