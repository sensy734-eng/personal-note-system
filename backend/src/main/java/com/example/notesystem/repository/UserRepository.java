package com.example.notesystem.repository;

import com.example.notesystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 只要按照命名规范写方法名，JPA 会自动生成 "SELECT * FROM users WHERE username = ?" 的 SQL
    Optional<User> findByUsername(String username);
}