package com.busbooking.bus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByBusIdOrderByRowNumberAscColumnNumberAsc(Long busId);
    List<Seat> findByBusIdAndIsActiveTrueOrderByRowNumberAscColumnNumberAsc(Long busId);
    long countByBusId(Long busId);
    void deleteByBusId(Long busId);
}
