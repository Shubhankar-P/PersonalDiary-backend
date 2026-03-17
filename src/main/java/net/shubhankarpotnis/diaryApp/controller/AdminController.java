package net.shubhankarpotnis.diaryApp.controller;

import net.shubhankarpotnis.diaryApp.cache.AppCache;
import net.shubhankarpotnis.diaryApp.dto.UserResponseDto;
import net.shubhankarpotnis.diaryApp.entity.User;
import net.shubhankarpotnis.diaryApp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private AppCache appCache;

    @GetMapping("/all-users")
    public ResponseEntity<?> getAllUsers() {
        List<User> all = userService.getAllUsers();
        if (all == null || all.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        // Map to DTO — never return passwords
        List<UserResponseDto> response = all.stream().map(u -> new UserResponseDto(
                u.getId() != null ? u.getId().toHexString() : null,
                u.getUserName(),
                u.getRoles(),
                u.getDiaryEntries() != null ? u.getDiaryEntries().size() : 0
        )).collect(Collectors.toList());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/create-admin-user")
    public ResponseEntity<?> createAdminUser(@Valid @RequestBody User user) {
        try {
            userService.saveAdmin(user);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(
                    Collections.singletonMap("error", e.getMessage()),
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                    Collections.singletonMap("error", "Could not create admin user"),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/clear-app-cache")
    public ResponseEntity<?> clearAppCache() {
        appCache.init();
        return new ResponseEntity<>(
                Collections.singletonMap("message", "Cache cleared successfully"),
                HttpStatus.OK
        );
    }
}