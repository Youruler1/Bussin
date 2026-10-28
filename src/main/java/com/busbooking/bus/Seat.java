package com.busbooking.bus;

import jakarta.persistence.*;

@Entity
@Table(name = "seat", uniqueConstraints = {
    @UniqueConstraint(name = "uq_bus_seat", columnNames = {"bus_id", "seat_number"})
})
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @Column(name = "column_number", nullable = false)
    private int columnNumber;

    @Column(name = "seat_type", nullable = false, length = 30)
    private String seatType = "SEATER";

    @Column(name = "deck", nullable = false, length = 20)
    private String deck = "LOWER";

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public Seat() {
    }

    public Seat(Bus bus, String seatNumber, int rowNumber, int columnNumber, String seatType, String deck) {
        this.bus = bus;
        this.seatNumber = seatNumber;
        this.rowNumber = rowNumber;
        this.columnNumber = columnNumber;
        this.seatType = seatType != null ? seatType : "SEATER";
        this.deck = deck != null ? deck : "LOWER";
        this.isActive = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public void setColumnNumber(int columnNumber) {
        this.columnNumber = columnNumber;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public String getDeck() {
        return deck;
    }

    public void setDeck(String deck) {
        this.deck = deck;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
