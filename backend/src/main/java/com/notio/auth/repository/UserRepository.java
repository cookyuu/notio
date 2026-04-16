package com.notio.auth.repository;

import com.notio.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL AND u.status = com.notio.auth.domain.UserStatus.ACTIVE")
    java.util.Optional<User> findActiveById(@Param("id") Long id);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    boolean existsActiveById(@Param("id") Long id);
}
