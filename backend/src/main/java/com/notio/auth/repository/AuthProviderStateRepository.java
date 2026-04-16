package com.notio.auth.repository;

import com.notio.auth.domain.AuthProviderState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthProviderStateRepository extends JpaRepository<AuthProviderState, Long> {

    Optional<AuthProviderState> findByState(String state);
}
