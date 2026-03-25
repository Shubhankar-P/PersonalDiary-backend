package net.shubhankarpotnis.diaryApp.service;

import net.shubhankarpotnis.diaryApp.apiResponse.QuoteApiResponse;
import net.shubhankarpotnis.diaryApp.cache.AppCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuotesServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AppCache appCache;

    @InjectMocks
    private QuotesService quotesService;

    private static final String QUOTES_URL = "https://api.forismatic.com/api/1.0/";

    @BeforeEach
    void setUp() {
        // Directly populate the public cache map — mirrors what AppCache.init() does at runtime
        Map<String, String> cache = new HashMap<>();
        cache.put("quotes_api", QUOTES_URL);
        appCache.APP_CACHE = cache;
    }

    @Test  // A. Happy Path
    void getQuote_WhenApiRespondsSuccessfully_ShouldReturnPopulatedQuote() {
        // Arrange
        QuoteApiResponse expected = new QuoteApiResponse();
        expected.setQuoteText("To be or not to be.");
        expected.setQuoteAuthor("Shakespeare");

        when(restTemplate.getForObject(QUOTES_URL, QuoteApiResponse.class))
                .thenReturn(expected);

        // Act
        QuoteApiResponse result = quotesService.getQuote();

        // Assert
        assertNotNull(result, "Quote response should not be null");
        assertEquals("To be or not to be.", result.getQuoteText());
        assertEquals("Shakespeare", result.getQuoteAuthor());

        verify(restTemplate, times(1)).getForObject(QUOTES_URL, QuoteApiResponse.class);
    }

    @Test  // B. Edge Case: API returns null (network timeout, empty body)
    void getQuote_WhenApiReturnsNull_ShouldReturnNull() {
        // Arrange
        when(restTemplate.getForObject(QUOTES_URL, QuoteApiResponse.class)).thenReturn(null);

        // Act
        QuoteApiResponse result = quotesService.getQuote();

        // Assert
        assertNull(result, "A null API response should be passed through as-is");
        verify(restTemplate, times(1)).getForObject(QUOTES_URL, QuoteApiResponse.class);
    }

    @Test  // C. Error Case: RestTemplate throws (service down, DNS failure, etc.)
    void getQuote_WhenRestTemplateThrows_ShouldPropagateException() {
        // Arrange
        when(restTemplate.getForObject(QUOTES_URL, QuoteApiResponse.class))
                .thenThrow(new RuntimeException("External API unreachable"));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> quotesService.getQuote());
        assertEquals("External API unreachable", ex.getMessage());

        verify(restTemplate, times(1)).getForObject(QUOTES_URL, QuoteApiResponse.class);
    }
}