package net.shubhankarpotnis.diaryApp.repository;

import net.shubhankarpotnis.diaryApp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUserName(String username);
    void deleteByUserName(String username);
}