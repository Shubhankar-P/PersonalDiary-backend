package net.shubhankarpotnis.diaryApp.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityHeadersFilterTest {

    @InjectMocks
    private SecurityHeadersFilter securityHeadersFilter;

    @Test  // A. Happy Path: all five headers are set with correct values
    void doFilterInternal_ShouldSetAllSecurityHeadersCorrectly() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // Act
        securityHeadersFilter.doFilterInternal(request, response, chain);

        // Assert — each header must be set exactly as configured in the filter
        assertEquals("nosniff",
                response.getHeader("X-Content-Type-Options"),
                "X-Content-Type-Options must be 'nosniff' to prevent MIME sniffing");
        assertEquals("DENY",
                response.getHeader("X-Frame-Options"),
                "X-Frame-Options must be 'DENY' to block clickjacking");
        assertEquals("1; mode=block",
                response.getHeader("X-XSS-Protection"),
                "X-XSS-Protection must be '1; mode=block'");
        assertEquals("strict-origin-when-cross-origin",
                response.getHeader("Referrer-Policy"),
                "Referrer-Policy must be 'strict-origin-when-cross-origin'");
        assertEquals("geolocation=(), microphone=(), camera=()",
                response.getHeader("Permissions-Policy"),
                "Permissions-Policy must restrict geolocation, microphone, and camera");

        // Filter must always call the chain — security headers don't stop the request
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test  // B. Filter chain is called regardless of request content
    void doFilterInternal_ShouldAlwaysCallFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        securityHeadersFilter.doFilterInternal(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
    }
}