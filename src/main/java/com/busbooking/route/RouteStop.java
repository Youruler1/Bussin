package com.busbooking.route;

import com.busbooking.station.Station;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "route_stop", uniqueConstraints = {
    @UniqueConstraint(name = "uq_route_stop_order", columnNames = {"route_id", "stop_order"}),
    @UniqueConstraint(name = "uq_route_stop_station", columnNames = {"route_id", "station_id"})
})
public class RouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(name = "stop_order", nullable = false)
    private int stopOrder;

    @Column(name = "distance_from_prev_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal distanceFromPrevKm = BigDecimal.ZERO;

    @Column(name = "typical_duration_minutes", nullable = false)
    private int typicalDurationMinutes = 0;

    public RouteStop() {
    }

    public RouteStop(Route route, Station station, int stopOrder, BigDecimal distanceFromPrevKm, int typicalDurationMinutes) {
        this.route = route;
        this.station = station;
        this.stopOrder = stopOrder;
        this.distanceFromPrevKm = distanceFromPrevKm != null ? distanceFromPrevKm : BigDecimal.ZERO;
        this.typicalDurationMinutes = typicalDurationMinutes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
    }

    public int getStopOrder() {
        return stopOrder;
    }

    public void setStopOrder(int stopOrder) {
        this.stopOrder = stopOrder;
    }

    public BigDecimal getDistanceFromPrevKm() {
        return distanceFromPrevKm;
    }

    public void setDistanceFromPrevKm(BigDecimal distanceFromPrevKm) {
        this.distanceFromPrevKm = distanceFromPrevKm;
    }

    public int getTypicalDurationMinutes() {
        return typicalDurationMinutes;
    }

    public void setTypicalDurationMinutes(int typicalDurationMinutes) {
        this.typicalDurationMinutes = typicalDurationMinutes;
    }
}
