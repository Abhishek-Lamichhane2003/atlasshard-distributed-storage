package com.atlasshard.storage.repo;

import com.atlasshard.storage.model.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserAccount, String> {
    Optional<UserAccount> findByEmail(String email);
}
