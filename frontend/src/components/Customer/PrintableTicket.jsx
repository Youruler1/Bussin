import React from 'react';

export default function PrintableTicket({ booking, onClose }) {
  if (!booking) return null;

  function handlePrint() {
    window.print();
  }

  const isCancelled = booking.status === 'CANCELLED';

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card printable-ticket-modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header no-print">
          <h3>E-Ticket & Boarding Pass</h3>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button className="btn btn-outline" onClick={handlePrint}>🖨️ Print Ticket</button>
            <button className="close-btn" onClick={onClose}>&times;</button>
          </div>
        </div>

        {/* Printable Ticket Area */}
        <div className="ticket-container" id="printable-area">
          <div className="ticket-header">
            <div>
              <h2 style={{ margin: 0, color: '#1e3a8a', letterSpacing: '0.5px' }}>BUSSIN' EXPRESS</h2>
              <span style={{ fontSize: '0.85rem', color: '#64748b' }}>Bussin' Intercity Travel Pass &bull; Ministry of Road Transport Certified</span>
            </div>
            <div style={{ textAlign: 'right' }}>
              <div className={`ticket-status-pill ${isCancelled ? 'status-cancelled' : 'status-confirmed'}`}>
                {booking.status}
              </div>
              <div style={{ marginTop: '0.25rem', fontFamily: 'monospace', fontWeight: 700, fontSize: '1.05rem' }}>
                {booking.bookingReference}
              </div>
            </div>
          </div>

          <div className="ticket-meta-grid">
            <div>
              <span className="meta-label">CONTACT EMAIL</span>
              <span className="meta-val">{booking.contactEmail}</span>
            </div>
            <div>
              <span className="meta-label">CONTACT PHONE</span>
              <span className="meta-val">{booking.contactPhone}</span>
            </div>
            <div>
              <span className="meta-label">BOOKING TIME</span>
              <span className="meta-val">{new Date(booking.bookedAt).toLocaleString()}</span>
            </div>
            {booking.cancelledAt && (
              <div>
                <span className="meta-label">CANCELLATION TIME</span>
                <span className="meta-val" style={{ color: '#dc2626' }}>
                  {new Date(booking.cancelledAt).toLocaleString()}
                </span>
              </div>
            )}
          </div>

          {/* Itinerary Legs */}
          <div className="ticket-legs">
            {booking.legs?.map((leg, idx) => (
              <div key={idx} className="ticket-leg-box">
                <div className="ticket-leg-header">
                  <strong>Leg {idx + 1}: {leg.tripNumber}</strong>
                  <span style={{ color: '#475569' }}>
                    {leg.boardStationName} &rarr; {leg.alightStationName}
                  </span>
                </div>
                <div className="ticket-leg-times">
                  <div>
                    <span className="meta-label">DEPARTURE</span>
                    <strong style={{ fontSize: '1.05rem', color: '#0f172a' }}>
                      {new Date(leg.departureTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </strong>
                    <div style={{ fontSize: '0.75rem', color: '#64748b' }}>
                      {new Date(leg.departureTime).toLocaleDateString()}
                    </div>
                  </div>
                  <div style={{ textAlign: 'center', color: '#94a3b8' }}>
                    &mdash;&mdash;&gt;
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <span className="meta-label">ARRIVAL</span>
                    <strong style={{ fontSize: '1.05rem', color: '#0f172a' }}>
                      {new Date(leg.arrivalTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </strong>
                    <div style={{ fontSize: '0.75rem', color: '#64748b' }}>
                      {new Date(leg.arrivalTime).toLocaleDateString()}
                    </div>
                  </div>
                </div>

                <div className="passenger-table-container">
                  <table className="ticket-table">
                    <thead>
                      <tr>
                        <th>Seat</th>
                        <th>Passenger Name</th>
                        <th>Age / Gender</th>
                        <th style={{ textAlign: 'right' }}>Fare (INR)</th>
                      </tr>
                    </thead>
                    <tbody>
                      {leg.allocatedSeats?.map((alloc, aIdx) => (
                        <tr key={aIdx}>
                          <td><span className="badge badge-seat">{alloc.seatNumber}</span></td>
                          <td><strong>{alloc.passengerName}</strong></td>
                          <td>{alloc.passengerAge} yrs / {alloc.passengerGender || 'N/A'}</td>
                          <td style={{ textAlign: 'right' }}>₹{alloc.fareInr?.toFixed(2)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            ))}
          </div>

          <div className="ticket-footer">
            <div style={{ fontSize: '0.8rem', color: '#64748b', maxWidth: '65%' }}>
              * Please present a valid Government Photo ID (Aadhaar / Driving License / Voter ID) alongside this E-Ticket upon boarding.
              Standard Indian Intercity Transit Rules & Cancellation Policy apply. Helpline: 1800-BUSSIN (Toll-Free).
            </div>
            <div style={{ textAlign: 'right' }}>
              <span className="meta-label">TOTAL AMOUNT PAID</span>
              <div style={{ fontSize: '1.4rem', fontWeight: 800, color: '#16a34a' }}>
                ₹{booking.totalAmountInr?.toFixed(2)}
              </div>
            </div>
          </div>
        </div>

        <div className="modal-footer no-print">
          <button className="btn btn-secondary" onClick={onClose}>Close</button>
          <button className="btn btn-primary" onClick={handlePrint}>Print E-Ticket</button>
        </div>
      </div>
    </div>
  );
}
