package com.busbooking.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<BusCategory, Long> {
    List<BusCategory> findByIsActiveTrue();
    Optional<BusCategory> findByCode(String code);
    boolean existsByCode(String code);
}
