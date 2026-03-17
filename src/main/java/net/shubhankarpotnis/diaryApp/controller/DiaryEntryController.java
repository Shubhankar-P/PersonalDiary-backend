package net.shubhankarpotnis.diaryApp.controller;

import net.shubhankarpotnis.diaryApp.entity.DiaryEntry;
import net.shubhankarpotnis.diaryApp.entity.User;
import net.shubhankarpotnis.diaryApp.service.DiaryEntryService;
import net.shubhankarpotnis.diaryApp.service.UserService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
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
        User user = userService.findByUserName(getAuthenticatedUsername());
        List<DiaryEntry> all = user.getDiaryEntries();
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
    public ResponseEntity<DiaryEntry> getDiaryEntryById(@PathVariable ObjectId myId) {
        User user = userService.findByUserName(getAuthenticatedUsername());
        List<DiaryEntry> collect = user.getDiaryEntries().stream()
                .filter(x -> x.getId().equals(myId)).collect(Collectors.toList());
        if (!collect.isEmpty()) {
            Optional<DiaryEntry> diaryEntry = diaryEntryService.findById(myId);
            if (diaryEntry.isPresent()) {
                return new ResponseEntity<>(diaryEntry.get(), HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("id/{myId}")
    public ResponseEntity<?> deleteDiaryEntryById(@PathVariable ObjectId myId) {
        boolean removed = diaryEntryService.deleteById(myId, getAuthenticatedUsername());
        if (removed) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PutMapping("/id/{myId}")
    public ResponseEntity<?> updateDiaryById(@PathVariable ObjectId myId,
                                             @Valid @RequestBody DiaryEntry newEntry,
                                             BindingResult result) {
        if (result.hasErrors()) {
            String errorMsg = result.getAllErrors().get(0).getDefaultMessage();
            return new ResponseEntity<>(Collections.singletonMap("error", errorMsg), HttpStatus.BAD_REQUEST);
        }
        User user = userService.findByUserName(getAuthenticatedUsername());
        List<DiaryEntry> collect = user.getDiaryEntries().stream()
                .filter(x -> x.getId().equals(myId)).collect(Collectors.toList());
        if (!collect.isEmpty()) {
            Optional<DiaryEntry> diaryEntry = diaryEntryService.findById(myId);
            if (diaryEntry.isPresent()) {
                DiaryEntry oldEntry = diaryEntry.get();
                oldEntry.setTitle(newEntry.getTitle() != null && !newEntry.getTitle().isEmpty()
                        ? newEntry.getTitle() : oldEntry.getTitle());
                oldEntry.setContent(newEntry.getContent() != null && !newEntry.getContent().isEmpty()
                        ? newEntry.getContent() : oldEntry.getContent());
                diaryEntryService.saveEntry(oldEntry);
                return new ResponseEntity<>(oldEntry, HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}