package net.shubhankarpotnis.diaryApp.service;

import lombok.extern.slf4j.Slf4j;
import net.shubhankarpotnis.diaryApp.entity.DiaryEntry;
import net.shubhankarpotnis.diaryApp.entity.User;
import net.shubhankarpotnis.diaryApp.repository.DiaryEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class DiaryEntryService {

    @Autowired
    private DiaryEntryRepository diaryEntryRepository;

    @Autowired
    private UserService userService;

    // Re-enabled: JPA transactions work fine on single-node Postgres. This only
    // broke on standalone MongoDB, which requires a replica set for
    // multi-document transactions.
    @Transactional
    public void saveEntry(DiaryEntry diaryEntry, String userName){
        try {
            User user = userService.findByUserName(userName);
            // The entry now owns the foreign key (user_id) directly, so we set
            // the owning side and save the entry itself. We no longer need to
            // touch/save the User at all -- that was only necessary under the
            // old @DBRef model where the User document held the array of
            // entry references.
            diaryEntry.setUser(user);
            diaryEntry.setDate(LocalDateTime.now());
            diaryEntryRepository.save(diaryEntry);
        } catch (Exception e){
            log.error("Exception ", e);
        }
    }

    public void saveEntry(DiaryEntry diaryEntry){
        diaryEntryRepository.save(diaryEntry);
    }

    public Optional<DiaryEntry> findById(Long id){
        return diaryEntryRepository.findById(id);
    }

    @Transactional
    public boolean deleteById(Long id, String userName){
        try {
            User user = userService.findByUserName(userName);
            Optional<DiaryEntry> entryOpt = diaryEntryRepository.findById(id);
            // Ownership check: confirm this entry actually belongs to the
            // authenticated user before deleting -- otherwise any logged-in
            // user could delete any entry by guessing an id.
            if (entryOpt.isPresent() && entryOpt.get().getUser().getId().equals(user.getId())) {
                diaryEntryRepository.deleteById(id);
                return true;
            }
            return false;
        } catch (Exception e){
            throw new RuntimeException("An error occurred while deleting the entry.", e);
        }
    }

}

// controller ---> service ---> repository