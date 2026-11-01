package net.shubhankarpotnis.diaryApp.controller;

import net.shubhankarpotnis.diaryApp.entity.DiaryEntry;
import net.shubhankarpotnis.diaryApp.entity.User;
import net.shubhankarpotnis.diaryApp.repository.DiaryEntryRepository;
import net.shubhankarpotnis.diaryApp.repository.UserRepository;
import net.shubhankarpotnis.diaryApp.utilis.JwtUtil;
import net.shubhankarpotnis.diaryApp.dto.TrashDeleteRequest;
import net.shubhankarpotnis.diaryApp.service.DiaryEntryService;
import org.springframework.core.ParameterizedTypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Integration tests for DiaryEntryController.
 *
 * Uses the "test" Spring profile (application-test.yml), which points to a
 * local Postgres database (diarydb_test) and runs on port 8085 with
 * context-path /diary.
 * BASE_URL = http://localhost:8085  +  /diary (context)  +  /diary (controller mapping)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class DiaryEntryControllerTest {

    private static final String BASE_URL = "http://localhost:8085/diary/diary";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DiaryEntryRepository diaryEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private DiaryEntryService diaryEntryService;

    private User testUser;
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        // Deletion order matters: diary_entries has a foreign key to users,
        // so entries must go first or the FK constraint rejects the delete.
        diaryEntryRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUserName("testuser");
        testUser.setPassword("password");
        testUser.setRole("USER");
        testUser = userRepository.save(testUser);

        String token = jwtUtil.generateToken(testUser.getUserName());
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
    }

    // ---- POST /diary  (createEntry) ----

    @Test  // A. Happy Path
    void createDiaryEntry_WhenTitleAndContentProvided_ShouldReturnCreatedAndPersistEntry() {
        DiaryEntry entry = new DiaryEntry();
        entry.setTitle("My First Entry");
        entry.setContent("This is my first diary entry");

        ResponseEntity<DiaryEntry> response = restTemplate.postForEntity(
                BASE_URL, new HttpEntity<>(entry, headers), DiaryEntry.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("My First Entry");

        // Verify persistence in Postgres
        List<DiaryEntry> saved = diaryEntryRepository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getTitle()).isEqualTo("My First Entry");
    }

    @Test  // B. Edge Case: title omitted (title is optional now, content is required)
    void createDiaryEntry_WhenTitleIsMissing_ShouldReturnCreated() {
        DiaryEntry entry = new DiaryEntry();
        entry.setContent("Content Only");
        // title intentionally omitted -- it's not @NotBlank anymore

        ResponseEntity<DiaryEntry> response = restTemplate.postForEntity(
                BASE_URL, new HttpEntity<>(entry, headers), DiaryEntry.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEqualTo("Content Only");
    }

    @Test  // C. Error Case: missing content -- @NotBlank on DiaryEntry.content must reject this
    void createDiaryEntry_WhenContentIsBlank_ShouldReturnBadRequest() {
        DiaryEntry entry = new DiaryEntry();
        entry.setTitle("Has a title, no content");
        entry.setContent("");  // @NotBlank violation

        ResponseEntity<String> response = restTemplate.postForEntity(
                BASE_URL, new HttpEntity<>(entry, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---- GET /diary  (getAllDairyEntriesOfUser) ----

    @Test  // A. Happy Path: user has entries
    void getAllDiaryEntries_WhenUserHasEntries_ShouldReturnOkWithList() {
        DiaryEntry entry = new DiaryEntry();
        entry.setTitle("Entry 1");
        entry.setContent("Content 1");
        entry.setUser(testUser);
        entry.setDate(LocalDateTime.now());
        diaryEntryRepository.save(entry);

        ResponseEntity<DiaryEntry[]> response = restTemplate.exchange(
                BASE_URL, HttpMethod.GET,
                new HttpEntity<>(headers), DiaryEntry[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].getTitle()).isEqualTo("Entry 1");
    }

    @Test  // B. Edge Case: user has no entries yet
    void getAllDiaryEntries_WhenUserHasNoEntries_ShouldReturnNotFound() {
        // testUser was saved in @BeforeEach with no entries
        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL, HttpMethod.GET,
                new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- GET /diary/id/{id}  (getDiaryEntryById) ----

    @Test  // A. Happy Path
    void getDiaryEntryById_WhenEntryBelongsToUser_ShouldReturnOk() {
        DiaryEntry entry = new DiaryEntry();
        entry.setTitle("Owned Entry");
        entry.setContent("Owned Content");
        entry.setUser(testUser);
        entry.setDate(LocalDateTime.now());
        entry = diaryEntryRepository.save(entry);

        ResponseEntity<DiaryEntry> response = restTemplate.exchange(
                BASE_URL + "/id/" + entry.getId(),
                HttpMethod.GET, new HttpEntity<>(headers), DiaryEntry.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Owned Entry");
    }

    @Test  // C. Error Case: entry ID does not exist
    void getDiaryEntryById_WhenEntryDoesNotExist_ShouldReturnNotFound() {
        Long fakeId = 999999L;

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/id/" + fakeId,
                HttpMethod.GET, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- DELETE /diary/id/{id}  (deleteDiaryEntryById) ----

    @Test  // A. Happy Path
    void deleteDiaryEntryById_WhenEntryExists_ShouldReturnNoContent() {
        DiaryEntry entry = new DiaryEntry();
        entry.setTitle("To Delete");
        entry.setContent("Delete Me");
        entry.setUser(testUser);
        entry.setDate(LocalDateTime.now());
        entry = diaryEntryRepository.save(entry);

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/id/" + entry.getId(),
                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        DiaryEntry reloaded = diaryEntryRepository.findById(entry.getId()).orElseThrow();
        assertThat(reloaded.isDeleted()).isTrue();
        assertThat(reloaded.getDeletedAt()).isNotNull();
    }

    @Test  // C. Error Case: entry ID does not exist
    void deleteDiaryEntryById_WhenEntryDoesNotExist_ShouldReturnNotFound() {
        Long fakeId = 999999L;

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/id/" + fakeId,
                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- PUT /diary/id/{id}  (updateDiaryById) ----

    @Test  // A. Happy Path
    void updateDiaryById_WhenEntryExists_ShouldReturnOkWithUpdatedEntry() {
        DiaryEntry entry = new DiaryEntry();
        entry.setTitle("Original Title");
        entry.setContent("Original Content");
        entry.setUser(testUser);
        entry.setDate(LocalDateTime.now());
        entry = diaryEntryRepository.save(entry);

        DiaryEntry update = new DiaryEntry();
        update.setTitle("Updated Title");
        update.setContent("Updated Content");

        ResponseEntity<DiaryEntry> response = restTemplate.exchange(
                BASE_URL + "/id/" + entry.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(update, headers),
                DiaryEntry.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Updated Title");
        assertThat(response.getBody().getContent()).isEqualTo("Updated Content");
    }

    @Test  // C. Error Case: entry to update does not exist
    void updateDiaryById_WhenEntryDoesNotExist_ShouldReturnNotFound() {
        Long fakeId = 999999L;
        DiaryEntry update = new DiaryEntry();
        update.setContent("Updated Content");

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/id/" + fakeId,
                HttpMethod.PUT,
                new HttpEntity<>(update, headers),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- helpers for soft delete / trash tests ----

    private HttpHeaders headersFor(User user) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(jwtUtil.generateToken(user.getUserName()));
        return h;
    }

    private User saveOtherUser() {
        User other = new User();
        other.setUserName("otheruser");
        other.setPassword("password");
        other.setRole("USER");
        return userRepository.save(other);
    }

    /** deletedAt == null -> active entry, otherwise a trashed entry deleted at that time. */
    private DiaryEntry saveEntryFor(User owner, String title, OffsetDateTime deletedAt) {
        DiaryEntry entry = new DiaryEntry();
        entry.setTitle(title);
        entry.setContent("Content of " + title);
        entry.setUser(owner);
        entry.setDate(LocalDateTime.now());
        if (deletedAt != null) {
            entry.setDeleted(true);
            entry.setDeletedAt(deletedAt);
        }
        return diaryEntryRepository.save(entry);
    }

    private ResponseEntity<List<Map<String, Object>>> getTrash(HttpHeaders h) {
        return restTemplate.exchange(
                BASE_URL + "/trash", HttpMethod.GET, new HttpEntity<>(h),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }

    // ---- soft delete: effect on existing endpoints ----

    @Test  // A. Happy Path: dashboard hides trashed entries
    void getAllDiaryEntries_WhenSomeEntriesAreInTrash_ShouldReturnOnlyActiveOnes() {
        saveEntryFor(testUser, "Active", null);
        saveEntryFor(testUser, "Trashed", OffsetDateTime.now());

        ResponseEntity<DiaryEntry[]> response = restTemplate.exchange(
                BASE_URL, HttpMethod.GET, new HttpEntity<>(headers), DiaryEntry[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].getTitle()).isEqualTo("Active");
    }

    @Test  // B. Edge Case: everything is in trash, dashboard behaves as empty
    void getAllDiaryEntries_WhenAllEntriesAreInTrash_ShouldReturnNotFound() {
        saveEntryFor(testUser, "Trashed", OffsetDateTime.now());

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL, HttpMethod.GET, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test  // C. Error Case: trashed entry can't be fetched by id
    void getDiaryEntryById_WhenEntryIsInTrash_ShouldReturnNotFound() {
        DiaryEntry trashed = saveEntryFor(testUser, "Trashed", OffsetDateTime.now());

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/id/" + trashed.getId(),
                HttpMethod.GET, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test  // C. Error Case: trashed entry can't be edited
    void updateDiaryById_WhenEntryIsInTrash_ShouldReturnNotFoundAndLeaveEntryUntouched() {
        DiaryEntry trashed = saveEntryFor(testUser, "Trashed", OffsetDateTime.now());
        DiaryEntry update = new DiaryEntry();
        update.setTitle("Sneaky edit");
        update.setContent("Sneaky content");

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/id/" + trashed.getId(),
                HttpMethod.PUT, new HttpEntity<>(update, headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(diaryEntryRepository.findById(trashed.getId()).orElseThrow().getTitle())
                .isEqualTo("Trashed");
    }

    @Test  // C. Error Case: soft deleting twice
    void deleteDiaryEntryById_WhenEntryIsAlreadyInTrash_ShouldReturnNotFound() {
        DiaryEntry trashed = saveEntryFor(testUser, "Trashed", OffsetDateTime.now());

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/id/" + trashed.getId(),
                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- GET /diary/trash ----

    @Test  // B. Edge Case: empty trash is a normal 200 with an empty list, not a 404
    void getTrash_WhenTrashIsEmpty_ShouldReturnOkWithEmptyList() {
        saveEntryFor(testUser, "Active", null);

        ResponseEntity<List<Map<String, Object>>> response = getTrash(headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test  // A. Happy Path: only trashed entries, with daysRemaining
    void getTrash_WhenTrashHasEntries_ShouldReturnOnlyTrashedWithDaysRemaining() {
        saveEntryFor(testUser, "Active", null);
        saveEntryFor(testUser, "Trashed Ten Days Ago", OffsetDateTime.now().minusDays(10));

        ResponseEntity<List<Map<String, Object>>> response = getTrash(headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        Map<String, Object> item = response.getBody().get(0);
        assertThat(item.get("title")).isEqualTo("Trashed Ten Days Ago");
        assertThat(((Number) item.get("daysRemaining")).longValue()).isEqualTo(20L);
        assertThat(item.get("deletedAt")).isNotNull();
    }

    @Test  // C. Error Case: users must never see each other's trash
    void getTrash_WhenAnotherUserHasTrashedEntries_ShouldNotIncludeThem() {
        User other = saveOtherUser();
        saveEntryFor(other, "Other user's trash", OffsetDateTime.now());

        ResponseEntity<List<Map<String, Object>>> response = getTrash(headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    // ---- POST /diary/id/{id}/restore ----

    @Test  // A. Happy Path
    void restoreDiaryEntry_WhenEntryIsInTrash_ShouldReturnNoContentAndMakeItActiveAgain() {
        DiaryEntry trashed = saveEntryFor(testUser, "Trashed", OffsetDateTime.now());

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/id/" + trashed.getId() + "/restore",
                HttpMethod.POST, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        DiaryEntry reloaded = diaryEntryRepository.findById(trashed.getId()).orElseThrow();
        assertThat(reloaded.isDeleted()).isFalse();
        assertThat(reloaded.getDeletedAt()).isNull();
    }

    @Test  // C. Error Case: entry is active, nothing to restore
    void restoreDiaryEntry_WhenEntryIsNotInTrash_ShouldReturnNotFound() {
        DiaryEntry active = saveEntryFor(testUser, "Active", null);

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/id/" + active.getId() + "/restore",
                HttpMethod.POST, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test  // C. Error Case: someone else's trashed entry
    void restoreDiaryEntry_WhenEntryBelongsToAnotherUser_ShouldReturnNotFoundAndStayTrashed() {
        User other = saveOtherUser();
        DiaryEntry othersTrashed = saveEntryFor(other, "Other user's", OffsetDateTime.now());

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/id/" + othersTrashed.getId() + "/restore",
                HttpMethod.POST, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(diaryEntryRepository.findById(othersTrashed.getId()).orElseThrow().isDeleted()).isTrue();
    }

    // ---- DELETE /diary/trash/{id} ----

    @Test  // A. Happy Path
    void permanentlyDeleteOne_WhenEntryIsInTrash_ShouldReturnNoContentAndRemoveRow() {
        DiaryEntry trashed = saveEntryFor(testUser, "Trashed", OffsetDateTime.now());

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/trash/" + trashed.getId(),
                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(diaryEntryRepository.findById(trashed.getId())).isEmpty();
    }

    @Test  // C. Error Case: this endpoint must not be able to hard delete an active entry
    void permanentlyDeleteOne_WhenEntryIsActive_ShouldReturnNotFoundAndKeepEntry() {
        DiaryEntry active = saveEntryFor(testUser, "Active", null);

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/trash/" + active.getId(),
                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(diaryEntryRepository.findById(active.getId())).isPresent();
    }

    @Test  // C. Error Case: someone else's trashed entry
    void permanentlyDeleteOne_WhenEntryBelongsToAnotherUser_ShouldReturnNotFoundAndKeepEntry() {
        User other = saveOtherUser();
        DiaryEntry othersTrashed = saveEntryFor(other, "Other user's", OffsetDateTime.now());

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/trash/" + othersTrashed.getId(),
                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(diaryEntryRepository.findById(othersTrashed.getId())).isPresent();
    }

    // ---- DELETE /diary/trash  (batch, body: {"ids": [...]}) ----

    @Test  // A. Happy Path: trashed ids deleted, active id in the list is skipped
    void permanentlyDeleteBatch_WhenIdsIncludeActiveEntry_ShouldDeleteOnlyTrashedOnes() {
        DiaryEntry t1 = saveEntryFor(testUser, "Trashed 1", OffsetDateTime.now());
        DiaryEntry t2 = saveEntryFor(testUser, "Trashed 2", OffsetDateTime.now());
        DiaryEntry active = saveEntryFor(testUser, "Active", null);

        TrashDeleteRequest request = new TrashDeleteRequest();
        request.setIds(List.of(t1.getId(), t2.getId(), active.getId()));

        ResponseEntity<Map<String, Integer>> response = restTemplate.exchange(
                BASE_URL + "/trash", HttpMethod.DELETE, new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<Map<String, Integer>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("deletedCount")).isEqualTo(2);
        assertThat(diaryEntryRepository.findById(t1.getId())).isEmpty();
        assertThat(diaryEntryRepository.findById(t2.getId())).isEmpty();
        assertThat(diaryEntryRepository.findById(active.getId())).isPresent();
    }

    @Test  // C. Error Case: empty id list
    void permanentlyDeleteBatch_WhenIdsListIsEmpty_ShouldReturnBadRequest() {
        TrashDeleteRequest request = new TrashDeleteRequest();
        request.setIds(List.of());

        ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/trash", HttpMethod.DELETE, new HttpEntity<>(request, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---- DELETE /diary/trash/all ----

    @Test  // A. Happy Path: empties trash, leaves active entries and other users alone
    void emptyTrash_WhenTrashHasEntries_ShouldDeleteOnlyMyTrashedEntries() {
        User other = saveOtherUser();
        DiaryEntry t1 = saveEntryFor(testUser, "Trashed 1", OffsetDateTime.now());
        DiaryEntry t2 = saveEntryFor(testUser, "Trashed 2", OffsetDateTime.now().minusDays(7));
        DiaryEntry active = saveEntryFor(testUser, "Active", null);
        DiaryEntry othersTrashed = saveEntryFor(other, "Other user's trash", OffsetDateTime.now());

        ResponseEntity<Map<String, Integer>> response = restTemplate.exchange(
                BASE_URL + "/trash/all", HttpMethod.DELETE, new HttpEntity<>(headers),
                new ParameterizedTypeReference<Map<String, Integer>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("deletedCount")).isEqualTo(2);
        assertThat(diaryEntryRepository.findById(t1.getId())).isEmpty();
        assertThat(diaryEntryRepository.findById(t2.getId())).isEmpty();
        assertThat(diaryEntryRepository.findById(active.getId())).isPresent();
        assertThat(diaryEntryRepository.findById(othersTrashed.getId())).isPresent();
    }

    @Test  // B. Edge Case: nothing in trash
    void emptyTrash_WhenTrashIsEmpty_ShouldReturnOkWithZeroDeleted() {
        saveEntryFor(testUser, "Active", null);

        ResponseEntity<Map<String, Integer>> response = restTemplate.exchange(
                BASE_URL + "/trash/all", HttpMethod.DELETE, new HttpEntity<>(headers),
                new ParameterizedTypeReference<Map<String, Integer>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("deletedCount")).isEqualTo(0);
    }

    // ---- scheduled purge, against the real DB (exercises the derived query) ----

    @Test  // A. Happy Path
    void purgeExpiredEntries_ShouldHardDeleteOnlyEntriesTrashedMoreThanThirtyDaysAgo() {
        DiaryEntry expired = saveEntryFor(testUser, "Expired", OffsetDateTime.now().minusDays(31));
        DiaryEntry recent = saveEntryFor(testUser, "Recent trash", OffsetDateTime.now().minusDays(5));
        DiaryEntry active = saveEntryFor(testUser, "Active", null);

        diaryEntryService.purgeExpiredEntries();

        assertThat(diaryEntryRepository.findById(expired.getId())).isEmpty();
        assertThat(diaryEntryRepository.findById(recent.getId())).isPresent();
        assertThat(diaryEntryRepository.findById(active.getId())).isPresent();
    }
}