package net.shubhankarpotnis.diaryApp.controller;

import net.shubhankarpotnis.diaryApp.entity.User;
import net.shubhankarpotnis.diaryApp.service.UserDetailsServiceImpl;
import net.shubhankarpotnis.diaryApp.service.UserService;
import net.shubhankarpotnis.diaryApp.utilis.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private PublicController publicController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserName("testUser");
        testUser.setPassword("password123");
    }

    // ---- healthCheck ----

    @Test  // A. Happy Path
    void healthCheck_ShouldReturnSingletonMapWithStatusOk() {
        Map<String, String> result = publicController.healthCheck();

        assertNotNull(result);
        assertEquals("OK", result.get("status"), "Health check must always return status=OK");
    }

    // ---- signup ----

    @Test  // A. Happy Path
    void signup_WhenValidUser_ShouldCallServiceAndReturnCreated() {
        // Arrange
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        doNothing().when(userService).saveNewUser(any(User.class));

        // Act
        ResponseEntity<?> response = publicController.signup(testUser, bindingResult);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(userService, times(1)).saveNewUser(testUser);
    }

    @Test  // B. Edge Case: @Valid triggers BindingResult errors
    @SuppressWarnings("unchecked")
    void signup_WhenBindingErrors_ShouldReturnBadRequestWithFirstErrorMessage() {
        // Arrange
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getAllErrors()).thenReturn(
                Collections.singletonList(new ObjectError("userName", "Username is required")));

        // Act
        ResponseEntity<?> response = publicController.signup(testUser, bindingResult);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals("Username is required", body.get("error"));
        // Service must NOT be called when validation fails
        verify(userService, never()).saveNewUser(any());
    }

    @Test  // C. Error Case: username already taken
    @SuppressWarnings("unchecked")
    void signup_WhenUsernameAlreadyTaken_ShouldReturnConflict() {
        // Arrange — MongoDB duplicate key exception propagates as a generic RuntimeException
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new RuntimeException("Duplicate key")).when(userService).saveNewUser(any(User.class));

        // Act
        ResponseEntity<?> response = publicController.signup(testUser, bindingResult);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals("Username already taken", body.get("error"));
    }

    // ---- login ----

    @Test  // A. Happy Path
    void login_WhenCredentialsAreValid_ShouldAuthenticateAndReturnJwt() {
        // Arrange
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("testUser");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // return value is ignored in the controller
        when(userDetailsService.loadUserByUsername("testUser")).thenReturn(userDetails);
        when(jwtUtil.generateToken("testUser")).thenReturn("signed.jwt.token");

        // Act
        ResponseEntity<?> response = publicController.login(testUser);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("signed.jwt.token", response.getBody(),
                "JWT string must be returned in the body on successful login");
    }

    @Test  // C. Error Case: wrong password
    @SuppressWarnings("unchecked")
    void login_WhenCredentialsAreInvalid_ShouldReturnUnauthorized() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act
        ResponseEntity<?> response = publicController.login(testUser);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals("Incorrect username or password", body.get("error"),
                "Error message must NOT reveal whether the username or the password was wrong");
    }
}