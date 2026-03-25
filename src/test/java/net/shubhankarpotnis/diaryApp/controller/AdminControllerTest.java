package net.shubhankarpotnis.diaryApp.controller;

import net.shubhankarpotnis.diaryApp.cache.AppCache;
import net.shubhankarpotnis.diaryApp.entity.User;
import net.shubhankarpotnis.diaryApp.service.UserService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AppCache appCache;

    @InjectMocks
    private AdminController adminController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserName("adminUser");
        testUser.setPassword("encodedPassword");
        testUser.setId(new ObjectId());
        testUser.setRoles(Arrays.asList("USER", "ADMIN"));
    }

    // ---- getAllUsers ----

    @Test  // A. Happy Path
    void getAllUsers_WhenUsersExist_ShouldReturnOkWithUserDtoList() {
        // Arrange
        when(userService.getAllUsers()).thenReturn(Arrays.asList(testUser));

        // Act
        ResponseEntity<?> response = adminController.getAllUsers();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody(), "Response body must contain the DTO list");

        verify(userService, times(1)).getAllUsers();
    }

    @Test  // B. Edge Case: empty list
    void getAllUsers_WhenNoUsersExist_ShouldReturnNotFound() {
        // Arrange
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<?> response = adminController.getAllUsers();

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test  // B. Edge Case: service returns null
    void getAllUsers_WhenServiceReturnsNull_ShouldReturnNotFound() {
        // Arrange
        when(userService.getAllUsers()).thenReturn(null);

        // Act
        ResponseEntity<?> response = adminController.getAllUsers();

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test  // User with null id — toHexString guard in controller
    void getAllUsers_WhenUserHasNullId_ShouldStillReturnOkWithNullIdInDto() {
        User noIdUser = new User();
        noIdUser.setUserName("noIdUser");
        noIdUser.setPassword("pass");
        noIdUser.setRoles(Collections.singletonList("USER"));
        // id is null — controller checks: u.getId() != null ? u.getId().toHexString() : null
        when(userService.getAllUsers()).thenReturn(Collections.singletonList(noIdUser));

        ResponseEntity<?> response = adminController.getAllUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ---- createAdminUser ----

    @Test  // A. Happy Path
    void createAdminUser_WhenValidUser_ShouldReturnCreated() {
        // Arrange
        doNothing().when(userService).saveAdmin(any(User.class));

        // Act
        ResponseEntity<?> response = adminController.createAdminUser(testUser);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(userService, times(1)).saveAdmin(testUser);
    }

    @Test  // B. Edge Case: empty password (service throws IllegalArgumentException)
    @SuppressWarnings("unchecked")
    void createAdminUser_WhenPasswordIsEmpty_ShouldReturnBadRequest() {
        // Arrange
        doThrow(new IllegalArgumentException("Password cannot be empty"))
                .when(userService).saveAdmin(any(User.class));

        // Act
        ResponseEntity<?> response = adminController.createAdminUser(testUser);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals("Password cannot be empty", body.get("error"));
    }

    @Test  // C. Error Case: unexpected DB failure
    @SuppressWarnings("unchecked")
    void createAdminUser_WhenUnexpectedExceptionOccurs_ShouldReturnInternalServerError() {
        // Arrange
        doThrow(new RuntimeException("DB failure"))
                .when(userService).saveAdmin(any(User.class));

        // Act
        ResponseEntity<?> response = adminController.createAdminUser(testUser);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals("Could not create admin user", body.get("error"));
    }

    // ---- clearAppCache ----

    @Test  // A. Happy Path
    @SuppressWarnings("unchecked")
    void clearAppCache_WhenCalled_ShouldCallInitAndReturnOkWithMessage() {
        // Arrange
        doNothing().when(appCache).init();

        // Act
        ResponseEntity<?> response = adminController.clearAppCache();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals("Cache cleared successfully", body.get("message"),
                "Response must confirm the cache was cleared");
        verify(appCache, times(1)).init();
    }
}