package net.shubhankarpotnis.diaryApp.cache;

import net.shubhankarpotnis.diaryApp.entity.ConfigDiaryAppEntity;
import net.shubhankarpotnis.diaryApp.repository.ConfigDiaryAppRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppCacheTest {

    @Mock
    private ConfigDiaryAppRepository configDiaryAppRepository;

    @InjectMocks
    private AppCache appCache;

    // Helper to build a ConfigDiaryAppEntity
    private ConfigDiaryAppEntity entry(String key, String value) {
        ConfigDiaryAppEntity e = new ConfigDiaryAppEntity();
        e.setKey(key);
        e.setValue(value);
        return e;
    }

    @Test  // A. Happy Path
    void init_WhenConfigEntriesExist_ShouldPopulateCacheCorrectly() {
        // Arrange
        when(configDiaryAppRepository.findAll()).thenReturn(Arrays.asList(
                entry("quotes_api", "https://api.quotes.net/random"),
                entry("other_setting", "someValue")
        ));

        // Act
        appCache.init();

        // Assert
        assertNotNull(appCache.APP_CACHE, "Cache map should not be null after init");
        assertEquals(2, appCache.APP_CACHE.size(), "Cache should hold all loaded entries");
        assertEquals("https://api.quotes.net/random", appCache.APP_CACHE.get("quotes_api"));
        assertEquals("someValue", appCache.APP_CACHE.get("other_setting"));

        verify(configDiaryAppRepository, times(1)).findAll();
    }

    @Test  // B. Edge Case: No config entries in DB
    void init_WhenNoConfigEntriesExist_ShouldCreateEmptyCache() {
        // Arrange
        when(configDiaryAppRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        appCache.init();

        // Assert
        assertNotNull(appCache.APP_CACHE, "Cache map should still be initialised (not null)");
        assertTrue(appCache.APP_CACHE.isEmpty(), "Cache should be empty when DB has no config");

        verify(configDiaryAppRepository, times(1)).findAll();
    }

    @Test  // C. Error Case: Repository throws
    void init_WhenRepositoryThrows_ShouldPropagateException() {
        // Arrange
        when(configDiaryAppRepository.findAll())
                .thenThrow(new RuntimeException("DB connection refused"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> appCache.init(),
                "A DB error during init should propagate so the startup fails visibly");

        verify(configDiaryAppRepository, times(1)).findAll();
    }

    @Test  // Re-init wipes previous state — important for the /admin/clear-app-cache endpoint
    void init_WhenCalledTwice_ShouldReinitialiseAndNotRetainOldEntries() {
        // First init — populate with data
        when(configDiaryAppRepository.findAll())
                .thenReturn(Collections.singletonList(entry("quotes_api", "url1")))
                .thenReturn(Collections.emptyList());

        appCache.init();
        assertEquals("url1", appCache.APP_CACHE.get("quotes_api"),
                "First init should populate the key");

        // Second init — empty DB now
        appCache.init();
        assertTrue(appCache.APP_CACHE.isEmpty(),
                "Second init should wipe the previous cache; old keys must not survive");
    }
}