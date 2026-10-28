package com.busbooking.trip;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "trip_stop_fare", uniqueConstraints = {
    @UniqueConstraint(name = "uq_trip_stop_fare", columnNames = {"trip_id", "board_stop_order", "alight_stop_order"})
})
public class TripStopFare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "board_stop_order", nullable = false)
    private int boardStopOrder;

    @Column(name = "alight_stop_order", nullable = false)
    private int alightStopOrder;

    @Column(name = "base_fare_inr", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFareInr;

    public TripStopFare() {
    }

    public TripStopFare(Trip trip, int boardStopOrder, int alightStopOrder, BigDecimal baseFareInr) {
        this.trip = trip;
        this.boardStopOrder = boardStopOrder;
        this.alightStopOrder = alightStopOrder;
        this.baseFareInr = baseFareInr;
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

    public int getBoardStopOrder() {
        return boardStopOrder;
    }

    public void setBoardStopOrder(int boardStopOrder) {
        this.boardStopOrder = boardStopOrder;
    }

    public int getAlightStopOrder() {
        return alightStopOrder;
    }

    public void setAlightStopOrder(int alightStopOrder) {
        this.alightStopOrder = alightStopOrder;
    }

    public BigDecimal getBaseFareInr() {
        return baseFareInr;
    }

    public void setBaseFareInr(BigDecimal baseFareInr) {
        this.baseFareInr = baseFareInr;
    }
}
