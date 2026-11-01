package net.shubhankarpotnis.diaryApp.repository;

import net.shubhankarpotnis.diaryApp.entity.DiaryEntry;
import net.shubhankarpotnis.diaryApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, Long> {

    List<DiaryEntry> findByUserAndDeletedFalseOrderByDateDesc(User user);

    List<DiaryEntry> findByUserAndDeletedTrueOrderByDeletedAtDesc(User user);

    Optional<DiaryEntry> findByIdAndUserAndDeletedTrue(Long id, User user);

    List<DiaryEntry> findByDeletedTrueAndDeletedAtBefore(OffsetDateTime cutoff);
}