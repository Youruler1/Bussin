import React, { useState, useEffect } from 'react';
import { apiRequest } from '../../api/client';
import PrintableTicket from '../Customer/PrintableTicket';

export default function AdminBookings({ setAlert }) {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedTicket, setSelectedTicket] = useState(null);
  const [cancellingId, setCancellingId] = useState(null);

  useEffect(() => {
    loadAllBookings();
  }, []);

  async function loadAllBookings() {
    setLoading(true);
    try {
      const data = await apiRequest('/api/v1/admin/bookings');
      setBookings(data);
    } catch (err) {
      setAlert({
        type: 'error',
        title: 'Error Loading Admin Bookings',
        status: err.status,
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

  async function handleAdminCancel(bookingId, reference) {
    if (!window.confirm(`Admin override: Cancel booking ${reference}? This will immediately release all reserved seats.`)) {
      return;
    }

    setCancellingId(bookingId);
    try {
      await apiRequest(`/api/v1/bookings/${bookingId}/cancel`, { method: 'POST' });
      setAlert({
        type: 'success',
        title: 'Booking Cancelled (Admin Override)',
        message: `Booking ${reference} has been cancelled.`
      });
      loadAllBookings();
    } catch (err) {
      setAlert({
        type: 'error',
        title: 'Admin Cancellation Failed',
        status: err.status,
        message: err.problem?.detail || err.message
      });
    } finally {
      setCancellingId(null);
    }
  }

  const filtered = bookings.filter((b) =>
    b.bookingReference.toLowerCase().includes(searchTerm.toLowerCase()) ||
    b.routeSummary.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
        <div>
          <h2 style={{ margin: 0 }}>System-Wide Booking Ledger (Admin)</h2>
          <p style={{ margin: '0.25rem 0 0', color: '#64748b', fontSize: '0.9rem' }}>
            Auditable overview of all customer itineraries across all routes.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <input
            type="text"
            placeholder="Search by Reference..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{ width: '220px' }}
          />
          <button className="btn btn-outline btn-sm" onClick={loadAllBookings}>🔄 Refresh</button>
        </div>
      </div>

      {loading ? (
        <div className="loading-spinner">Loading system bookings...</div>
      ) : filtered.length === 0 ? (
        <p style={{ color: '#64748b' }}>No bookings matching criteria.</p>
      ) : (
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Booking Ref</th>
                <th>Route</th>
                <th>Passengers</th>
                <th>Booked At</th>
                <th>Fare</th>
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((b) => (
                <tr key={b.id}>
                  <td><code>{b.bookingReference}</code></td>
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
                      View Ticket
                    </button>
                    {b.status === 'CONFIRMED' && (
                      <button
                        className="btn btn-xs btn-danger"
                        disabled={cancellingId === b.id}
                        onClick={() => handleAdminCancel(b.id, b.bookingReference)}
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
