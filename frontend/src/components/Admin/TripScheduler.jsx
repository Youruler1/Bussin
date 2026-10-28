import React, { useState, useEffect } from 'react';
import { apiRequest } from '../../api/client';

export default function TripScheduler({ setAlert }) {
  const [routes, setRoutes] = useState([]);
  const [buses, setBuses] = useState([]);
  const [trips, setTrips] = useState([]);
  const [loading, setLoading] = useState(true);

  // Form states
  const [tripNumber, setTripNumber] = useState('');
  const [selectedRouteId, setSelectedRouteId] = useState('');
  const [selectedBusId, setSelectedBusId] = useState('');
  const [serviceDate, setServiceDate] = useState('2026-11-01');
  const [departureTime, setDepartureTime] = useState('06:00');
  const [dwellMinutes, setDwellMinutes] = useState(10);
  const [baseFarePerKm, setBaseFarePerKm] = useState(2.0);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    setLoading(true);
    try {
      const [rtData, busData, tripData] = await Promise.all([
        apiRequest('/api/v1/routes'),
        apiRequest('/api/v1/buses'),
        apiRequest('/api/v1/admin/trips')
      ]);
      setRoutes(rtData);
      setBuses(busData.filter((b) => b.isActive));
      setTrips(tripData);
      if (!selectedRouteId && rtData.length > 0) setSelectedRouteId(String(rtData[0].id));
      if (!selectedBusId && busData.length > 0) setSelectedBusId(String(busData[0].id));
      if (!tripNumber) setTripNumber('TRIP-' + Math.floor(1000 + Math.random() * 9000));
    } catch (err) {
      setAlert({
        type: 'error',
        title: 'Error Loading Scheduling Data',
        message: err.problem?.detail || err.message
      });
    } finally {
      setLoading(false);
    }
  }

  const selectedRoute = routes.find((r) => String(r.id) === String(selectedRouteId));

  async function handleSchedule(e) {
    e.preventDefault();
    if (!selectedRoute || !selectedBusId) {
      setAlert({ type: 'error', title: 'Route and Bus Required', message: 'Please select both a route and a bus.' });
      return;
    }

    setSubmitting(true);
    try {
      // Calculate ISO 8601 UTC departure instant (interpreting local input in IST +05:30 or UTC)
      const depIso = new Date(`${serviceDate}T${departureTime}:00+05:30`).toISOString();

      const payload = {
        tripNumber: tripNumber.trim().toUpperCase(),
        routeId: parseInt(selectedRouteId, 10),
        busId: parseInt(selectedBusId, 10),
        serviceDate,
        scheduledDepartureUtc: depIso,
        dwellMinutes: parseInt(dwellMinutes, 10),
        baseFarePerKm: parseFloat(baseFarePerKm)
      };

      const created = await apiRequest('/api/v1/admin/trips', {
        method: 'POST',
        body: payload
      });

      setAlert({
        type: 'success',
        title: 'Trip Scheduled',
        message: `Trip ${created.tripNumber} scheduled on ${selectedRoute.name} with ${created.fares?.length || 0} segment fare snapshots and ${created.totalSeatsAllocatedMutex || 0} seat mutexes.`
      });

      setTripNumber('TRIP-' + Math.floor(1000 + Math.random() * 9000));
      loadData();
    } catch (err) {
      setAlert({
        type: 'error',
        title: 'Trip Scheduling Failed',
        status: err.status,
        message: err.problem?.detail || err.message
      });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteTrip(id, tripNum) {
    if (!window.confirm(`Delete or cancel trip ${tripNum}?`)) return;
    try {
      await apiRequest(`/api/v1/admin/trips/${id}`, { method: 'DELETE' });
      setAlert({ type: 'success', title: 'Trip Deleted', message: `Trip ${tripNum} removed.` });
      loadData();
    } catch (err) {
      setAlert({
        type: 'error',
        title: err.status === 409 ? 'Reference Conflict (Trip Cancelled)' : 'Delete Trip Failed',
        status: err.status,
        message: err.problem?.detail || err.message
      });
      loadData();
    }
  }

  return (
    <div className="admin-grid" style={{ gridTemplateColumns: '1fr 1fr' }}>
      {/* Trip Scheduler Form */}
      <div className="card">
        <h3>Schedule Trip on Route</h3>
        <p style={{ color: '#64748b', fontSize: '0.85rem' }}>
          Assign bus, freeze departure/arrival stop times, and snapshot segment fares.
        </p>

        <form onSubmit={handleSchedule} className="form-grid">
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
            <div className="form-group">
              <label>Trip Number</label>
              <input
                type="text"
                required
                value={tripNumber}
                onChange={(e) => setTripNumber(e.target.value)}
              />
            </div>
            <div className="form-group">
              <label>Service Date</label>
              <input
                type="date"
                required
                value={serviceDate}
                onChange={(e) => setServiceDate(e.target.value)}
              />
            </div>
          </div>

          <div className="form-group">
            <label>Assigned Route</label>
            <select
              value={selectedRouteId}
              onChange={(e) => setSelectedRouteId(e.target.value)}
              required
            >
              {routes.map((r) => (
                <option key={r.id} value={r.id}>{r.name} ({r.routeCode})</option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label>Assigned Bus Fleet Vehicle</label>
            <select
              value={selectedBusId}
              onChange={(e) => setSelectedBusId(e.target.value)}
              required
            >
              {buses.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.registrationNumber} - {b.brand} {b.model} ({b.totalCapacity} seats)
                </option>
              ))}
            </select>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.5rem' }}>
            <div className="form-group">
              <label>Departure (IST)</label>
              <input
                type="time"
                required
                value={departureTime}
                onChange={(e) => setDepartureTime(e.target.value)}
              />
            </div>
            <div className="form-group">
              <label>Stop Dwell (mins)</label>
              <input
                type="number"
                min="0"
                max="120"
                required
                value={dwellMinutes}
                onChange={(e) => setDwellMinutes(e.target.value)}
              />
            </div>
            <div className="form-group">
              <label>Rate (₹/km)</label>
              <input
                type="number"
                step="0.1"
                min="0.5"
                required
                value={baseFarePerKm}
                onChange={(e) => setBaseFarePerKm(e.target.value)}
              />
            </div>
          </div>

          <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? 'Scheduling Trip...' : '📅 Schedule Trip & Prepopulate Mutex Seats'}
          </button>
        </form>
      </div>

      {/* Scheduled Trips View */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <h3 style={{ margin: 0 }}>Active Scheduled Trips</h3>
          <button className="btn btn-xs btn-outline" onClick={loadData}>Refresh</button>
        </div>

        {loading ? (
          <div className="loading-spinner">Loading scheduled trips...</div>
        ) : trips.length === 0 ? (
          <p style={{ color: '#64748b' }}>No trips scheduled yet.</p>
        ) : (
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Trip #</th>
                  <th>Route / Bus</th>
                  <th>Date & Time</th>
                  <th>Status</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {trips.map((t) => (
                  <tr key={t.id}>
                    <td><code>{t.tripNumber}</code></td>
                    <td>
                      <strong>{t.routeName}</strong>
                      <div style={{ fontSize: '0.8rem', color: '#64748b' }}>{t.busRegistration} ({t.totalCapacity} seats)</div>
                    </td>
                    <td>
                      <div>{t.serviceDate}</div>
                      <div style={{ fontSize: '0.8rem', color: '#0369a1' }}>
                        {new Date(t.scheduledDepartureUtc).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} &rarr;{' '}
                        {new Date(t.scheduledArrivalUtc).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </div>
                    </td>
                    <td>
                      <span className="badge badge-success">{t.status}</span>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <button
                        className="btn btn-xs btn-danger"
                        onClick={() => handleDeleteTrip(t.id, t.tripNumber)}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
