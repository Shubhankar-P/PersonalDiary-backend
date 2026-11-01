package net.shubhankarpotnis.diaryApp.controller;

import net.shubhankarpotnis.diaryApp.dto.TrashDeleteRequest;
import net.shubhankarpotnis.diaryApp.dto.TrashEntryDTO;
import net.shubhankarpotnis.diaryApp.entity.DiaryEntry;
import net.shubhankarpotnis.diaryApp.entity.User;
import net.shubhankarpotnis.diaryApp.service.DiaryEntryService;
import net.shubhankarpotnis.diaryApp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/diary")
public class DiaryEntryController {

    @Autowired
    private DiaryEntryService diaryEntryService;

    @Autowired
    private UserService userService;

    private String getAuthenticatedUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping
    public ResponseEntity<?> getAllDairyEntriesOfUser() {
        List<DiaryEntry> all = diaryEntryService.getActiveEntries(getAuthenticatedUsername());
        if (all != null && !all.isEmpty()) {
            return new ResponseEntity<>(all, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping
    public ResponseEntity<?> createEntry(@Valid @RequestBody DiaryEntry myEntry, BindingResult result) {
        if (result.hasErrors()) {
            String errorMsg = result.getAllErrors().get(0).getDefaultMessage();
            return new ResponseEntity<>(Collections.singletonMap("error", errorMsg), HttpStatus.BAD_REQUEST);
        }
        try {
            diaryEntryService.saveEntry(myEntry, getAuthenticatedUsername());
            return new ResponseEntity<>(myEntry, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    Collections.singletonMap("error", "Could not save entry"),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @GetMapping("id/{myId}")
    public ResponseEntity<DiaryEntry> getDiaryEntryById(@PathVariable Long myId) {
        User user = userService.findByUserName(getAuthenticatedUsername());
        Optional<DiaryEntry> diaryEntry = diaryEntryService.findById(myId);
        if (diaryEntry.isPresent()
                && diaryEntry.get().getUser().getId().equals(user.getId())
                && !diaryEntry.get().isDeleted()) {
            return new ResponseEntity<>(diaryEntry.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("id/{myId}")
    public ResponseEntity<?> deleteDiaryEntryById(@PathVariable Long myId) {
        boolean removed = diaryEntryService.deleteById(myId, getAuthenticatedUsername());
        if (removed) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PutMapping("/id/{myId}")
    public ResponseEntity<?> updateDiaryById(@PathVariable Long myId,
                                             @Valid @RequestBody DiaryEntry newEntry,
                                             BindingResult result) {
        if (result.hasErrors()) {
            String errorMsg = result.getAllErrors().get(0).getDefaultMessage();
            return new ResponseEntity<>(Collections.singletonMap("error", errorMsg), HttpStatus.BAD_REQUEST);
        }
        User user = userService.findByUserName(getAuthenticatedUsername());
        Optional<DiaryEntry> diaryEntry = diaryEntryService.findById(myId);
        if (diaryEntry.isPresent()
                && diaryEntry.get().getUser().getId().equals(user.getId())
                && !diaryEntry.get().isDeleted()) {
            DiaryEntry oldEntry = diaryEntry.get();
            oldEntry.setTitle(newEntry.getTitle() != null && !newEntry.getTitle().isEmpty()
                    ? newEntry.getTitle() : oldEntry.getTitle());
            oldEntry.setContent(newEntry.getContent() != null && !newEntry.getContent().isEmpty()
                    ? newEntry.getContent() : oldEntry.getContent());
            diaryEntryService.saveEntry(oldEntry);
            return new ResponseEntity<>(oldEntry, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    // ---- Trash / Recently Deleted ----

    @GetMapping("/trash")
    public ResponseEntity<?> getTrash() {
        List<DiaryEntry> trashed = diaryEntryService.getTrashEntries(getAuthenticatedUsername());
        List<TrashEntryDTO> response = trashed.stream()
                .map(entry -> new TrashEntryDTO(
                        entry.getId(),
                        entry.getTitle(),
                        entry.getContent(),
                        entry.getDeletedAt(),
                        diaryEntryService.calculateDaysRemaining(entry)))
                .collect(Collectors.toList());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("id/{myId}/restore")
    public ResponseEntity<?> restoreDiaryEntry(@PathVariable Long myId) {
        boolean restored = diaryEntryService.restoreById(myId, getAuthenticatedUsername());
        if (restored) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/trash/{myId}")
    public ResponseEntity<?> permanentlyDeleteOne(@PathVariable Long myId) {
        boolean removed = diaryEntryService.permanentlyDeleteById(myId, getAuthenticatedUsername());
        if (removed) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/trash")
    public ResponseEntity<?> permanentlyDeleteBatch(@RequestBody TrashDeleteRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return new ResponseEntity<>(
                    Collections.singletonMap("error", "No entry ids provided"), HttpStatus.BAD_REQUEST);
        }
        int deletedCount = diaryEntryService.permanentlyDeleteByIds(request.getIds(), getAuthenticatedUsername());
        return new ResponseEntity<>(Collections.singletonMap("deletedCount", deletedCount), HttpStatus.OK);
    }

    @DeleteMapping("/trash/all")
    public ResponseEntity<?> emptyTrash() {
        int deletedCount = diaryEntryService.permanentlyDeleteAllTrash(getAuthenticatedUsername());
        return new ResponseEntity<>(Collections.singletonMap("deletedCount", deletedCount), HttpStatus.OK);
    }
}