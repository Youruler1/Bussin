package com.busbooking.booking;

import com.busbooking.auth.Role;
import com.busbooking.auth.User;
import com.busbooking.auth.UserRepository;
import com.busbooking.booking.dto.CreateBookingRequest;
import com.busbooking.booking.dto.LegAllocationRequest;
import com.busbooking.booking.dto.PassengerLegBooking;
import com.busbooking.bus.Bus;
import com.busbooking.bus.BusRepository;
import com.busbooking.bus.Seat;
import com.busbooking.bus.SeatRepository;
import com.busbooking.category.BusCategory;
import com.busbooking.category.CategoryRepository;
import com.busbooking.common.exception.ResourceConflictException;
import com.busbooking.route.Route;
import com.busbooking.route.RouteRepository;
import com.busbooking.route.RouteStop;
import com.busbooking.route.RouteStopRepository;
import com.busbooking.station.Station;
import com.busbooking.station.StationRepository;
import com.busbooking.trip.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(com.busbooking.TestSecurityConfig.class)
class BookingConcurrencyIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SeatAllocationRepository seatAllocationRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripStopRepository tripStopRepository;

    @Autowired
    private TripStopFareRepository tripStopFareRepository;

    @Autowired
    private TripSeatRepository tripSeatRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private RouteStopRepository routeStopRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.busbooking.TestDatabaseCleaner testDatabaseCleaner;

    private Station delhi;
    private Station agra;
    private Station lucknow;
    private Trip trip;
    private Seat targetSeat;
    private User customer;

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.clearAll();

        customer = userRepository.save(new User("conc@test.com", "pass", "Concurrency User", "+919000011111", Role.ROLE_CUSTOMER));

        delhi = stationRepository.save(new Station("NDLS", "Delhi", "Delhi", "Delhi", "Asia/Kolkata"));
        agra = stationRepository.save(new Station("AGR", "Agra", "Agra", "UP", "Asia/Kolkata"));
        lucknow = stationRepository.save(new Station("LKO", "Lucknow", "Lucknow", "UP", "Asia/Kolkata"));

        Route route = new Route("RT-CONC", "Delhi-Agra-Lucknow", delhi, lucknow, BigDecimal.valueOf(480.0));
        route.addStop(new RouteStop(route, delhi, 0, BigDecimal.ZERO, 0));
        route.addStop(new RouteStop(route, agra, 1, BigDecimal.valueOf(200.0), 200));
        route.addStop(new RouteStop(route, lucknow, 2, BigDecimal.valueOf(280.0), 280));
        route = routeRepository.save(route);

        BusCategory category = categoryRepository.save(new BusCategory("AC_DELUXE", "Deluxe AC", "AC Bus"));
        Bus bus = busRepository.save(new Bus("DL-CONC-01", "Volvo", "9600", category, 20));

        List<Seat> seats = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            seats.add(new Seat(bus, "S" + (i + 1), (i / 4) + 1, (i % 4) + 1, "SEATER", "LOWER"));
        }
        seats = seatRepository.saveAll(seats);
        targetSeat = seats.get(0); // Seat S1

        trip = new Trip("TRIP-CONC-01", route, bus, LocalDate.of(2026, 11, 1), "SCHEDULED",
                Instant.parse("2026-11-01T06:00:00Z"),
                Instant.parse("2026-11-01T14:00:00Z"));
        trip.addStop(new TripStop(trip, delhi, 0, null, Instant.parse("2026-11-01T06:00:00Z"), BigDecimal.ZERO));
        trip.addStop(new TripStop(trip, agra, 1, Instant.parse("2026-11-01T09:30:00Z"), Instant.parse("2026-11-01T09:45:00Z"), BigDecimal.valueOf(200.0)));
        trip.addStop(new TripStop(trip, lucknow, 2, Instant.parse("2026-11-01T14:00:00Z"), null, BigDecimal.valueOf(480.0)));
        trip.addFare(new TripStopFare(trip, 0, 1, BigDecimal.valueOf(400.00)));
        trip.addFare(new TripStopFare(trip, 1, 2, BigDecimal.valueOf(560.00)));
        trip.addFare(new TripStopFare(trip, 0, 2, BigDecimal.valueOf(950.00)));

        for (Seat s : seats) {
            trip.addSeat(new TripSeat(trip, s));
        }
        trip = tripRepository.save(trip);
    }

    @Test
    @DisplayName("R08: Concurrency Stress Test: 20 simultaneous threads attempting the exact same seat and segment produce exactly 1 success and 19 conflicts")
    void testConcurrentBookingSameSeatSameSegment() throws Exception {
        int numberOfThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger otherErrorsCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIdx = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // All 20 threads wait here until triggered simultaneously

                    CreateBookingRequest request = new CreateBookingRequest(
                            "thread" + threadIdx + "@test.com",
                            "+9198000" + String.format("%05d", threadIdx),
                            List.of(new PassengerLegBooking("Passenger " + threadIdx, 25 + (threadIdx % 20), "MALE",
                                    List.of(new LegAllocationRequest(trip.getId(), targetSeat.getId(), 0, 1)))) // Delhi -> Agra
                    );

                    bookingService.createBooking(customer.getId(), request, "idem-thread-" + threadIdx);
                    successCount.incrementAndGet();
                } catch (ResourceConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    otherErrorsCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Wait for all 20 threads to become ready
        assertTrue(readyLatch.await(10, TimeUnit.SECONDS), "All threads should be ready within 10s");

        // Fire all 20 threads simultaneously!
        startLatch.countDown();

        // Wait for all 20 threads to complete execution
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All threads should complete within 30s");
        executor.shutdown();

        // STRICT CONCURRENCY ASSERTIONS:
        assertEquals(1, successCount.get(), "Exactly 1 concurrent attempt must succeed");
        assertEquals(19, conflictCount.get(), "Exactly 19 concurrent attempts must fail with ResourceConflictException");
        assertEquals(0, otherErrorsCount.get(), "Zero unexpected errors should occur");

        // DATABASE STATE ASSERTIONS:
        assertEquals(1, bookingRepository.count(), "Database must contain exactly 1 confirmed booking");
        assertEquals(1, seatAllocationRepository.count(), "Database must contain exactly 1 active seat allocation");
    }

    @Test
    @DisplayName("R08: Concurrent Disjoint Segments: 10 threads on [0, 1) and 10 threads on [1, 2) on the same seat produce exactly 1 success each (2 total)")
    void testConcurrentDisjointSegmentsHighThroughput() throws Exception {
        int threadsPerSegment = 10;
        int totalThreads = threadsPerSegment * 2; // 20 threads total
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);

        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        AtomicInteger leg1SuccessCount = new AtomicInteger(0);
        AtomicInteger leg2SuccessCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            final int threadIdx = i;
            final boolean isFirstSegment = (i % 2 == 0); // 10 threads for [0, 1), 10 threads for [1, 2)
            final int board = isFirstSegment ? 0 : 1;
            final int alight = isFirstSegment ? 1 : 2;

            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();

                    CreateBookingRequest request = new CreateBookingRequest(
                            "disjoint" + threadIdx + "@test.com",
                            "+9198111" + String.format("%05d", threadIdx),
                            List.of(new PassengerLegBooking("Passenger " + threadIdx, 25, "FEMALE",
                                    List.of(new LegAllocationRequest(trip.getId(), targetSeat.getId(), board, alight))))
                    );

                    bookingService.createBooking(customer.getId(), request, "idem-disjoint-" + threadIdx);
                    if (isFirstSegment) {
                        leg1SuccessCount.incrementAndGet();
                    } else {
                        leg2SuccessCount.incrementAndGet();
                    }
                } catch (ResourceConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactly 1 winner on [0, 1) and exactly 1 winner on [1, 2)
        assertEquals(1, leg1SuccessCount.get(), "Exactly 1 booking must succeed on segment [0, 1)");
        assertEquals(1, leg2SuccessCount.get(), "Exactly 1 booking must succeed on disjoint segment [1, 2)");
        assertEquals(18, conflictCount.get(), "18 remaining threads must receive conflict");

        // Exactly 2 active allocations exist for targetSeat on the 2 disjoint segments
        assertEquals(2, bookingRepository.count());
        assertEquals(2, seatAllocationRepository.count());
    }

    @Test
    @DisplayName("R08: Concurrent Idempotency: 10 concurrent threads submitting the exact same Idempotency-Key produce exactly 1 DB record and identical references")
    void testConcurrentIdempotencyDeduplication() throws Exception {
        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        String sharedIdempotencyKey = "shared-idempotency-token-" + UUID.randomUUID();
        ConcurrentLinkedQueue<String> returnedBookingReferences = new ConcurrentLinkedQueue<>();
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIdx = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();

                    CreateBookingRequest request = new CreateBookingRequest(
                            "idempotent" + threadIdx + "@test.com",
                            "+9198222" + String.format("%05d", threadIdx),
                            List.of(new PassengerLegBooking("Same Passenger", 30, "MALE",
                                    List.of(new LegAllocationRequest(trip.getId(), targetSeat.getId(), 0, 1))))
                    );

                    var response = bookingService.createBooking(customer.getId(), request, sharedIdempotencyKey);
                    returnedBookingReferences.add(response.bookingReference());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // In a race where multiple threads hit concurrently with the same idempotency key,
                    // any unique constraint violation on idempotency_key is caught or one commits first
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        // Database must contain strictly 1 booking and 1 allocation
        assertEquals(1, bookingRepository.count(), "DB must contain strictly 1 booking record");
        assertEquals(1, seatAllocationRepository.count(), "DB must contain strictly 1 allocation");

        // All successful thread responses must share the exact same booking reference
        assertFalse(returnedBookingReferences.isEmpty());
        String expectedReference = returnedBookingReferences.peek();
        for (String ref : returnedBookingReferences) {
            assertEquals(expectedReference, ref, "All concurrent idempotent responses must return the identical booking reference");
        }
    }
}
