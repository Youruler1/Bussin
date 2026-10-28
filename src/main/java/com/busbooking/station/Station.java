package com.busbooking.station;

import jakarta.persistence.*;

@Entity
@Table(name = "station", uniqueConstraints = {
    @UniqueConstraint(name = "uq_station_code", columnNames = {"code"})
})
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 50)
    private String timezone = "Asia/Kolkata";

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public Station() {
    }

    public Station(String code, String name, String city, String state, String timezone) {
        this.code = code;
        this.name = name;
        this.city = city;
        this.state = state;
        this.timezone = (timezone != null && !timezone.isBlank()) ? timezone : "Asia/Kolkata";
        this.isActive = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
