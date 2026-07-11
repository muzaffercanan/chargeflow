package com.chargesquare.station.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "connectors", schema = "station")
public class Connector {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tariff_id", nullable = false)
    private Tariff tariff;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(name = "power_kw", nullable = false)
    private Integer powerKw;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConnectorStatus status;

    protected Connector() {
    }

    public Connector(Long id, Station station, Tariff tariff, String type, Integer powerKw, ConnectorStatus status) {
        this.id = id;
        this.station = station;
        this.tariff = tariff;
        this.type = type;
        this.powerKw = powerKw;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Station getStation() {
        return station;
    }

    public Tariff getTariff() {
        return tariff;
    }

    public String getType() {
        return type;
    }

    public Integer getPowerKw() {
        return powerKw;
    }

    public ConnectorStatus getStatus() {
        return status;
    }
}
