package net.shubhankarpotnis.diaryApp.service;

import net.shubhankarpotnis.diaryApp.entity.DiaryEntry;
import net.shubhankarpotnis.diaryApp.entity.User;
import net.shubhankarpotnis.diaryApp.repository.DiaryEntryRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiaryEntryServiceTest {

    @Mock
    private DiaryEntryRepository diaryEntryRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private DiaryEntryService diaryEntryService;

    private User testUser;
    private DiaryEntry testDiaryEntry;
    ObjectId objectId;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testDiaryEntry = new DiaryEntry();
        testDiaryEntry.setTitle("My test Diary");
        testDiaryEntry.setContent("Some content");
        testDiaryEntry.setId(new ObjectId());

    }

    @Test  // A. Happy Path
    void saveEntry_WhenCalled_SavesDiaryEntryAndUpdatesUser() {
        // Arrange
        when(userService.findByUserName("testUser")).thenReturn(testUser);
        when(diaryEntryRepository.save(any(DiaryEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        diaryEntryService.saveEntry(testDiaryEntry, "testUser");

        // Assert
        assertNotNull(testDiaryEntry.getDate(), "DiaryEntry should have a set date");
        assertTrue(testUser.getDiaryEntries().contains(testDiaryEntry), "User's diary entries should contain the saved entry");

        // Verify interactions
        verify(userService, times(1)).findByUserName("testUser");
        verify(diaryEntryRepository, times(1)).save(testDiaryEntry);
        verify(userService, times(1)).saveUser(testUser);
    }

    @Test    // B. Edge Case: Empty DiaryEntry Title
    void saveEntry_WhenDiaryEntryTitleIsEmpty_ShouldStillSaveNormally() {
        // Arrange
        testDiaryEntry.setTitle("");
        when(userService.findByUserName("testUser")).thenReturn(testUser);
        when(diaryEntryRepository.save(any(DiaryEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        diaryEntryService.saveEntry(testDiaryEntry, "testUser");

        // Assert
        assertNotNull(testDiaryEntry.getDate(), "Date should be set even if title is empty");
        assertTrue(testUser.getDiaryEntries().contains(testDiaryEntry), "Diary entry should still be saved");

        verify(userService, times(1)).findByUserName("testUser");
        verify(diaryEntryRepository, times(1)).save(testDiaryEntry);
        verify(userService, times(1)).saveUser(testUser);
    }

    @Test   // C. Error Case: userService.findByUserName Throws Exception
    void saveEntry_WhenUserServiceThrowsException_ShouldNotPropagateButLogError() {
        // Arrange
        when(userService.findByUserName("testUser")).thenThrow(new RuntimeException("User lookup failed"));

        // Act & Assert
        assertDoesNotThrow(() -> diaryEntryService.saveEntry(testDiaryEntry, "testUser"));

        // Verify that save was never called because of exception
        verify(diaryEntryRepository, never()).save(any(DiaryEntry.class));
        verify(userService, never()).saveUser(any(User.class));
    }

    @Test  // A. Happy Path: Direct save (used by update endpoint)
    void saveEntry_WhenCalledWithJustEntry_ShouldDelegateDirectlyToRepository() {
        // Arrange
        when(diaryEntryRepository.save(testDiaryEntry)).thenReturn(testDiaryEntry);

        // Act
        diaryEntryService.saveEntry(testDiaryEntry);

        // Assert — repository.save must be called; no user lookup should happen
        verify(diaryEntryRepository, times(1)).save(testDiaryEntry);
        verify(userService, never()).findByUserName(any());
    }

    @Test  // A. Happy Path: findById returns entry
    void findById_WhenEntryExists_ShouldReturnDiaryEntry() {
        // Arrange
        when(diaryEntryRepository.findById(testDiaryEntry.getId())).thenReturn(Optional.of(testDiaryEntry));

        // Act
        Optional<DiaryEntry> result = diaryEntryService.findById(testDiaryEntry.getId());

        // Assert
        assertTrue(result.isPresent(), "Result should be present");
        assertEquals(testDiaryEntry, result.get(), "Result should match the entry returned by repository");

        // Verify
        verify(diaryEntryRepository, times(1)).findById(testDiaryEntry.getId());
    }

    @Test  // B. Edge Case: findById returns empty Optional
    void findById_WhenEntryDoesNotExist_ShouldReturnEmptyOptional() {
        // Arrange
        ObjectId id = new ObjectId();
        when(diaryEntryRepository.findById(id)).thenReturn(Optional.empty());

        // Act
        Optional<DiaryEntry> result = diaryEntryService.findById(id);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isPresent(), "Result should be empty if entry not found");

        // Verify
        verify(diaryEntryRepository, times(1)).findById(id);
    }

    @Test  // C. Error Case: repository throws exception
    void findById_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Arrange
        ObjectId id = new ObjectId();
        when(diaryEntryRepository.findById(id)).thenThrow(new RuntimeException("DB error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> diaryEntryService.findById(id));
        assertEquals("DB error", exception.getMessage(), "Exception message should match");

        // Verify
        verify(diaryEntryRepository, times(1)).findById(id);
    }




    @Test
    void deleteById_WhenEntryExists_ShouldRemoveEntryAndReturnTrue() {
        // Arrange
        ObjectId id = testDiaryEntry.getId();
        testUser.getDiaryEntries().add(testDiaryEntry);

        when(userService.findByUserName("testUser")).thenReturn(testUser);

        // Act
        boolean result = diaryEntryService.deleteById(id, "testUser");

        // Assert
        assertTrue(result, "deleteById should return true when entry is removed");
        assertFalse(testUser.getDiaryEntries().contains(testDiaryEntry), "User's diary entries should no longer contain the deleted entry");

        // Verify interactions
        verify(userService, times(1)).findByUserName("testUser");
        verify(userService, times(1)).saveUser(testUser);
        verify(diaryEntryRepository, times(1)).deleteById(id);
    }

    @Test
    void deleteById_WhenEntryDoesNotExist_ShouldReturnFalseAndNotCallDelete() {
        // Arrange
        ObjectId id = new ObjectId(); // not in user's diary entries
        when(userService.findByUserName("testUser")).thenReturn(testUser);

        // Act
        boolean result = diaryEntryService.deleteById(id, "testUser");

        // Assert
        assertFalse(result, "deleteById should return false when entry not found");

        // Verify interactions
        verify(userService, times(1)).findByUserName("testUser");
        verify(userService, never()).saveUser(any());
        verify(diaryEntryRepository, never()).deleteById(any());
    }

    @Test
    void deleteById_WhenUserServiceThrowsException_ShouldPropagateException() {
        // Arrange
        ObjectId id = testDiaryEntry.getId();
        when(userService.findByUserName("testUser")).thenThrow(new RuntimeException("User lookup failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> diaryEntryService.deleteById(id, "testUser"));
        assertEquals("An error occurred while deleting the entry.", exception.getMessage());

        // Verify interactions
        verify(userService, times(1)).findByUserName("testUser");
        verify(userService, never()).saveUser(any());
        verify(diaryEntryRepository, never()).deleteById(any());
    }

}