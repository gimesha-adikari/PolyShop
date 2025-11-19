package com.polyshop.authservice.repository;

import com.polyshop.authservice.domain.Ban;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BanRepository extends JpaRepository<Ban, Long> {
    Optional<Ban> findByKey(String key);
    void deleteByKey(String key);
}
