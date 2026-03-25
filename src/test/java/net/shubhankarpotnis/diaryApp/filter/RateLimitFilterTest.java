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
class RateLimitFilterTest {

    @InjectMocks
    private RateLimitFilter rateLimitFilter;

    @Test  // A. Happy Path: non-rate-limited path goes straight through
    void doFilterInternal_WhenPathIsNotLoginOrSignup_ShouldAlwaysPassThrough() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/diary/entries");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // Act
        rateLimitFilter.doFilterInternal(request, response, chain);

        // Assert — filter must not touch the response status for unrelated paths
        verify(chain, times(1)).doFilter(request, response);
        assertEquals(200, response.getStatus(),
                "Non-rate-limited paths should not have their status changed");
    }

    @Test  // A. Happy Path: first login request within limit passes
    void doFilterInternal_WhenLoginRequestWithinLimit_ShouldPassThrough() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/public/login");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // Act
        rateLimitFilter.doFilterInternal(request, response, chain);

        // Assert
        verify(chain, times(1)).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test  // A. Happy Path: first signup request within limit passes
    void doFilterInternal_WhenSignupRequestWithinLimit_ShouldPassThrough() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/public/signup");
        request.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // Act
        rateLimitFilter.doFilterInternal(request, response, chain);

        // Assert
        verify(chain, times(1)).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test  // C. Error Case: rate limit exhausted — 11th request from same IP must get 429
    void doFilterInternal_WhenRateLimitExceeded_ShouldReturn429AndNotCallChain()
            throws Exception {
        // Arrange — same IP sends 10 requests first to exhaust the bucket
        String ip = "10.9.9.9";  // unique IP so other tests don't pre-exhaust it
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRequestURI("/public/login");
            req.setRemoteAddr(ip);
            MockHttpServletResponse resp = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(req, resp, chain);
        }

        // 11th request — bucket is now empty
        MockHttpServletRequest eleventh = new MockHttpServletRequest();
        eleventh.setRequestURI("/public/login");
        eleventh.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        rateLimitFilter.doFilterInternal(eleventh, response, chain);

        // Assert
        assertEquals(429, response.getStatus(),
                "11th request from same IP must be rate-limited with HTTP 429");
        // Chain was called only for the first 10, not the 11th
        verify(chain, times(10)).doFilter(any(), any());
    }
}