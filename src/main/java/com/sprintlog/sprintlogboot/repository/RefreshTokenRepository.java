package com.sprintlog.sprintlogboot.repository;

import com.sprintlog.sprintlogboot.domain.RefreshToken;
import com.sprintlog.sprintlogboot.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    long countByUserAndRevokedReasonIsNull(User user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken r set r.revokedReason = 'REVOKED_ALL' where r.user = :user and r.revokedReason is null")
    int revokeAllByUser(@Param("user") User user);


}












