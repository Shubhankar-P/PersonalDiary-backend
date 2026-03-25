package net.shubhankarpotnis.diaryApp.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test  // A. Happy Path: @Valid failure triggers this handler
    void handleValidationErrors_WhenValidationFails_ShouldReturnBadRequestWithMessage() {
        // Arrange
        BindingResult bindingResult = mock(BindingResult.class);
        ObjectError error = new ObjectError("userName", "Username is required");

        when(bindingResult.getAllErrors()).thenReturn(Collections.singletonList(error));

        // Create REAL exception instead of mocking
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        // Act
        ResponseEntity<Map<String, String>> response =
                globalExceptionHandler.handleValidationErrors(ex);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Username is required", response.getBody().get("error"),
                "Error message from the first binding error must be forwarded");
    }

    @Test  // A. Happy Path: explicit throws in service layer trigger this
    void handleIllegalArgument_WhenIllegalArgumentThrown_ShouldReturnBadRequest() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("Password cannot be empty");

        // Act
        ResponseEntity<Map<String, String>> response =
                globalExceptionHandler.handleIllegalArgument(ex);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Password cannot be empty", response.getBody().get("error"));
    }

    @Test  // C. Catch-all handler for unexpected runtime errors
    void handleAllExceptions_WhenUnexpectedThrown_ShouldReturnInternalServerErrorWithGenericMessage() {
        // Arrange — simulates a NullPointerException from deep in the stack
        Exception ex = new RuntimeException("Unexpected DB timeout");

        // Act
        ResponseEntity<Map<String, String>> response =
                globalExceptionHandler.handleAllExceptions(ex);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        // The generic message must NOT expose internal details
        assertEquals("Something went wrong. Please try again.", response.getBody().get("error"),
                "Generic error message must be returned — never expose internals");
    }
}