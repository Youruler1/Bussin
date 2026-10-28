-- ============================================================================
-- V2__seed_reference_data.sql: Baseline Reference & Seed Dataset
-- ============================================================================

-- 1. Bootstrap Admin & Demo Customer (Password: Password@123)
-- BCrypt hash for 'Password@123'
INSERT INTO app_user (email, password_hash, full_name, phone, role, is_active)
VALUES 
    ('admin@busbooking.local', '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'System Administrator', '+919876543210', 'ROLE_ADMIN', TRUE),
    ('customer@demo.local', '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Demo Customer', '+919123456789', 'ROLE_CUSTOMER', TRUE)
ON CONFLICT (email) DO NOTHING;

-- 2. Bus Categories
INSERT INTO bus_category (code, name, description, is_active)
VALUES 
    ('AC_SLEEPER', 'AC Sleeper', 'Air-conditioned luxury sleeper berths', TRUE),
    ('NON_AC_SEATER', 'Non-AC Seater', 'Economical standard push-back seats', TRUE),
    ('AC_CHAIR_CAR', 'AC Chair Car', 'Comfortable reclining chair car with AC', TRUE),
    ('LUXURY_VOLVO', 'Luxury Volvo Multi-Axle', 'Premium Volvo multi-axle bus with extra legroom', TRUE)
ON CONFLICT (code) DO NOTHING;

-- 3. Stations (6 stations in India)
INSERT INTO station (code, name, city, state, timezone, is_active)
VALUES 
    ('NDLS', 'New Delhi Central Bus Terminus', 'New Delhi', 'Delhi', 'Asia/Kolkata', TRUE),
    ('AGR', 'Agra Cantt Bus Stand', 'Agra', 'Uttar Pradesh', 'Asia/Kolkata', TRUE),
    ('JPR', 'Jaipur Sindhi Camp', 'Jaipur', 'Rajasthan', 'Asia/Kolkata', TRUE),
    ('CDG', 'Chandigarh Sector 43 ISBT', 'Chandigarh', 'Punjab', 'Asia/Kolkata', TRUE),
    ('LKO', 'Lucknow Alambagh Bus Terminal', 'Lucknow', 'Uttar Pradesh', 'Asia/Kolkata', TRUE),
    ('KNP', 'Kanpur Central ISBT', 'Kanpur', 'Uttar Pradesh', 'Asia/Kolkata', TRUE)
ON CONFLICT (code) DO NOTHING;

-- 4. Routes
-- Route 1: Delhi -> Agra -> Lucknow (480 km)
INSERT INTO route (route_code, name, origin_station_id, destination_station_id, total_distance_km, is_active)
VALUES ('RT-DEL-LKO', 'New Delhi to Lucknow via Agra', 1, 5, 480.00, TRUE)
ON CONFLICT (route_code) DO NOTHING;

-- Route 2: Lucknow -> Kanpur (85 km)
INSERT INTO route (route_code, name, origin_station_id, destination_station_id, total_distance_km, is_active)
VALUES ('RT-LKO-KNP', 'Lucknow to Kanpur Express', 5, 6, 85.00, TRUE)
ON CONFLICT (route_code) DO NOTHING;

-- Route 3: Delhi -> Chandigarh (250 km)
INSERT INTO route (route_code, name, origin_station_id, destination_station_id, total_distance_km, is_active)
VALUES ('RT-DEL-CDG', 'New Delhi to Chandigarh Direct', 1, 4, 250.00, TRUE)
ON CONFLICT (route_code) DO NOTHING;

-- Route Stops
-- Route 1: Delhi (0) -> Agra (1) -> Lucknow (2)
INSERT INTO route_stop (route_id, station_id, stop_order, distance_from_prev_km, typical_duration_minutes)
VALUES 
    (1, 1, 0, 0.00, 0),
    (1, 2, 1, 200.00, 210),
    (1, 5, 2, 280.00, 270)
ON CONFLICT (route_id, stop_order) DO NOTHING;

-- Route 2: Lucknow (0) -> Kanpur (1)
INSERT INTO route_stop (route_id, station_id, stop_order, distance_from_prev_km, typical_duration_minutes)
VALUES 
    (2, 5, 0, 0.00, 0),
    (2, 6, 1, 85.00, 90)
ON CONFLICT (route_id, stop_order) DO NOTHING;

-- Route 3: Delhi (0) -> Chandigarh (1)
INSERT INTO route_stop (route_id, station_id, stop_order, distance_from_prev_km, typical_duration_minutes)
VALUES 
    (3, 1, 0, 0.00, 0),
    (3, 4, 1, 250.00, 240)
ON CONFLICT (route_id, stop_order) DO NOTHING;

-- 5. Buses (3 buses)
INSERT INTO bus (registration_number, brand, model, category_id, total_capacity, is_active)
VALUES 
    ('DL-01-AB-1234', 'Volvo', '9600 Multi-Axle', 4, 30, TRUE),
    ('UP-32-CD-5678', 'Tata Motors', 'Starbus Prime AC', 3, 30, TRUE),
    ('HR-02-EF-9012', 'Scania', 'Metrolink Comfort', 1, 20, TRUE)
ON CONFLICT (registration_number) DO NOTHING;

-- 6. Generate Seats for Bus 1 (30 seats: 10 rows x 3 columns)
DO $$
DECLARE
    r INT;
    c INT;
    s_num VARCHAR(10);
BEGIN
    FOR r IN 1..10 LOOP
        FOR c IN 1..3 LOOP
            s_num := r || CHR(64 + c);
            INSERT INTO seat (bus_id, seat_number, row_number, column_number, seat_type, deck, is_active)
            VALUES (1, s_num, r, c, 'SEATER', 'LOWER', TRUE)
            ON CONFLICT (bus_id, seat_number) DO NOTHING;
        END LOOP;
    END LOOP;
END $$;

-- Generate Seats for Bus 2 (30 seats: 10 rows x 3 columns)
DO $$
DECLARE
    r INT;
    c INT;
    s_num VARCHAR(10);
BEGIN
    FOR r IN 1..10 LOOP
        FOR c IN 1..3 LOOP
            s_num := r || CHR(64 + c);
            INSERT INTO seat (bus_id, seat_number, row_number, column_number, seat_type, deck, is_active)
            VALUES (2, s_num, r, c, 'SEATER', 'LOWER', TRUE)
            ON CONFLICT (bus_id, seat_number) DO NOTHING;
        END LOOP;
    END LOOP;
END $$;

-- Generate Seats for Bus 3 (20 sleeper berths: 10 rows x 2 columns)
DO $$
DECLARE
    r INT;
    c INT;
    s_num VARCHAR(10);
BEGIN
    FOR r IN 1..10 LOOP
        FOR c IN 1..2 LOOP
            s_num := 'S' || r || CHR(64 + c);
            INSERT INTO seat (bus_id, seat_number, row_number, column_number, seat_type, deck, is_active)
            VALUES (3, s_num, r, c, 'SLEEPER', 'LOWER', TRUE)
            ON CONFLICT (bus_id, seat_number) DO NOTHING;
        END LOOP;
    END LOOP;
END $$;
