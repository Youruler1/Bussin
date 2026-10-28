import React, { useState, useEffect } from 'react';
import { apiRequest } from '../../api/client';

export default function SeatMapModal({ isOpen, onClose, journey, currentUser, onBookingConfirmed, setAlert }) {
  if (!isOpen || !journey) return null;

  const legs = journey.legs || [];
  const isConnecting = legs.length > 1;

  // Active leg tab for seat selection (0 or 1)
  const [activeLegIndex, setActiveLegIndex] = useState(0);

  // seatMaps cache: { [legIndex]: { seats: [], totalCapacity: 0 } }
  const [seatMaps, setSeatMaps] = useState({});
  const [loadingSeatMap, setLoadingSeatMap] = useState(true);

  // selections per leg: { [legIndex]: [seatId1, seatId2] }
  const [selectedSeatsByLeg, setSelectedSeatsByLeg] = useState({ 0: [] });

  // Passenger details array: [ { name: '', age: '', gender: 'MALE' } ]
  // Number of passengers is determined by the number of selected seats
  const [passengerDetails, setPassengerDetails] = useState([]);
  const [contactEmail, setContactEmail] = useState(currentUser?.email || '');
  const [contactPhone, setContactPhone] = useState(currentUser?.phone || '+919000000001');
  const [submitting, setSubmitting] = useState(false);

  // Fetch seat map for the active leg
  useEffect(() => {
    async function loadSeatMap() {
      const leg = legs[activeLegIndex];
      if (!leg) return;

      setLoadingSeatMap(true);
      try {
        const data = await apiRequest(
          `/api/v1/trips/${leg.tripId}/seats?boardIndex=${leg.boardStopOrder}&alightIndex=${leg.alightStopOrder}`
        );
        setSeatMaps((prev) => ({ ...prev, [activeLegIndex]: data }));
      } catch (err) {
        setAlert({
          type: 'error',
          title: 'Seat Map Failed',
          message: err.problem?.detail || err.message
        });
      } finally {
        setLoadingSeatMap(false);
      }
    }
    loadSeatMap();
  }, [activeLegIndex, journey]);

  // Handle seat click toggle for active leg
  function toggleSeatSelection(seat) {
    if (!seat.available) return;

    const currentLegSelections = selectedSeatsByLeg[activeLegIndex] || [];
    const isAlreadySelected = currentLegSelections.includes(seat.seatId);

    let nextSelections;
    if (isAlreadySelected) {
      nextSelections = currentLegSelections.filter((id) => id !== seat.seatId);
    } else {
      // In connecting journeys, ensure passenger count aligns across legs
      nextSelections = [...currentLegSelections, seat.seatId];
    }

    const updatedMap = {
      ...selectedSeatsByLeg,
      [activeLegIndex]: nextSelections
    };
    setSelectedSeatsByLeg(updatedMap);

    // Synchronize passenger count with max selected seats
    const maxSeatsSelected = Math.max(
      ...(Object.values(updatedMap).map((arr) => arr.length)),
      1
    );

    setPassengerDetails((prev) => {
      const copy = [...prev];
      while (copy.length < maxSeatsSelected) {
        copy.push({
          name: currentUser?.fullName || '',
          age: 28,
          gender: 'MALE'
        });
      }
      return copy.slice(0, maxSeatsSelected);
    });
  }

  function updatePassenger(index, field, value) {
    setPassengerDetails((prev) => {
      const copy = [...prev];
      copy[index] = { ...copy[index], [field]: value };
      return copy;
    });
  }

  // Calculate total price based on selected seats across legs
  const leg0Count = (selectedSeatsByLeg[0] || []).length;
  const leg1Count = isConnecting ? (selectedSeatsByLeg[1] || []).length : 0;
  const leg0Fare = legs[0]?.segmentFareInr || 0;
  const leg1Fare = isConnecting ? (legs[1]?.segmentFareInr || 0) : 0;
  const totalCalculatedPrice = (leg0Count * leg0Fare) + (leg1Count * leg1Fare);

  const currentSeatMap = seatMaps[activeLegIndex]?.seats || [];
  const currentSelections = selectedSeatsByLeg[activeLegIndex] || [];

  // Group seats by row for 2x2 grid rendering
  const seatsByRow = {};
  currentSeatMap.forEach((seat) => {
    if (!seatsByRow[seat.rowNumber]) {
      seatsByRow[seat.rowNumber] = [];
    }
    seatsByRow[seat.rowNumber].push(seat);
  });
  Object.values(seatsByRow).forEach((row) => row.sort((a, b) => a.columnNumber - b.columnNumber));

  async function handleBook(e) {
    e.preventDefault();
    if (!currentUser) {
      setAlert({
        type: 'error',
        title: 'Authentication Required',
        message: 'Please sign in or register before booking seats.'
      });
      return;
    }

    // Validation: each leg must have equal seat count matching passenger count
    for (let i = 0; i < legs.length; i++) {
      const legSelected = selectedSeatsByLeg[i] || [];
      if (legSelected.length === 0) {
        setAlert({
          type: 'error',
          title: 'Incomplete Seat Selection',
          message: `Please select seats for Leg ${i + 1} (${legs[i].originStationName} -> ${legs[i].destinationStationName}).`
        });
        return;
      }
      if (legSelected.length !== passengerDetails.length) {
        setAlert({
          type: 'error',
          title: 'Seat Count Mismatch',
          message: `You selected ${legSelected.length} seat(s) for Leg ${i + 1}, but have ${passengerDetails.length} passenger(s). Each leg must have matching seat counts.`
        });
        return;
      }
    }

    // Build passengerBookings payload
    const passengerBookings = passengerDetails.map((passenger, pIdx) => {
      const legAllocations = legs.map((leg, lIdx) => ({
        tripId: leg.tripId,
        seatId: selectedSeatsByLeg[lIdx][pIdx],
        boardStopOrder: leg.boardStopOrder,
        alightStopOrder: leg.alightStopOrder
      }));

      return {
        passengerName: passenger.name.trim(),
        passengerAge: parseInt(passenger.age, 10),
        passengerGender: passenger.gender,
        legAllocations
      };
    });

    const payload = {
      contactEmail: contactEmail.trim(),
      contactPhone: contactPhone.trim(),
      passengerBookings
    };

    setSubmitting(true);
    try {
      const idempotencyKey = 'idempotent-web-' + crypto.randomUUID();
      const booking = await apiRequest('/api/v1/bookings', {
        method: 'POST',
        headers: {
          'Idempotency-Key': idempotencyKey
        },
        body: payload
      });
      onBookingConfirmed(booking);
      onClose();
    } catch (err) {
      setAlert({
        type: 'error',
        title: 'Booking Conflict / Error',
        status: err.status,
        message: err.problem?.detail || err.message
      });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card seat-modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <h3 style={{ margin: 0 }}>Select Seats & Passenger Details</h3>
            <p style={{ margin: '0.25rem 0 0', fontSize: '0.85rem', color: '#64748b' }}>
              {isConnecting ? 'Multi-Leg Connecting Journey' : 'Direct Journey'} &bull; Base Fare: ₹{journey.totalFareInr}
            </p>
          </div>
          <button className="close-btn" onClick={onClose}>&times;</button>
        </div>

        {/* Connecting Leg Switcher Tabs */}
        {isConnecting && (
          <div className="leg-tabs">
            {legs.map((leg, idx) => (
              <button
                key={leg.tripId}
                type="button"
                className={`leg-tab ${activeLegIndex === idx ? 'active' : ''}`}
                onClick={() => setActiveLegIndex(idx)}
              >
                <span>Leg {idx + 1}: {leg.originStationName} &rarr; {leg.destinationStationName}</span>
                <span className="badge">
                  {(selectedSeatsByLeg[idx] || []).length} Seat(s) Selected
                </span>
              </button>
            ))}
          </div>
        )}

        <div className="seat-modal-body">
          {/* Seat Layout View */}
          <div className="seat-grid-container">
            <div className="bus-cockpit">
              <span>DRIVER CABIN</span>
              <span>🚪 DOOR</span>
            </div>

            {loadingSeatMap ? (
              <div className="loading-spinner">Loading interactive seat layout...</div>
            ) : (
              <div className="seat-layout">
                {Object.entries(seatsByRow).map(([rowNum, rowSeats]) => (
                  <div key={rowNum} className="seat-row">
                    <span className="row-label">R{rowNum}</span>
                    <div className="seat-row-left">
                      {rowSeats.slice(0, 2).map((seat) => renderSeatButton(seat))}
                    </div>
                    <div className="aisle"></div>
                    <div className="seat-row-right">
                      {rowSeats.slice(2).map((seat) => renderSeatButton(seat))}
                    </div>
                  </div>
                ))}
              </div>
            )}

            <div className="seat-legend">
              <span className="legend-item"><span className="legend-box available"></span> Available</span>
              <span className="legend-item"><span className="legend-box selected"></span> Selected</span>
              <span className="legend-item"><span className="legend-box occupied"></span> Occupied / Blocked</span>
            </div>
          </div>

          {/* Passenger Details & Booking Summary */}
          <form onSubmit={handleBook} className="passenger-form-panel">
            <h4>Passenger Information</h4>
            {passengerDetails.length === 0 ? (
              <p style={{ color: '#64748b', fontSize: '0.9rem' }}>
                👈 Click on available seats to add passengers.
              </p>
            ) : (
              passengerDetails.map((p, idx) => (
                <div key={idx} className="passenger-card">
                  <div className="passenger-card-header">
                    <strong>Passenger {idx + 1}</strong>
                    <span style={{ fontSize: '0.8rem', color: '#0369a1' }}>
                      Leg 1: {getSeatNumber(0, idx) || 'Pending'}
                      {isConnecting && ` | Leg 2: ${getSeatNumber(1, idx) || 'Pending'}`}
                    </span>
                  </div>
                  <div className="form-grid" style={{ gridTemplateColumns: '2fr 1fr 1fr', gap: '0.5rem' }}>
                    <div className="form-group">
                      <label>Full Name</label>
                      <input
                        type="text"
                        required
                        value={p.name}
                        onChange={(e) => updatePassenger(idx, 'name', e.target.value)}
                        placeholder="Name on Govt ID"
                      />
                    </div>
                    <div className="form-group">
                      <label>Age</label>
                      <input
                        type="number"
                        min="1"
                        max="120"
                        required
                        value={p.age}
                        onChange={(e) => updatePassenger(idx, 'age', e.target.value)}
                      />
                    </div>
                    <div className="form-group">
                      <label>Gender</label>
                      <select
                        value={p.gender}
                        onChange={(e) => updatePassenger(idx, 'gender', e.target.value)}
                      >
                        <option value="MALE">Male</option>
                        <option value="FEMALE">Female</option>
                        <option value="OTHER">Other</option>
                      </select>
                    </div>
                  </div>
                </div>
              ))
            )}

            <h4 style={{ marginTop: '1rem' }}>Contact Details for Ticket Receipt</h4>
            <div className="form-grid">
              <div className="form-group">
                <label>Email Address</label>
                <input
                  type="email"
                  required
                  value={contactEmail}
                  onChange={(e) => setContactEmail(e.target.value)}
                  placeholder="name@domain.com"
                />
              </div>
              <div className="form-group">
                <label>Phone Number</label>
                <input
                  type="text"
                  required
                  value={contactPhone}
                  onChange={(e) => setContactPhone(e.target.value)}
                  placeholder="+919876543210"
                />
              </div>
            </div>

            <div className="price-summary-box">
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.25rem' }}>
                <span>Selected Seats:</span>
                <strong>{passengerDetails.length}</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '1.1rem', color: '#0f172a' }}>
                <span>Total Amount:</span>
                <strong style={{ color: '#16a34a' }}>₹{totalCalculatedPrice.toFixed(2)}</strong>
              </div>
            </div>

            <button
              type="submit"
              className="btn btn-primary btn-block btn-lg"
              disabled={submitting || passengerDetails.length === 0}
            >
              {submitting ? 'Confirming Reservation...' : `Confirm & Book Tickets (₹${totalCalculatedPrice.toFixed(2)})`}
            </button>
          </form>
        </div>
      </div>
    </div>
  );

  function renderSeatButton(seat) {
    const isSelected = currentSelections.includes(seat.seatId);
    let btnClass = 'seat-btn';
    if (!seat.available) {
      btnClass += ' seat-occupied';
    } else if (isSelected) {
      btnClass += ' seat-selected';
    } else {
      btnClass += ' seat-available';
    }

    return (
      <button
        key={seat.seatId}
        type="button"
        className={btnClass}
        disabled={!seat.available}
        onClick={() => toggleSeatSelection(seat)}
        title={`Seat ${seat.seatNumber} (${seat.seatType}) - ${seat.available ? 'Available' : 'Occupied'}`}
      >
        <span className="seat-icon">💺</span>
        <span className="seat-num">{seat.seatNumber}</span>
      </button>
    );
  }

  function getSeatNumber(legIndex, passengerIdx) {
    const seatId = (selectedSeatsByLeg[legIndex] || [])[passengerIdx];
    if (!seatId) return null;
    const seat = (seatMaps[legIndex]?.seats || []).find((s) => s.seatId === seatId);
    return seat ? seat.seatNumber : null;
  }
}
