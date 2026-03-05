package com.zaby.helpdesk.repository;

import com.zaby.helpdesk.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByDeletedFalse();
    Optional<User> findByIdAndDeletedFalse(Long id);
}
