package com.busbooking.trip;

import com.busbooking.bus.Bus;
import com.busbooking.route.Route;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trip", uniqueConstraints = {
    @UniqueConstraint(name = "uq_trip_number", columnNames = {"trip_number"})
})
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trip_number", nullable = false, unique = true, length = 50)
    private String tripNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(nullable = false, length = 30)
    private String status = "SCHEDULED";

    @Column(name = "scheduled_departure_utc", nullable = false)
    private Instant scheduledDepartureUtc;

    @Column(name = "scheduled_arrival_utc", nullable = false)
    private Instant scheduledArrivalUtc;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stopOrder ASC")
    private List<TripStop> stops = new ArrayList<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripStopFare> fares = new ArrayList<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripSeat> seats = new ArrayList<>();

    public Trip() {
    }

    public Trip(String tripNumber, Route route, Bus bus, LocalDate serviceDate, String status,
                Instant scheduledDepartureUtc, Instant scheduledArrivalUtc) {
        this.tripNumber = tripNumber;
        this.route = route;
        this.bus = bus;
        this.serviceDate = serviceDate;
        this.status = status != null ? status : "SCHEDULED";
        this.scheduledDepartureUtc = scheduledDepartureUtc;
        this.scheduledArrivalUtc = scheduledArrivalUtc;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTripNumber() {
        return tripNumber;
    }

    public void setTripNumber(String tripNumber) {
        this.tripNumber = tripNumber;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getScheduledDepartureUtc() {
        return scheduledDepartureUtc;
    }

    public void setScheduledDepartureUtc(Instant scheduledDepartureUtc) {
        this.scheduledDepartureUtc = scheduledDepartureUtc;
    }

    public Instant getScheduledArrivalUtc() {
        return scheduledArrivalUtc;
    }

    public void setScheduledArrivalUtc(Instant scheduledArrivalUtc) {
        this.scheduledArrivalUtc = scheduledArrivalUtc;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<TripStop> getStops() {
        return stops;
    }

    public void setStops(List<TripStop> stops) {
        this.stops = stops;
    }

    public void addStop(TripStop stop) {
        stops.add(stop);
        stop.setTrip(this);
    }

    public List<TripStopFare> getFares() {
        return fares;
    }

    public void setFares(List<TripStopFare> fares) {
        this.fares = fares;
    }

    public void addFare(TripStopFare fare) {
        fares.add(fare);
        fare.setTrip(this);
    }

    public List<TripSeat> getSeats() {
        return seats;
    }

    public void setSeats(List<TripSeat> seats) {
        this.seats = seats;
    }

    public void addSeat(TripSeat seat) {
        seats.add(seat);
        seat.setTrip(this);
    }
}
