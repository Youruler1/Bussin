package com.busbooking.route;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    Optional<Route> findByRouteCode(String routeCode);
    boolean existsByRouteCode(String routeCode);
    List<Route> findByIsActiveTrue();

    @Query("SELECT COUNT(r) > 0 FROM Route r WHERE r.originStation.id = :stationId OR r.destinationStation.id = :stationId")
    boolean existsByStationIdAsTerminal(@Param("stationId") Long stationId);
}
