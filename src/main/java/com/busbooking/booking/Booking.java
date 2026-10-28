package com.busbooking.booking;

import com.busbooking.auth.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "booking", uniqueConstraints = {
    @UniqueConstraint(name = "uq_booking_reference", columnNames = {"booking_reference"}),
    @UniqueConstraint(name = "uq_booking_idempotency", columnNames = {"idempotency_key"})
})
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_reference", nullable = false, unique = true, length = 64)
    private String bookingReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(nullable = false, length = 30)
    private String status = "CONFIRMED";

    @Column(name = "total_amount_inr", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmountInr;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "contact_phone", nullable = false, length = 30)
    private String contactPhone;

    @Column(name = "booked_at", nullable = false, updatable = false)
    private Instant bookedAt = Instant.now();

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("legSequence ASC")
    private List<BookingLeg> legs = new ArrayList<>();

    public Booking() {
    }

    public Booking(String bookingReference, User customer, String status, BigDecimal totalAmountInr,
                   String contactEmail, String contactPhone, String idempotencyKey) {
        this.bookingReference = bookingReference;
        this.customer = customer;
        this.status = status != null ? status : "CONFIRMED";
        this.totalAmountInr = totalAmountInr;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.idempotencyKey = idempotencyKey;
        this.bookedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalAmountInr() {
        return totalAmountInr;
    }

    public void setTotalAmountInr(BigDecimal totalAmountInr) {
        this.totalAmountInr = totalAmountInr;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public Instant getBookedAt() {
        return bookedAt;
    }

    public void setBookedAt(Instant bookedAt) {
        this.bookedAt = bookedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public List<BookingLeg> getLegs() {
        return legs;
    }

    public void setLegs(List<BookingLeg> legs) {
        this.legs = legs;
    }

    public void addLeg(BookingLeg leg) {
        legs.add(leg);
        leg.setBooking(this);
    }
}
