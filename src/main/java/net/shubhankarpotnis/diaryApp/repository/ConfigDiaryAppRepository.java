package net.shubhankarpotnis.diaryApp.repository;

import net.shubhankarpotnis.diaryApp.entity.ConfigDiaryAppEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigDiaryAppRepository extends JpaRepository<ConfigDiaryAppEntity, Long> {

}