package net.shubhankarpotnis.diaryApp.controller;

import net.shubhankarpotnis.diaryApp.apiResponse.QuoteApiResponse;
import net.shubhankarpotnis.diaryApp.entity.User;
import net.shubhankarpotnis.diaryApp.repository.UserRepository;
import net.shubhankarpotnis.diaryApp.service.QuotesService;
import net.shubhankarpotnis.diaryApp.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuotesService quotesService;

    @InjectMocks
    private UserController userController;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        // Set up a real SecurityContext so SecurityContextHolder.getContext().getAuthentication()
        // works without requiring mockStatic
        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("testUser");

        SecurityContext context = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---- updateUser ----

    @Test  // A. Happy Path
    void updateUser_WhenCalled_ShouldUpdateFieldsAndReturnNoContent() {
        // Arrange
        User newData = new User();
        newData.setUserName("updatedName");
        newData.setPassword("newPass");

        User existingUser = new User();
        existingUser.setUserName("testUser");
        existingUser.setPassword("oldEncodedPass");

        when(userService.findByUserName("testUser")).thenReturn(existingUser);
        doNothing().when(userService).saveNewUser(existingUser);

        // Act
        ResponseEntity<?> response = userController.updateUser(newData);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        // Controller sets new values on existingUser before calling saveNewUser
        assertEquals("updatedName", existingUser.getUserName(),
                "Username must be updated to the new value");
        assertEquals("newPass", existingUser.getPassword(),
                "Password is set here (encoding happens inside saveNewUser)");

        verify(userService, times(1)).findByUserName("testUser");
        verify(userService, times(1)).saveNewUser(existingUser);
    }

    // ---- deleteUserById ----

    @Test  // A. Happy Path
    void deleteUserById_WhenCalled_ShouldDeleteByUsernameAndReturnNoContent() {
        // Arrange
        doNothing().when(userRepository).deleteByUserName("testUser");

        // Act
        ResponseEntity<?> response = userController.deleteUserById();

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userRepository, times(1)).deleteByUserName("testUser");
    }

    // ---- greeting ----

    @Test  // A. Happy Path: quote has a named author
    void greeting_WhenQuoteHasAuthor_ShouldReturnFormattedGreetingWithAuthor() {
        // Arrange
        QuoteApiResponse quote = new QuoteApiResponse();
        quote.setQuoteText("The journey of a thousand miles begins with one step.");
        quote.setQuoteAuthor("Lao Tzu");
        when(quotesService.getQuote()).thenReturn(quote);

        // Act — controller takes Authentication directly as a method parameter
        String result = userController.greeting(authentication);

        // Assert
        assertTrue(result.contains("Hi testUser"), "Greeting must address the authenticated user");
        assertTrue(result.contains("The journey of a thousand miles"),
                "Quote text must be present");
        assertTrue(result.contains("Lao Tzu"), "Named author must be shown");
        assertFalse(result.contains("Unknown"), "Should not say 'Unknown' when author is provided");
    }

    @Test  // B. Edge Case: author field is an empty string
    void greeting_WhenQuoteAuthorIsEmpty_ShouldFallbackToUnknown() {
        // Arrange
        QuoteApiResponse quote = new QuoteApiResponse();
        quote.setQuoteText("Some quote.");
        quote.setQuoteAuthor("");  // empty string — the null-or-empty guard in controller must fire
        when(quotesService.getQuote()).thenReturn(quote);

        // Act
        String result = userController.greeting(authentication);

        // Assert
        assertTrue(result.contains("Unknown"),
                "Empty author string must be replaced with 'Unknown'");
    }

    @Test  // B. Edge Case: author field is null
    void greeting_WhenQuoteAuthorIsNull_ShouldFallbackToUnknown() {
        // Arrange
        QuoteApiResponse quote = new QuoteApiResponse();
        quote.setQuoteText("Another quote.");
        quote.setQuoteAuthor(null);  // null — the null check must fire
        when(quotesService.getQuote()).thenReturn(quote);

        // Act
        String result = userController.greeting(authentication);

        // Assert
        assertTrue(result.contains("Unknown"),
                "Null author must be replaced with 'Unknown'");
    }
}