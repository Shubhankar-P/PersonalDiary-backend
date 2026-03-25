package net.shubhankarpotnis.diaryApp.controller;

import net.shubhankarpotnis.diaryApp.entity.DiaryEntry;
import net.shubhankarpotnis.diaryApp.entity.User;
import net.shubhankarpotnis.diaryApp.repository.UserRepository;
import net.shubhankarpotnis.diaryApp.utilis.JwtUtil;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import java.util.Collections;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DiaryEntryController.
 *
 * Uses the "test" Spring profile (application-test.yml) which points to the real
 * test MongoDB and runs on port 8085 with context-path /diary.
 * BASE_URL = http://localhost:8085  +  /diary (context)  +  /diary (controller mapping)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class DiaryEntryControllerTest {

    private static final String BASE_URL = "http://localhost:8085/diary/diary";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(DiaryEntry.class);
        mongoTemplate.dropCollection(User.class);

        testUser = new User();
        testUser.setUserName("testuser");
        testUser.setPassword("password");
        testUser.setRoles(Collections.singletonList("USER")); // ADD THIS LINE
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

        // Verify persistence in MongoDB
        List<DiaryEntry> saved = mongoTemplate.findAll(DiaryEntry.class);
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getTitle()).isEqualTo("My First Entry");
    }

    @Test  // B. Edge Case: title only (content is optional)
    void createDiaryEntry_WhenContentIsMissing_ShouldReturnCreated() {
        DiaryEntry entry = new DiaryEntry();
        entry.setTitle("Title Only");
        // content intentionally omitted — it's not @NotBlank

        ResponseEntity<DiaryEntry> response = restTemplate.postForEntity(
                BASE_URL, new HttpEntity<>(entry, headers), DiaryEntry.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Title Only");
    }

    @Test  // C. Error Case: missing title — @NotBlank on DiaryEntry.title must reject this
    void createDiaryEntry_WhenTitleIsBlank_ShouldReturnBadRequest() {
        DiaryEntry entry = new DiaryEntry();
        entry.setTitle("");  // @NotBlank violation

        ResponseEntity<String> response = restTemplate.postForEntity(
                BASE_URL, new HttpEntity<>(entry, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---- GET /diary  (getAllDairyEntriesOfUser) ----

    @Test  // A. Happy Path: user has entries
    void getAllDiaryEntries_WhenUserHasEntries_ShouldReturnOkWithList() {
        // Persist a diary entry and link it to the test user
        DiaryEntry entry = new DiaryEntry();
        entry.setTitle("Entry 1");
        entry.setContent("Content 1");
        mongoTemplate.save(entry);

        testUser.getDiaryEntries().add(entry);
        userRepository.save(testUser);

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
        // testUser was saved in @BeforeEach with an empty diary list
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
        mongoTemplate.save(entry);
        testUser.getDiaryEntries().add(entry);
        userRepository.save(testUser);

        ResponseEntity<DiaryEntry> response = restTemplate.exchange(
                BASE_URL + "/id/" + entry.getId().toHexString(),
                HttpMethod.GET, new HttpEntity<>(headers), DiaryEntry.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Owned Entry");
    }

    @Test  // C. Error Case: entry ID does not exist
    void getDiaryEntryById_WhenEntryDoesNotExist_ShouldReturnNotFound() {
        ObjectId fakeId = new ObjectId();

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/id/" + fakeId.toHexString(),
                HttpMethod.GET, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- DELETE /diary/id/{id}  (deleteDiaryEntryById) ----

    @Test  // A. Happy Path
    void deleteDiaryEntryById_WhenEntryExists_ShouldReturnNoContent() {
        DiaryEntry entry = new DiaryEntry();
        entry.setTitle("To Delete");
        mongoTemplate.save(entry);
        testUser.getDiaryEntries().add(entry);
        userRepository.save(testUser);

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/id/" + entry.getId().toHexString(),
                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        // Verify it's actually gone from MongoDB
        assertThat(mongoTemplate.findAll(DiaryEntry.class)).isEmpty();
    }

    @Test  // C. Error Case: entry ID does not exist
    void deleteDiaryEntryById_WhenEntryDoesNotExist_ShouldReturnNotFound() {
        ObjectId fakeId = new ObjectId();

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/id/" + fakeId.toHexString(),
                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- PUT /diary/id/{id}  (updateDiaryById) ----

    @Test  // A. Happy Path
    void updateDiaryById_WhenEntryExists_ShouldReturnOkWithUpdatedEntry() {
        DiaryEntry entry = new DiaryEntry();
        entry.setTitle("Original Title");
        entry.setContent("Original Content");
        mongoTemplate.save(entry);
        testUser.getDiaryEntries().add(entry);
        userRepository.save(testUser);

        DiaryEntry update = new DiaryEntry();
        update.setTitle("Updated Title");
        update.setContent("Updated Content");

        ResponseEntity<DiaryEntry> response = restTemplate.exchange(
                BASE_URL + "/id/" + entry.getId().toHexString(),
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
        ObjectId fakeId = new ObjectId();
        DiaryEntry update = new DiaryEntry();
        update.setTitle("Updated Title");

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/id/" + fakeId.toHexString(),
                HttpMethod.PUT,
                new HttpEntity<>(update, headers),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}