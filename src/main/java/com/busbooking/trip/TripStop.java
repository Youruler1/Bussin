package com.busbooking.trip;

import com.busbooking.station.Station;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "trip_stop", uniqueConstraints = {
    @UniqueConstraint(name = "uq_trip_stop_order", columnNames = {"trip_id", "stop_order"})
})
public class TripStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(name = "stop_order", nullable = false)
    private int stopOrder;

    @Column(name = "scheduled_arrival_utc")
    private Instant scheduledArrivalUtc;

    @Column(name = "scheduled_departure_utc")
    private Instant scheduledDepartureUtc;

    @Column(name = "cumulative_distance_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal cumulativeDistanceKm = BigDecimal.ZERO;

    public TripStop() {
    }

    public TripStop(Trip trip, Station station, int stopOrder,
                    Instant scheduledArrivalUtc, Instant scheduledDepartureUtc,
                    BigDecimal cumulativeDistanceKm) {
        this.trip = trip;
        this.station = station;
        this.stopOrder = stopOrder;
        this.scheduledArrivalUtc = scheduledArrivalUtc;
        this.scheduledDepartureUtc = scheduledDepartureUtc;
        this.cumulativeDistanceKm = cumulativeDistanceKm != null ? cumulativeDistanceKm : BigDecimal.ZERO;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
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

    public Instant getScheduledArrivalUtc() {
        return scheduledArrivalUtc;
    }

    public void setScheduledArrivalUtc(Instant scheduledArrivalUtc) {
        this.scheduledArrivalUtc = scheduledArrivalUtc;
    }

    public Instant getScheduledDepartureUtc() {
        return scheduledDepartureUtc;
    }

    public void setScheduledDepartureUtc(Instant scheduledDepartureUtc) {
        this.scheduledDepartureUtc = scheduledDepartureUtc;
    }

    public BigDecimal getCumulativeDistanceKm() {
        return cumulativeDistanceKm;
    }

    public void setCumulativeDistanceKm(BigDecimal cumulativeDistanceKm) {
        this.cumulativeDistanceKm = cumulativeDistanceKm;
    }
}
