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
import org.mockito.ArgumentCaptor;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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
        assertTrue(testDiaryEntry.isDeleted(), "Entry should be flagged as deleted");
        assertNotNull(testDiaryEntry.getDeletedAt(), "deletedAt should be set");
        verify(diaryEntryRepository, times(1)).save(testDiaryEntry);
        verify(diaryEntryRepository, never()).deleteById(any());
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


    // ---- helper ----
    private DiaryEntry trashedEntry(Long id) {
        DiaryEntry e = new DiaryEntry();
        e.setId(id);
        e.setTitle("Trashed " + id);
        e.setContent("Content " + id);
        e.setUser(testUser);
        e.setDeleted(true);
        e.setDeletedAt(OffsetDateTime.now().minusDays(3));
        return e;
    }


    // ---- getActiveEntries / getTrashEntries ----
    @Test  // A. Happy Path
    void getActiveEntries_WhenCalled_ShouldReturnRepositoryResultForUser() {
        when(userService.findByUserName("testUser")).thenReturn(testUser);
        when(diaryEntryRepository.findByUserAndDeletedFalseOrderByDateDesc(testUser))
                .thenReturn(List.of(testDiaryEntry));

        List<DiaryEntry> result = diaryEntryService.getActiveEntries("testUser");

        assertEquals(1, result.size());
        assertEquals(testDiaryEntry, result.get(0));
        verify(diaryEntryRepository, times(1)).findByUserAndDeletedFalseOrderByDateDesc(testUser);
    }

    @Test  // A. Happy Path
    void getTrashEntries_WhenCalled_ShouldReturnRepositoryResultForUser() {
        DiaryEntry trashed = trashedEntry(5L);
        when(userService.findByUserName("testUser")).thenReturn(testUser);
        when(diaryEntryRepository.findByUserAndDeletedTrueOrderByDeletedAtDesc(testUser))
                .thenReturn(List.of(trashed));

        List<DiaryEntry> result = diaryEntryService.getTrashEntries("testUser");

        assertEquals(1, result.size());
        assertEquals(trashed, result.get(0));
        verify(diaryEntryRepository, times(1)).findByUserAndDeletedTrueOrderByDeletedAtDesc(testUser);
    }

    // ---- calculateDaysRemaining ----

    @Test  // B. Edge Case: deletedAt missing
    void calculateDaysRemaining_WhenDeletedAtIsNull_ShouldReturnFullWindow() {
        testDiaryEntry.setDeletedAt(null);

        assertEquals(30L, diaryEntryService.calculateDaysRemaining(testDiaryEntry));
    }

    @Test  // A. Happy Path
    void calculateDaysRemaining_WhenDeletedFiveDaysAgo_ShouldReturn25() {
        testDiaryEntry.setDeletedAt(OffsetDateTime.now().minusDays(5));

        assertEquals(25L, diaryEntryService.calculateDaysRemaining(testDiaryEntry));
    }

    @Test  // B. Edge Case: window already passed, must never go negative
    void calculateDaysRemaining_WhenDeletedFortyDaysAgo_ShouldClampToZero() {
        testDiaryEntry.setDeletedAt(OffsetDateTime.now().minusDays(40));

        assertEquals(0L, diaryEntryService.calculateDaysRemaining(testDiaryEntry));
    }

    // ---- deleteById (soft delete) extra case ----

    @Test  // B. Edge Case: entry is already in trash
    void deleteById_WhenEntryIsAlreadyInTrash_ShouldReturnFalseAndNotSave() {
        testDiaryEntry.setUser(testUser);
        testDiaryEntry.setDeleted(true);
        when(userService.findByUserName("testUser")).thenReturn(testUser);
        when(diaryEntryRepository.findById(testDiaryEntry.getId())).thenReturn(Optional.of(testDiaryEntry));

        boolean result = diaryEntryService.deleteById(testDiaryEntry.getId(), "testUser");

        assertFalse(result);
        verify(diaryEntryRepository, never()).save(any(DiaryEntry.class));
    }

    // ---- restoreById ----

    @Test  // A. Happy Path
    void restoreById_WhenEntryIsInTrash_ShouldClearFlagsSaveAndReturnTrue() {
        DiaryEntry trashed = trashedEntry(1L);
        when(userService.findByUserName("testUser")).thenReturn(testUser);
        when(diaryEntryRepository.findByIdAndUserAndDeletedTrue(1L, testUser)).thenReturn(Optional.of(trashed));

        boolean result = diaryEntryService.restoreById(1L, "testUser");

        assertTrue(result);
        assertFalse(trashed.isDeleted());
        assertNull(trashed.getDeletedAt());
        verify(diaryEntryRepository, times(1)).save(trashed);
    }

    @Test  // C. Error Case: not in trash, doesn't exist, or belongs to someone else
    void restoreById_WhenEntryNotFoundInTrash_ShouldReturnFalseAndNotSave() {
        when(userService.findByUserName("testUser")).thenReturn(testUser);
        when(diaryEntryRepository.findByIdAndUserAndDeletedTrue(99L, testUser)).thenReturn(Optional.empty());

        boolean result = diaryEntryService.restoreById(99L, "testUser");

        assertFalse(result);
        verify(diaryEntryRepository, never()).save(any(DiaryEntry.class));
    }

    // ---- permanentlyDeleteById ----

    @Test  // A. Happy Path
    void permanentlyDeleteById_WhenEntryIsInTrash_ShouldHardDeleteAndReturnTrue() {
        when(userService.findByUserName("testUser")).thenReturn(testUser);
        when(diaryEntryRepository.findByIdAndUserAndDeletedTrue(1L, testUser))
                .thenReturn(Optional.of(trashedEntry(1L)));

        boolean result = diaryEntryService.permanentlyDeleteById(1L, "testUser");

        assertTrue(result);
        verify(diaryEntryRepository, times(1)).deleteById(1L);
    }

    @Test  // C. Error Case: active entries must never be hard deleted through this path
    void permanentlyDeleteById_WhenEntryNotInTrash_ShouldReturnFalseAndNotDelete() {
        when(userService.findByUserName("testUser")).thenReturn(testUser);
        when(diaryEntryRepository.findByIdAndUserAndDeletedTrue(1L, testUser)).thenReturn(Optional.empty());

        boolean result = diaryEntryService.permanentlyDeleteById(1L, "testUser");

        assertFalse(result);
        verify(diaryEntryRepository, never()).deleteById(any());
    }

    // ---- permanentlyDeleteByIds (batch) ----

    @Test  // B. Edge Case: mix of valid and invalid ids, only valid ones are deleted
    void permanentlyDeleteByIds_WhenSomeIdsAreNotInTrash_ShouldDeleteOnlyTrashedOnesAndReturnCount() {
        when(userService.findByUserName("testUser")).thenReturn(testUser);
        when(diaryEntryRepository.findByIdAndUserAndDeletedTrue(1L, testUser)).thenReturn(Optional.of(trashedEntry(1L)));
        when(diaryEntryRepository.findByIdAndUserAndDeletedTrue(2L, testUser)).thenReturn(Optional.empty());
        when(diaryEntryRepository.findByIdAndUserAndDeletedTrue(3L, testUser)).thenReturn(Optional.of(trashedEntry(3L)));

        int count = diaryEntryService.permanentlyDeleteByIds(List.of(1L, 2L, 3L), "testUser");

        assertEquals(2, count);
        verify(diaryEntryRepository, times(1)).deleteById(1L);
        verify(diaryEntryRepository, times(1)).deleteById(3L);
        verify(diaryEntryRepository, never()).deleteById(2L);
    }

    // ---- permanentlyDeleteAllTrash ----

    @Test  // A. Happy Path
    void permanentlyDeleteAllTrash_WhenTrashHasEntries_ShouldDeleteAllAndReturnCount() {
        List<DiaryEntry> trashed = List.of(trashedEntry(1L), trashedEntry(2L));
        when(userService.findByUserName("testUser")).thenReturn(testUser);
        when(diaryEntryRepository.findByUserAndDeletedTrueOrderByDeletedAtDesc(testUser)).thenReturn(trashed);

        int count = diaryEntryService.permanentlyDeleteAllTrash("testUser");

        assertEquals(2, count);
        verify(diaryEntryRepository, times(1)).deleteAll(trashed);
    }

    @Test  // B. Edge Case: trash already empty
    void permanentlyDeleteAllTrash_WhenTrashIsEmpty_ShouldReturnZero() {
        when(userService.findByUserName("testUser")).thenReturn(testUser);
        when(diaryEntryRepository.findByUserAndDeletedTrueOrderByDeletedAtDesc(testUser)).thenReturn(List.of());

        int count = diaryEntryService.permanentlyDeleteAllTrash("testUser");

        assertEquals(0, count);
    }

    // ---- purgeExpiredEntries (scheduled job) ----

    @Test  // A. Happy Path: expired entries are hard deleted, cutoff is ~30 days ago
    void purgeExpiredEntries_WhenExpiredEntriesExist_ShouldDeleteThemUsingThirtyDayCutoff() {
        List<DiaryEntry> expired = List.of(trashedEntry(1L), trashedEntry(2L));
        when(diaryEntryRepository.findByDeletedTrueAndDeletedAtBefore(any(OffsetDateTime.class)))
                .thenReturn(expired);

        diaryEntryService.purgeExpiredEntries();

        ArgumentCaptor<OffsetDateTime> captor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(diaryEntryRepository).findByDeletedTrueAndDeletedAtBefore(captor.capture());
        long secondsOff = Math.abs(ChronoUnit.SECONDS.between(
                captor.getValue(), OffsetDateTime.now().minusDays(30)));
        assertTrue(secondsOff < 5, "Cutoff should be roughly 30 days ago");
        verify(diaryEntryRepository, times(1)).deleteAll(expired);
    }

    @Test  // B. Edge Case: nothing expired
    void purgeExpiredEntries_WhenNothingExpired_ShouldNotDeleteAnything() {
        when(diaryEntryRepository.findByDeletedTrueAndDeletedAtBefore(any(OffsetDateTime.class)))
                .thenReturn(List.of());

        diaryEntryService.purgeExpiredEntries();

        verify(diaryEntryRepository, never()).deleteAll(any());
    }
}