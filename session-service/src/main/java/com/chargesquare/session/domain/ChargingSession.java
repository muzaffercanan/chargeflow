package com.chargesquare.session.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "charging_sessions", schema = "session")
public class ChargingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "connector_id", nullable = false)
    private Long connectorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "energy_kwh", precision = 19, scale = 6)
    private BigDecimal energyKwh;

    @Column(precision = 19, scale = 2)
    private BigDecimal cost;

    @Embedded
    private TariffSnapshot tariffSnapshot;

    protected ChargingSession() {
    }

    public ChargingSession(User user, Long connectorId, Instant startedAt, TariffSnapshot tariffSnapshot) {
        this.user = user;
        this.connectorId = connectorId;
        this.startedAt = startedAt;
        this.tariffSnapshot = tariffSnapshot;
        this.status = SessionStatus.ACTIVE;
    }

    public void complete(BigDecimal energyKwh, BigDecimal cost, Instant endedAt) {
        this.energyKwh = energyKwh;
        this.cost = cost;
        this.endedAt = endedAt;
        this.status = SessionStatus.COMPLETED;
    }

    public Long getId() { return id; }
    public Long getUserId() { return user.getId(); }
    public Long getConnectorId() { return connectorId; }
    public SessionStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public BigDecimal getEnergyKwh() { return energyKwh; }
    public BigDecimal getCost() { return cost; }
    public TariffSnapshot getTariffSnapshot() { return tariffSnapshot; }
}

