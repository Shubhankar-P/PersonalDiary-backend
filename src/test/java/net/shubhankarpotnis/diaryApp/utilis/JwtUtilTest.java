package net.shubhankarpotnis.diaryApp.utilis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    // Must be ≥ 32 characters (256 bits) — JJWT requirement for HMAC-SHA
    private static final String TEST_SECRET =
            "test-secret-key-for-jwt-tests-that-is-at-least-256-bits-long";
    private static final long TEST_EXPIRY_MS = 900_000L; // 15 minutes

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "SECRET_KEY", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "ACCESS_EXPIRY_MS", TEST_EXPIRY_MS);
    }

    @Test  // A. Happy Path
    void generateToken_WhenCalled_ShouldReturnNonEmptyToken() {
        String token = jwtUtil.generateToken("testUser");

        assertNotNull(token, "Token should not be null");
        assertFalse(token.isEmpty(), "Token should not be empty");
        // JWT structure: three Base64url segments separated by dots
        assertEquals(3, token.split("\\.").length, "Token should have 3 JWT segments");
    }

    @Test  // A. Happy Path
    void extractUsername_WhenValidToken_ShouldReturnCorrectUsername() {
        String token = jwtUtil.generateToken("testUser");

        String extracted = jwtUtil.extractUsername(token);

        assertEquals("testUser", extracted, "Extracted username should match the original");
    }

    @Test  // A. Happy Path
    void extractExpiration_WhenValidToken_ShouldReturnFutureDate() {
        String token = jwtUtil.generateToken("testUser");

        Date expiry = jwtUtil.extractExpiration(token);

        assertNotNull(expiry, "Expiry date should not be null");
        assertTrue(expiry.after(new Date()), "Expiry date should be in the future");
    }

    @Test  // A. Happy Path
    void validateToken_WhenTokenIsFresh_ShouldReturnTrue() {
        String token = jwtUtil.generateToken("testUser");

        Boolean valid = jwtUtil.validateToken(token);

        assertTrue(valid, "A freshly generated token should be valid");
    }

    @Test  // B. Edge Case: Expired token
    void validateToken_WhenTokenIsExpired_ShouldThrowException() {
        // Set expiry to -1ms so the token is already expired at creation time
        ReflectionTestUtils.setField(jwtUtil, "ACCESS_EXPIRY_MS", -1L);
        String expiredToken = jwtUtil.generateToken("testUser");

        // JJWT throws ExpiredJwtException (a RuntimeException subtype) on parsing
        assertThrows(Exception.class, () -> jwtUtil.validateToken(expiredToken),
                "Validating an expired token should throw an exception");
    }

    @Test  // C. Error Case: Malformed token
    void extractUsername_WhenTokenIsGibberish_ShouldThrowException() {
        assertThrows(Exception.class,
                () -> jwtUtil.extractUsername("this.is.not.a.jwt"),
                "Parsing a malformed token should throw");
    }

    @Test  // Edge Case: Different users produce different tokens
    void generateToken_ForDifferentUsers_ShouldReturnDifferentTokens() {
        String tokenA = jwtUtil.generateToken("userA");
        String tokenB = jwtUtil.generateToken("userB");

        assertNotEquals(tokenA, tokenB, "Tokens for different users must differ");
    }
}