package net.shubhankarpotnis.diaryApp.service;

import lombok.extern.slf4j.Slf4j;
import net.shubhankarpotnis.diaryApp.entity.DiaryEntry;
import net.shubhankarpotnis.diaryApp.entity.User;
import net.shubhankarpotnis.diaryApp.repository.DiaryEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class DiaryEntryService {

    private static final int RETENTION_DAYS = 30;

    @Autowired
    private DiaryEntryRepository diaryEntryRepository;

    @Autowired
    private UserService userService;

    @Transactional
    public void saveEntry(DiaryEntry diaryEntry, String userName){
        try {
            User user = userService.findByUserName(userName);
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

    public List<DiaryEntry> getActiveEntries(String userName){
        User user = userService.findByUserName(userName);
        return diaryEntryRepository.findByUserAndDeletedFalseOrderByDateDesc(user);
    }

    public List<DiaryEntry> getTrashEntries(String userName){
        User user = userService.findByUserName(userName);
        return diaryEntryRepository.findByUserAndDeletedTrueOrderByDeletedAtDesc(user);
    }

    public long calculateDaysRemaining(DiaryEntry entry){
        if (entry.getDeletedAt() == null) {
            return RETENTION_DAYS;
        }
        long daysElapsed = ChronoUnit.DAYS.between(entry.getDeletedAt(), OffsetDateTime.now());
        return Math.max(RETENTION_DAYS - daysElapsed, 0);
    }

    // Soft delete: flags the entry instead of removing it
    @Transactional
    public boolean deleteById(Long id, String userName){
        try {
            User user = userService.findByUserName(userName);
            Optional<DiaryEntry> entryOpt = diaryEntryRepository.findById(id);
            if (entryOpt.isPresent()
                    && entryOpt.get().getUser().getId().equals(user.getId())
                    && !entryOpt.get().isDeleted()) {
                DiaryEntry entry = entryOpt.get();
                entry.setDeleted(true);
                entry.setDeletedAt(OffsetDateTime.now());
                diaryEntryRepository.save(entry);
                return true;
            }
            return false;
        } catch (Exception e){
            throw new RuntimeException("An error occurred while deleting the entry.", e);
        }
    }

    @Transactional
    public boolean restoreById(Long id, String userName){
        User user = userService.findByUserName(userName);
        Optional<DiaryEntry> entryOpt = diaryEntryRepository.findByIdAndUserAndDeletedTrue(id, user);
        if (entryOpt.isEmpty()) {
            return false;
        }
        DiaryEntry entry = entryOpt.get();
        entry.setDeleted(false);
        entry.setDeletedAt(null);
        diaryEntryRepository.save(entry);
        return true;
    }

    @Transactional
    public boolean permanentlyDeleteById(Long id, String userName){
        User user = userService.findByUserName(userName);
        Optional<DiaryEntry> entryOpt = diaryEntryRepository.findByIdAndUserAndDeletedTrue(id, user);
        if (entryOpt.isEmpty()) {
            return false;
        }
        diaryEntryRepository.deleteById(id);
        return true;
    }

    @Transactional
    public int permanentlyDeleteByIds(List<Long> ids, String userName){
        User user = userService.findByUserName(userName);
        int deletedCount = 0;
        for (Long id : ids) {
            Optional<DiaryEntry> entryOpt = diaryEntryRepository.findByIdAndUserAndDeletedTrue(id, user);
            if (entryOpt.isPresent()) {
                diaryEntryRepository.deleteById(id);
                deletedCount++;
            }
        }
        return deletedCount;
    }

    @Transactional
    public int permanentlyDeleteAllTrash(String userName){
        User user = userService.findByUserName(userName);
        List<DiaryEntry> trashed = diaryEntryRepository.findByUserAndDeletedTrueOrderByDeletedAtDesc(user);
        diaryEntryRepository.deleteAll(trashed);
        return trashed.size();
    }

    // Daily 3 AM purge of anything past the 30-day window
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredEntries(){
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(RETENTION_DAYS);
        List<DiaryEntry> expired = diaryEntryRepository.findByDeletedTrueAndDeletedAtBefore(cutoff);
        if (!expired.isEmpty()) {
            log.info("Purging {} diary entries past the {}-day retention window", expired.size(), RETENTION_DAYS);
            diaryEntryRepository.deleteAll(expired);
        }
    }
}

// controller ---> service ---> repository