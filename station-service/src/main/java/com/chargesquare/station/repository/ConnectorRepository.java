package com.chargesquare.station.repository;

import java.util.List;
import java.util.Optional;

import com.chargesquare.station.domain.Connector;
import com.chargesquare.station.domain.ConnectorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConnectorRepository extends JpaRepository<Connector, Long> {

    @Query("""
            select connector from Connector connector
            join fetch connector.station
            join fetch connector.tariff
            where connector.id = :connectorId
            """)
    Optional<Connector> findDetailsById(@Param("connectorId") Long connectorId);

    @Query("""
            select connector from Connector connector
            join fetch connector.station
            join fetch connector.tariff
            where connector.station.id = :stationId
            order by connector.id
            """)
    List<Connector> findDetailsByStationId(@Param("stationId") Long stationId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Connector connector
            set connector.status = :targetStatus
            where connector.id = :connectorId
              and connector.status = :expectedStatus
            """)
    int transitionStatus(
            @Param("connectorId") Long connectorId,
            @Param("expectedStatus") ConnectorStatus expectedStatus,
            @Param("targetStatus") ConnectorStatus targetStatus);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Connector connector
            set connector.status = :targetStatus
            where connector.id = :connectorId
            """)
    int updateStatus(@Param("connectorId") Long connectorId, @Param("targetStatus") ConnectorStatus targetStatus);
}
