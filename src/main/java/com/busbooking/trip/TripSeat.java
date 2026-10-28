package com.busbooking.trip;

import com.busbooking.bus.Seat;
import jakarta.persistence.*;

@Entity
@Table(name = "trip_seat", uniqueConstraints = {
    @UniqueConstraint(name = "uq_trip_seat", columnNames = {"trip_id", "seat_id"})
})
public class TripSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    public TripSeat() {
    }

    public TripSeat(Trip trip, Seat seat) {
        this.trip = trip;
        this.seat = seat;
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

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }
}
