package net.shubhankarpotnis.diaryApp.repository;

import net.shubhankarpotnis.diaryApp.entity.DiaryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, Long> {

}