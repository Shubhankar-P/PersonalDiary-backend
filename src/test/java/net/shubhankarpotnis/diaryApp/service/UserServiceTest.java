package net.shubhankarpotnis.diaryApp.service;

import net.shubhankarpotnis.diaryApp.entity.User;
import net.shubhankarpotnis.diaryApp.repository.UserRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private List<User> mockUsers;
    private User testUser;
    private ObjectId testId;

    @BeforeEach
    void setUp() {
      //  testUser = new User("testUser","plainPassword");
        testUser = new User();
        testUser.setUserName("testUser");
        testUser.setPassword("plainPassword");
        ObjectId testId = new ObjectId();
        testUser.setId(testId);

        User user1 = new User();
        user1.setUserName("user1");
        user1.setPassword("pass1");

        User user2 = new User();
        user2.setUserName("user2");
        user2.setPassword("pass2");

        mockUsers = Arrays.asList(user1, user2);
    }


    @Test  // A. Happy Path
    void saveNewUser_WhenCalled_ShouldEncodePasswordSetRoleAndSaveUser() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.saveNewUser(testUser);

        // Assert
        assertNotEquals("plainPassword", testUser.getPassword(), "Password should be encoded");
        assertEquals(1, testUser.getRoles().size(), "Role list should contain exactly one role");
        assertTrue(testUser.getRoles().contains("USER"), "Role should be USER");

        // Verify interaction
        verify(userRepository, times(1)).save(testUser);
    }

    @Test  // B. Edge Case: Empty Password
    void saveNewUser_WhenUserHasEmptyPassword_ShouldStillEncodeAndSave() {
        // Arrange
        testUser.setPassword("");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.saveNewUser(testUser);

        // Assert
        assertNotNull(testUser.getPassword(), "Encoded password should not be null");
        assertNotEquals("", testUser.getPassword(), "Encoded password should not remain empty");
        assertEquals(1, testUser.getRoles().size());
        assertTrue(testUser.getRoles().contains("USER"));

        verify(userRepository, times(1)).save(testUser);
    }

    @Test   // C. Error Case: Repository throws exception
    void saveNewUser_WhenRepositoryThrowsException_ShouldThrowException() {
        // Arrange
        when(userRepository.save(any(User.class)))
                .thenThrow(new RuntimeException("Database save failure"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.saveNewUser(testUser));
        assertEquals("Database save failure", exception.getMessage());

        verify(userRepository, times(1)).save(testUser);
    }




    @Test    // A. Happy Path
    void saveAdmin_WhenCalled_ShouldEncodePasswordSetAdminRoleAndSaveUser() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.saveAdmin(testUser);

        // Assert
        assertNotEquals("plainPassword", testUser.getPassword(), "Password should be encoded");
        assertEquals(2, testUser.getRoles().size(), "Roles list should contain exactly two roles");
        assertTrue(testUser.getRoles().contains("USER"), "Roles should contain USER");
        assertTrue(testUser.getRoles().contains("ADMIN"), "Roles should contain ADMIN");

        // Verify interaction
        verify(userRepository, times(1)).save(testUser);
    }

    @Test  // B. Edge Case: Empty password
    void saveAdmin_WhenPasswordIsEmpty_ShouldThrowException() {
        testUser.setPassword("");

        assertThrows(IllegalArgumentException.class, () -> {
            userService.saveAdmin(testUser);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test   // C. Error Case: Repository throws exception
    void saveAdmin_WhenRepositoryThrowsException_ShouldThrowException() {
        // Arrange
        when(userRepository.save(any(User.class)))
                .thenThrow(new RuntimeException("Database save failure"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.saveAdmin(testUser));
        assertEquals("Database save failure", exception.getMessage());

        verify(userRepository, times(1)).save(testUser);
    }



    @Test
    void saveUser_callsRepositorySave() {
        userService.saveUser(testUser);

        // Verify that userRepository.save(user) was called exactly once
        verify(userRepository, times(1)).save(testUser);
    }



    @Test  //A. happy path
    void getAllUsers_WhenCalled_ReturnsListOfUsers() {
        // Arrange: Prepare mock data
        // Tell Mockito to return mockUsers when findAll() is called
        when(userRepository.findAll()).thenReturn(mockUsers);

        // Act: Call the service method
        List<User> result = userService.getAllUsers();

        // Assert: Verify results
        assertEquals(2, result.size());
        assertEquals("user1", result.get(0).getUserName());
        assertEquals("user2", result.get(1).getUserName());

        // Verify that findAll() was called exactly once
        verify(userRepository, times(1)).findAll();
    }

    @Test  //B. Edge Case
    void getAllUsers_WhenNoUsersExist_ReturnsEmptyList() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findAll();
    }

    @Test  //C. Error Cas
    void getAllUsers_WhenRepositoryThrowsException_ShouldThrowException() {
        // Arrange
        when(userRepository.findAll()).thenThrow(new RuntimeException("DB failure"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.getAllUsers());

        //verify
        verify(userRepository, times(1)).findAll();
    }




    @Test // A. Happy Path
    void findById_WhenUserExists_ReturnsUser() {
        // Arrange
        when(userRepository.findById(testId)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findById(testId);

        // Assert
        assertTrue(result.isPresent(), "User should be found");
        assertEquals("testUser", result.get().getUserName(), "User name should match");

        // Verify interaction
        verify(userRepository, times(1)).findById(testId);
    }

    @Test  // B. Edge Case: User Not Found
    void findById_WhenUserDoesNotExist_ReturnsEmptyOptional() {
        // Arrange
        when(userRepository.findById(testId)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findById(testId);

        // Assert
        assertFalse(result.isPresent(), "No user should be found");

        // Verify interaction
        verify(userRepository, times(1)).findById(testId);
    }

    @Test  // C. Error Case: Repository Throws Exception
    void findById_WhenRepositoryThrowsException_ShouldThrowException() {
        // Arrange
        when(userRepository.findById(testId)).thenThrow(new RuntimeException("DB lookup failure"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.findById(testId));
        assertEquals("DB lookup failure", exception.getMessage());

        verify(userRepository, times(1)).findById(testId);
    }



    @Test
    void deleteById_callsRepositoryDeleteById() {
        userService.deleteById(testId);

        // Verify that userRepository.deleteById(id) was called exactly once
        verify(userRepository, times(1)).deleteById(testId);
    }



    @Test  // A. Happy Path
    void findByUserName_WhenUserExists_ReturnsUser() {
        // Arrange
        when(userRepository.findByUserName("testUser")).thenReturn(testUser);

        // Act
        User result = userService.findByUserName("testUser");

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals("testUser", result.getUserName(), "Username should match");
        assertEquals("plainPassword", result.getPassword(), "Password should match");

        // Verify interaction
        verify(userRepository, times(1)).findByUserName("testUser");
    }

    @Test   // B. Edge Case: User Not Found (returns null)
    void findByUserName_WhenUserDoesNotExist_ReturnsNull() {
        // Arrange
        when(userRepository.findByUserName("nonExistentUser")).thenReturn(null);

        // Act
        User result = userService.findByUserName("nonExistentUser");

        // Assert
        assertNull(result, "Result should be null when user is not found");

        // Verify interaction
        verify(userRepository, times(1)).findByUserName("nonExistentUser");
    }

    @Test  // C. Error Case: Repository Throws Exception
    void findByUserName_WhenRepositoryThrowsException_ShouldThrowException() {
        // Arrange
        when(userRepository.findByUserName("testUser"))
                .thenThrow(new RuntimeException("Database lookup failure"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.findByUserName("testUser"));
        assertEquals("Database lookup failure", exception.getMessage());

        // Verify interaction
        verify(userRepository, times(1)).findByUserName("testUser");
    }
}