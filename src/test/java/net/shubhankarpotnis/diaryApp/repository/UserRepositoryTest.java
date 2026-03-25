package net.shubhankarpotnis.diaryApp.repository;

import net.shubhankarpotnis.diaryApp.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setup() {
        // Clear test database before each test
        userRepository.deleteAll();

        User testUser = new User();
        testUser.setUserName("testUserEEEE");
        testUser.setPassword("testPassEEEE");
        userRepository.save(testUser);
    }

    @Test
    public void findByUserName_WhenUserExists_ReturnsUser() {
        User user = userRepository.findByUserName("testUserEEEE");
        assertNotNull(user);
        assertEquals("testUserEEEE", user.getUserName());
    }

    @Test
    void deleteByUserName_WhenCalled_RemovesUser() {
        userRepository.deleteByUserName("testUserEEEE");
        User user = userRepository.findByUserName("testUserEEEE");
        assertNull(user);
    }
}
