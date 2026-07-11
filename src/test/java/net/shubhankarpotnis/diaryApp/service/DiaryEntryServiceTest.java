package net.shubhankarpotnis.diaryApp.service;

import net.shubhankarpotnis.diaryApp.entity.DiaryEntry;
import net.shubhankarpotnis.diaryApp.entity.User;
import net.shubhankarpotnis.diaryApp.repository.DiaryEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserName("testUser");

        testDiaryEntry = new DiaryEntry();
        testDiaryEntry.setId(1L);
        testDiaryEntry.setTitle("My test Diary");
        testDiaryEntry.setContent("Some content");
    }

    @Test  // A. Happy Path
    void saveEntry_WhenCalled_SetsOwnerAndDateThenSaves() {
        when(userService.findByUserName("testUser")).thenReturn(testUser);
        when(diaryEntryRepository.save(any(DiaryEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        diaryEntryService.saveEntry(testDiaryEntry, "testUser");

        assertNotNull(testDiaryEntry.getDate(), "DiaryEntry should have a set date");
        assertEquals(testUser, testDiaryEntry.getUser(), "DiaryEntry should be linked to the owning user via the FK");

        verify(userService, times(1)).findByUserName("testUser");
        verify(diaryEntryRepository, times(1)).save(testDiaryEntry);
        verify(userService, never()).saveUser(any(User.class));
    }

    @Test    // B. Edge Case: Empty title (title is optional now, content is required)
    void saveEntry_WhenTitleIsEmpty_ShouldStillSaveNormally() {
        testDiaryEntry.setTitle("");
        when(userService.findByUserName("testUser")).thenReturn(testUser);
        when(diaryEntryRepository.save(any(DiaryEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        diaryEntryService.saveEntry(testDiaryEntry, "testUser");

        assertNotNull(testDiaryEntry.getDate(), "Date should be set even if title is empty");
        assertEquals(testUser, testDiaryEntry.getUser());

        verify(userService, times(1)).findByUserName("testUser");
        verify(diaryEntryRepository, times(1)).save(testDiaryEntry);
    }

    @Test   // C. Error Case: userService.findByUserName Throws Exception
    void saveEntry_WhenUserServiceThrowsException_ShouldNotPropagateButLogError() {
        when(userService.findByUserName("testUser")).thenThrow(new RuntimeException("User lookup failed"));

        assertDoesNotThrow(() -> diaryEntryService.saveEntry(testDiaryEntry, "testUser"));

        verify(diaryEntryRepository, never()).save(any(DiaryEntry.class));
    }

    @Test  // A. Happy Path: Direct save (used by update endpoint)
    void saveEntry_WhenCalledWithJustEntry_ShouldDelegateDirectlyToRepository() {
        when(diaryEntryRepository.save(testDiaryEntry)).thenReturn(testDiaryEntry);

        diaryEntryService.saveEntry(testDiaryEntry);

        verify(diaryEntryRepository, times(1)).save(testDiaryEntry);
        verify(userService, never()).findByUserName(any());
    }

    @Test  // A. Happy Path: findById returns entry
    void findById_WhenEntryExists_ShouldReturnDiaryEntry() {
        when(diaryEntryRepository.findById(testDiaryEntry.getId())).thenReturn(Optional.of(testDiaryEntry));

        Optional<DiaryEntry> result = diaryEntryService.findById(testDiaryEntry.getId());

        assertTrue(result.isPresent(), "Result should be present");
        assertEquals(testDiaryEntry, result.get(), "Result should match the entry returned by repository");

        verify(diaryEntryRepository, times(1)).findById(testDiaryEntry.getId());
    }

    @Test  // B. Edge Case: findById returns empty Optional
    void findById_WhenEntryDoesNotExist_ShouldReturnEmptyOptional() {
        Long id = 999L;
        when(diaryEntryRepository.findById(id)).thenReturn(Optional.empty());

        Optional<DiaryEntry> result = diaryEntryService.findById(id);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.isPresent(), "Result should be empty if entry not found");

        verify(diaryEntryRepository, times(1)).findById(id);
    }

    @Test  // C. Error Case: repository throws exception
    void findById_WhenRepositoryThrowsException_ShouldPropagateException() {
        Long id = 999L;
        when(diaryEntryRepository.findById(id)).thenThrow(new RuntimeException("DB error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> diaryEntryService.findById(id));
        assertEquals("DB error", exception.getMessage(), "Exception message should match");

        verify(diaryEntryRepository, times(1)).findById(id);
    }

    @Test  // A. Happy Path: entry belongs to the requesting user
    void deleteById_WhenEntryBelongsToUser_ShouldRemoveEntryAndReturnTrue() {
        testDiaryEntry.setUser(testUser);
        when(userService.findByUserName("testUser")).thenReturn(testUser);
        when(diaryEntryRepository.findById(testDiaryEntry.getId())).thenReturn(Optional.of(testDiaryEntry));

        boolean result = diaryEntryService.deleteById(testDiaryEntry.getId(), "testUser");

        assertTrue(result, "deleteById should return true when entry belongs to the user");

        verify(userService, times(1)).findByUserName("testUser");
        verify(diaryEntryRepository, times(1)).deleteById(testDiaryEntry.getId());
    }

    @Test  // B. Edge Case: entry exists but belongs to a different user
    void deleteById_WhenEntryBelongsToAnotherUser_ShouldReturnFalseAndNotDelete() {
        User anotherUser = new User();
        anotherUser.setId(2L);
        testDiaryEntry.setUser(anotherUser);

        when(userService.findByUserName("testUser")).thenReturn(testUser);
        when(diaryEntryRepository.findById(testDiaryEntry.getId())).thenReturn(Optional.of(testDiaryEntry));

        boolean result = diaryEntryService.deleteById(testDiaryEntry.getId(), "testUser");

        assertFalse(result, "deleteById should return false when the entry belongs to someone else");
        verify(diaryEntryRepository, never()).deleteById(any());
    }

    @Test  // C. Edge Case: entry does not exist at all
    void deleteById_WhenEntryDoesNotExist_ShouldReturnFalseAndNotCallDelete() {
        Long id = 999L;
        when(userService.findByUserName("testUser")).thenReturn(testUser);
        when(diaryEntryRepository.findById(id)).thenReturn(Optional.empty());

        boolean result = diaryEntryService.deleteById(id, "testUser");

        assertFalse(result, "deleteById should return false when entry not found");

        verify(userService, times(1)).findByUserName("testUser");
        verify(diaryEntryRepository, never()).deleteById(any());
    }

    @Test  // D. Error Case: userService throws
    void deleteById_WhenUserServiceThrowsException_ShouldPropagateException() {
        when(userService.findByUserName("testUser")).thenThrow(new RuntimeException("User lookup failed"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> diaryEntryService.deleteById(testDiaryEntry.getId(), "testUser"));
        assertEquals("An error occurred while deleting the entry.", exception.getMessage());

        verify(userService, times(1)).findByUserName("testUser");
        verify(diaryEntryRepository, never()).deleteById(any());
    }

}