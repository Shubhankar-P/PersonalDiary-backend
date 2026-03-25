package net.shubhankarpotnis.diaryApp.filter;

import net.shubhankarpotnis.diaryApp.utilis.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private JwtFilter jwtFilter;

    @AfterEach
    void tearDown() {
        // Critical: prevents authentication state from leaking between tests
        SecurityContextHolder.clearContext();
    }

    @Test  // A. Happy Path: valid Bearer token → authentication set
    void doFilterInternal_WhenValidBearerToken_ShouldSetAuthenticationInSecurityContext()
            throws Exception {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());
        when(userDetails.getUsername()).thenReturn("testUser");

        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
        when(jwtUtil.extractUsername("valid.jwt.token")).thenReturn("testUser");
        when(userDetailsService.loadUserByUsername("testUser")).thenReturn(userDetails);
        when(jwtUtil.validateToken("valid.jwt.token")).thenReturn(true);

        // Act
        jwtFilter.doFilterInternal(request, response, chain);

        // Assert
        assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                "Authentication must be populated in the context for a valid token");
        assertEquals("testUser",
                SecurityContextHolder.getContext().getAuthentication().getName());
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test  // B. Edge Case: No Authorization header at all
    void doFilterInternal_WhenNoAuthorizationHeader_ShouldNotAuthenticate() throws Exception {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        jwtFilter.doFilterInternal(request, response, chain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "No authentication should be set when there is no Authorization header");
        verify(jwtUtil, never()).extractUsername(any());
        verify(chain, times(1)).doFilter(request, response); // chain always continues
    }

    @Test  // B. Edge Case: Header present but not a Bearer scheme (e.g. Basic auth)
    void doFilterInternal_WhenNonBearerHeader_ShouldSkipTokenProcessing() throws Exception {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        // Act
        jwtFilter.doFilterInternal(request, response, chain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtil, never()).extractUsername(any());
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test  // C. Error Case: token present but validateToken returns false (expired/tampered)
    void doFilterInternal_WhenTokenFailsValidation_ShouldNotAuthenticate() throws Exception {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        UserDetails userDetails = mock(UserDetails.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer expired.token.here");
        when(jwtUtil.extractUsername("expired.token.here")).thenReturn("testUser");
        when(userDetailsService.loadUserByUsername("testUser")).thenReturn(userDetails);
        when(jwtUtil.validateToken("expired.token.here")).thenReturn(false);

        // Act
        jwtFilter.doFilterInternal(request, response, chain);

        // Assert — authentication must NOT be set for an invalid token
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "An invalid token must not result in an authenticated context");
        verify(chain, times(1)).doFilter(request, response);
    }
}