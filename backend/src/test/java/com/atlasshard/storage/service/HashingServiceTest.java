package com.atlasshard.storage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class HashingServiceTest {
    @Test
    void computesKnownSha256() throws Exception {
        HashingService hashing = new HashingService();
        Path file = Files.createTempFile("atlasshard-hash", ".txt");
        try {
            Files.writeString(file, "abc");
            assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                hashing.sha256(file));
            assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                hashing.sha256Text("abc"));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
