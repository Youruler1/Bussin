package com.busbooking.booking;

import com.busbooking.station.Station;
import com.busbooking.trip.Trip;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "booking_leg", uniqueConstraints = {
    @UniqueConstraint(name = "uq_booking_leg_sequence", columnNames = {"booking_id", "leg_sequence"})
})
public class BookingLeg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "leg_sequence", nullable = false)
    private int legSequence = 1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_station_id", nullable = false)
    private Station boardStation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alight_station_id", nullable = false)
    private Station alightStation;

    @Column(name = "board_stop_order", nullable = false)
    private int boardStopOrder;

    @Column(name = "alight_stop_order", nullable = false)
    private int alightStopOrder;

    @Column(name = "fare_snapshot_inr", nullable = false, precision = 10, scale = 2)
    private BigDecimal fareSnapshotInr;

    @OneToMany(mappedBy = "bookingLeg", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SeatAllocation> allocations = new ArrayList<>();

    public BookingLeg() {
    }

    public BookingLeg(Booking booking, Trip trip, int legSequence, Station boardStation, Station alightStation,
                      int boardStopOrder, int alightStopOrder, BigDecimal fareSnapshotInr) {
        this.booking = booking;
        this.trip = trip;
        this.legSequence = legSequence;
        this.boardStation = boardStation;
        this.alightStation = alightStation;
        this.boardStopOrder = boardStopOrder;
        this.alightStopOrder = alightStopOrder;
        this.fareSnapshotInr = fareSnapshotInr;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public int getLegSequence() {
        return legSequence;
    }

    public void setLegSequence(int legSequence) {
        this.legSequence = legSequence;
    }

    public Station getBoardStation() {
        return boardStation;
    }

    public void setBoardStation(Station boardStation) {
        this.boardStation = boardStation;
    }

    public Station getAlightStation() {
        return alightStation;
    }

    public void setAlightStation(Station alightStation) {
        this.alightStation = alightStation;
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

    public BigDecimal getFareSnapshotInr() {
        return fareSnapshotInr;
    }

    public void setFareSnapshotInr(BigDecimal fareSnapshotInr) {
        this.fareSnapshotInr = fareSnapshotInr;
    }

    public List<SeatAllocation> getAllocations() {
        return allocations;
    }

    public void setAllocations(List<SeatAllocation> allocations) {
        this.allocations = allocations;
    }

    public void addAllocation(SeatAllocation allocation) {
        allocations.add(allocation);
        allocation.setBookingLeg(this);
    }
}
