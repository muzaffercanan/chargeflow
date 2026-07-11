package com.chargesquare.session.repository;

import java.util.List;
import java.util.Optional;

import com.chargesquare.session.domain.ChargingSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChargingSessionRepository extends JpaRepository<ChargingSession, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select chargingSession from ChargingSession chargingSession join fetch chargingSession.user "
            + "where chargingSession.id = :id")
    Optional<ChargingSession> findByIdForUpdate(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = "user")
    Optional<ChargingSession> findById(Long id);

    @EntityGraph(attributePaths = "user")
    List<ChargingSession> findAllByUser_IdOrderByStartedAtDesc(Long userId);
}
