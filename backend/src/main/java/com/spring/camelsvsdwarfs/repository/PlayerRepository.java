package com.spring.camelsvsdwarfs.repository;

import com.spring.camelsvsdwarfs.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<Player, UUID>, JpaSpecificationExecutor<Player> {
    boolean existsByNickname(String nickname);
    Optional<Player> findByNickname(String nickname);
}
