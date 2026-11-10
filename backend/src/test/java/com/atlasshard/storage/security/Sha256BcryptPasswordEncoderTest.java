package com.atlasshard.storage.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Sha256BcryptPasswordEncoderTest {
    @Test
    void supportsLongPasswordsWithoutBcryptTruncation() {
        Sha256BcryptPasswordEncoder encoder = new Sha256BcryptPasswordEncoder();
        String password = "a".repeat(200) + "-unique-tail";
        String encoded = encoder.encode(password);
        assertTrue(encoder.matches(password, encoded));
        assertFalse(encoder.matches("a".repeat(200) + "-different-tail", encoded));
    }
}
