package com.busbooking.booking;

import com.busbooking.trip.TripSeat;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "seat_allocation")
public class SeatAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_leg_id", nullable = false)
    private BookingLeg bookingLeg;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_seat_id", nullable = false)
    private TripSeat tripSeat;

    @Column(name = "board_stop_order", nullable = false)
    private int boardStopOrder;

    @Column(name = "alight_stop_order", nullable = false)
    private int alightStopOrder;

    @Column(name = "passenger_name", nullable = false, length = 150)
    private String passengerName;

    @Column(name = "passenger_age", nullable = false)
    private int passengerAge;

    @Column(name = "passenger_gender", nullable = false, length = 20)
    private String passengerGender;

    @Column(nullable = false, length = 30)
    private String status = "CONFIRMED";

    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public SeatAllocation() {
    }

    public SeatAllocation(BookingLeg bookingLeg, TripSeat tripSeat, int boardStopOrder, int alightStopOrder,
                          String passengerName, int passengerAge, String passengerGender, String status) {
        this.bookingLeg = bookingLeg;
        this.tripSeat = tripSeat;
        this.boardStopOrder = boardStopOrder;
        this.alightStopOrder = alightStopOrder;
        this.passengerName = passengerName;
        this.passengerAge = passengerAge;
        this.passengerGender = passengerGender;
        this.status = status != null ? status : "CONFIRMED";
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BookingLeg getBookingLeg() {
        return bookingLeg;
    }

    public void setBookingLeg(BookingLeg bookingLeg) {
        this.bookingLeg = bookingLeg;
    }

    public TripSeat getTripSeat() {
        return tripSeat;
    }

    public void setTripSeat(TripSeat tripSeat) {
        this.tripSeat = tripSeat;
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

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public int getPassengerAge() {
        return passengerAge;
    }

    public void setPassengerAge(int passengerAge) {
        this.passengerAge = passengerAge;
    }

    public String getPassengerGender() {
        return passengerGender;
    }

    public void setPassengerGender(String passengerGender) {
        this.passengerGender = passengerGender;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void setHoldExpiresAt(Instant holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
