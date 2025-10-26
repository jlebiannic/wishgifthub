package com.wishgifthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.UUID;

@SpringBootApplication
public class WishGiftHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(WishGiftHubApplication.class, args);
    }
}
package com.wishgifthub.repository;

import com.wishgifthub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}

