package com.busbooking.station;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<Station, Long> {
    Optional<Station> findByCode(String code);
    boolean existsByCode(String code);
    List<Station> findByIsActiveTrueOrderByNameAsc();
    List<Station> findAllByOrderByNameAsc();
}
