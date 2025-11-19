package com.polyshop.authservice;

import com.polyshop.authservice.domain.User;
import com.polyshop.authservice.domain.enums.Status;
import com.polyshop.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PasswordResetIntegrationTest {
    @Autowired
    UserRepository userRepository;

    @Test
    void smoke() {
        var u = new User();
        u.setUsername("t");
        u.setEmail("t@example.com");
        u.setPhone("000");
        u.setFullName("t");
        u.setPasswordHash("$2a$10$something");
        u.setStatus(Status.ACTIVE);
        userRepository.save(u);
        assertThat(userRepository.findByEmail("t@example.com")).isPresent();
    }
}
