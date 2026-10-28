import React, { useState, useEffect } from 'react';
import { apiRequest } from '../../api/client';
import PrintableTicket from './PrintableTicket';

export default function BookingHistory({ currentUser, setAlert }) {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedTicket, setSelectedTicket] = useState(null);
  const [cancellingId, setCancellingId] = useState(null);

  useEffect(() => {
    loadBookings();
  }, [currentUser]);

  async function loadBookings() {
    setLoading(true);
    try {
      const data = await apiRequest('/api/v1/me/bookings');
      setBookings(data);
    } catch (err) {
      setAlert({
        type: 'error',
        title: 'Failed to Load Bookings',
        message: err.problem?.detail || err.message
      });
    } finally {
      setLoading(false);
    }
  }

  async function viewTicketDetails(bookingId) {
    try {
      const fullBooking = await apiRequest(`/api/v1/bookings/${bookingId}`);
      setSelectedTicket(fullBooking);
    } catch (err) {
      setAlert({
        type: 'error',
        title: 'Error Loading Ticket Details',
        message: err.problem?.detail || err.message
      });
    }
  }

  async function handleCancel(bookingId, reference) {
    if (!window.confirm(`Are you sure you want to cancel booking ${reference}? This will immediately release all booked seats.`)) {
      return;
    }

    setCancellingId(bookingId);
    try {
      const response = await apiRequest(`/api/v1/bookings/${bookingId}/cancel`, {
        method: 'POST'
      });
      setAlert({
        type: 'success',
        title: 'Booking Cancelled',
        message: `Booking ${reference} has been cancelled. Your seats have been released.`
      });
      // Refresh list
      await loadBookings();
    } catch (err) {
      setAlert({
        type: 'error',
        title: 'Cancellation Failed',
        status: err.status,
        message: err.problem?.detail || err.message
      });
    } finally {
      setCancellingId(null);
    }
  }

  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
        <div>
          <h2 style={{ margin: 0 }}>My Bookings & Travel History</h2>
          <p style={{ margin: '0.25rem 0 0', color: '#64748b', fontSize: '0.9rem' }}>
            View your confirmed itineraries, download printable tickets, or manage cancellations.
          </p>
        </div>
        <button className="btn btn-outline btn-sm" onClick={loadBookings} disabled={loading}>
          🔄 Refresh
        </button>
      </div>

      {loading ? (
        <div className="loading-spinner">Loading your booking history...</div>
      ) : bookings.length === 0 ? (
        <div className="empty-state">
          <span style={{ fontSize: '2.5rem' }}>🎫</span>
          <h3>No bookings found</h3>
          <p style={{ color: '#64748b' }}>You haven't made any bus reservations yet.</p>
        </div>
      ) : (
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Reference</th>
                <th>Route Summary</th>
                <th>Passengers / Legs</th>
                <th>Booked At</th>
                <th>Total Fare</th>
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {bookings.map((b) => (
                <tr key={b.id}>
                  <td>
                    <strong style={{ fontFamily: 'monospace', color: '#0369a1' }}>
                      {b.bookingReference}
                    </strong>
                  </td>
                  <td><strong>{b.routeSummary}</strong></td>
                  <td>{b.passengerCount} Pax ({b.legCount} Leg{b.legCount > 1 ? 's' : ''})</td>
                  <td style={{ fontSize: '0.85rem', color: '#475569' }}>
                    {new Date(b.bookedAt).toLocaleString()}
                  </td>
                  <td><strong>₹{b.totalAmountInr?.toFixed(2)}</strong></td>
                  <td>
                    <span className={`badge ${b.status === 'CONFIRMED' ? 'badge-success' : 'badge-danger'}`}>
                      {b.status}
                    </span>
                  </td>
                  <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                    <button
                      className="btn btn-xs btn-outline"
                      style={{ marginRight: '0.5rem' }}
                      onClick={() => viewTicketDetails(b.id)}
                    >
                      🖨️ View Ticket
                    </button>
                    {b.status === 'CONFIRMED' && (
                      <button
                        className="btn btn-xs btn-danger"
                        disabled={cancellingId === b.id}
                        onClick={() => handleCancel(b.id, b.bookingReference)}
                      >
                        {cancellingId === b.id ? 'Cancelling...' : 'Cancel'}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selectedTicket && (
        <PrintableTicket
          booking={selectedTicket}
          onClose={() => setSelectedTicket(null)}
        />
      )}
    </div>
  );
}
