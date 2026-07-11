package net.shubhankarpotnis.diaryApp.controller;

import net.shubhankarpotnis.diaryApp.entity.DiaryEntry;
import net.shubhankarpotnis.diaryApp.entity.User;
import net.shubhankarpotnis.diaryApp.repository.DiaryEntryRepository;
import net.shubhankarpotnis.diaryApp.repository.UserRepository;
import net.shubhankarpotnis.diaryApp.utilis.JwtUtil;
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
        entry = diaryEntryRepository.save(entry);

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/id/" + entry.getId(),
                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(diaryEntryRepository.findAll()).isEmpty();
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
}