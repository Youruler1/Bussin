import React, { useState, useEffect } from 'react';
import { apiRequest } from '../../api/client';

export default function StationRouteManager({ setAlert }) {
  const [stations, setStations] = useState([]);
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(true);

  // Station Form
  const [stnCode, setStnCode] = useState('');
  const [stnName, setStnName] = useState('');
  const [stnCity, setStnCity] = useState('');
  const [stnState, setStnState] = useState('');
  const [stnTimezone, setStnTimezone] = useState('Asia/Kolkata');

  // Route Form
  const [rtCode, setRtCode] = useState('');
  const [rtName, setRtName] = useState('');
  const [rtOriginId, setRtOriginId] = useState('');
  const [rtDestId, setRtDestId] = useState('');
  const [rtDistance, setRtDistance] = useState(480);
  // Route Stops: array of { stationId, stopOrder, distanceFromPrevKm, typicalDurationMinutes }
  const [routeStops, setRouteStops] = useState([]);

  useEffect(() => {
    loadAll();
  }, []);

  async function loadAll() {
    setLoading(true);
    try {
      const [stnData, rtData] = await Promise.all([
        apiRequest('/api/v1/admin/stations'),
        apiRequest('/api/v1/admin/routes')
      ]);
      setStations(stnData);
      setRoutes(rtData);
      const activeStations = stnData.filter((s) => s.isActive);
      if (activeStations.length >= 2 && routeStops.length === 0) {
        setRtOriginId(String(activeStations[0].id));
        setRtDestId(String(activeStations[activeStations.length - 1].id));
        initRouteStops(activeStations[0].id, activeStations[activeStations.length - 1].id);
      }
    } catch (err) {
      setAlert({
        type: 'error',
        title: 'Error Loading Stations & Routes',
        message: err.problem?.detail || err.message
      });
    } finally {
      setLoading(false);
    }
  }

  function initRouteStops(origId, destId) {
    setRouteStops([
      { stationId: origId, stopOrder: 0, distanceFromPrevKm: 0, typicalDurationMinutes: 0 },
      { stationId: destId, stopOrder: 1, distanceFromPrevKm: 240, typicalDurationMinutes: 240 }
    ]);
  }

  function addIntermediateStop() {
    const nextOrder = routeStops.length;
    const availableStation = stations.find((s) => !routeStops.some((rs) => String(rs.stationId) === String(s.id))) || stations[0];
    setRouteStops([
      ...routeStops.slice(0, -1),
      { stationId: availableStation?.id, stopOrder: nextOrder - 1, distanceFromPrevKm: 150, typicalDurationMinutes: 180 },
      { ...routeStops[routeStops.length - 1], stopOrder: nextOrder }
    ]);
  }

  function removeStop(index) {
    if (routeStops.length <= 2) return;
    const updated = routeStops.filter((_, idx) => idx !== index).map((rs, idx) => ({ ...rs, stopOrder: idx }));
    setRouteStops(updated);
  }

  function updateStop(index, field, value) {
    setRouteStops((prev) => {
      const copy = [...prev];
      copy[index] = { ...copy[index], [field]: value };
      return copy;
    });
  }

  async function handleCreateStation(e) {
    e.preventDefault();
    try {
      await apiRequest('/api/v1/admin/stations', {
        method: 'POST',
        body: {
          code: stnCode.trim().toUpperCase(),
          name: stnName.trim(),
          city: stnCity.trim(),
          state: stnState.trim(),
          timezone: stnTimezone.trim() || 'Asia/Kolkata'
        }
      });
      setAlert({ type: 'success', title: 'Station Created', message: `Station ${stnName} (${stnCode}) registered.` });
      setStnCode('');
      setStnName('');
      setStnCity('');
      setStnState('');
      loadAll();
    } catch (err) {
      setAlert({ type: 'error', title: 'Create Station Failed', status: err.status, message: err.problem?.detail || err.message });
    }
  }

  async function handleDeleteStation(id, name) {
    if (!window.confirm(`Delete or deactivate station "${name}"?`)) return;
    try {
      await apiRequest(`/api/v1/admin/stations/${id}`, { method: 'DELETE' });
      setAlert({ type: 'success', title: 'Station Deleted', message: `Station "${name}" removed.` });
      loadAll();
    } catch (err) {
      setAlert({ type: 'error', title: 'Reference Conflict (Soft-Deactivated)', status: err.status, message: err.problem?.detail || err.message });
      loadAll();
    }
  }

  async function handleCreateRoute(e) {
    e.preventDefault();
    try {
      const stopsPayload = routeStops.map((rs, idx) => ({
        stationId: parseInt(rs.stationId, 10),
        stopOrder: idx,
        distanceFromPrevKm: idx === 0 ? 0 : parseFloat(rs.distanceFromPrevKm || 0),
        typicalDurationMinutes: idx === 0 ? 0 : parseInt(rs.typicalDurationMinutes || 0, 10)
      }));

      await apiRequest('/api/v1/admin/routes', {
        method: 'POST',
        body: {
          routeCode: rtCode.trim().toUpperCase(),
          name: rtName.trim(),
          originStationId: parseInt(routeStops[0].stationId, 10),
          destinationStationId: parseInt(routeStops[routeStops.length - 1].stationId, 10),
          totalDistanceKm: parseFloat(rtDistance),
          stops: stopsPayload
        }
      });
      setAlert({ type: 'success', title: 'Route Created', message: `Route "${rtName}" created with ${routeStops.length} ordered stops.` });
      setRtCode('');
      setRtName('');
      loadAll();
    } catch (err) {
      setAlert({ type: 'error', title: 'Route Creation Failed', status: err.status, message: err.problem?.detail || err.message });
    }
  }

  async function handleDeleteRoute(id, name) {
    if (!window.confirm(`Delete or deactivate route "${name}"?`)) return;
    try {
      await apiRequest(`/api/v1/admin/routes/${id}`, { method: 'DELETE' });
      setAlert({ type: 'success', title: 'Route Deleted', message: `Route "${name}" removed.` });
      loadAll();
    } catch (err) {
      setAlert({ type: 'error', title: 'Reference Conflict (Soft-Deactivated)', status: err.status, message: err.problem?.detail || err.message });
      loadAll();
    }
  }

  return (
    <div className="admin-grid" style={{ gridTemplateColumns: '1fr 1fr' }}>
      {/* Station Management */}
      <div className="card">
        <h3>Station Management</h3>
        <p style={{ color: '#64748b', fontSize: '0.85rem' }}>
          Stations with IATA-style code and IANA timezone.
        </p>
        <form onSubmit={handleCreateStation} className="form-grid" style={{ marginBottom: '1.5rem' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '0.5rem' }}>
            <div className="form-group">
              <label>Station Code</label>
              <input
                type="text"
                required
                value={stnCode}
                onChange={(e) => setStnCode(e.target.value)}
                placeholder="e.g. NDLS"
              />
            </div>
            <div className="form-group">
              <label>Station Name</label>
              <input
                type="text"
                required
                value={stnName}
                onChange={(e) => setStnName(e.target.value)}
                placeholder="e.g. Kashmere Gate ISBT"
              />
            </div>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
            <div className="form-group">
              <label>City</label>
              <input
                type="text"
                required
                value={stnCity}
                onChange={(e) => setStnCity(e.target.value)}
                placeholder="Delhi"
              />
            </div>
            <div className="form-group">
              <label>State</label>
              <input
                type="text"
                required
                value={stnState}
                onChange={(e) => setStnState(e.target.value)}
                placeholder="Delhi"
              />
            </div>
          </div>
          <button type="submit" className="btn btn-primary btn-block">+ Add Station</button>
        </form>

        <h4>Registered Stations ({stations.length})</h4>
        <div className="table-responsive" style={{ maxHeight: '250px', overflowY: 'auto' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Name</th>
                <th>City / State</th>
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {stations.map((s) => (
                <tr key={s.id}>
                  <td><code>{s.code}</code></td>
                  <td><strong>{s.name}</strong></td>
                  <td>{s.city}, {s.state}</td>
                  <td>
                    <span className={`badge ${s.isActive ? 'badge-success' : 'badge-danger'}`}>
                      {s.isActive ? 'ACTIVE' : 'INACTIVE'}
                    </span>
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <button
                      className="btn btn-xs btn-danger"
                      onClick={() => handleDeleteStation(s.id, s.name)}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Route Management */}
      <div className="card">
        <h3>Route Builder & Stops</h3>
        <p style={{ color: '#64748b', fontSize: '0.85rem' }}>
          Sequential stops (0..n-1), non-repeat stations, segment distance and duration from previous stop.
        </p>
        <form onSubmit={handleCreateRoute} className="form-grid">
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '0.5rem' }}>
            <div className="form-group">
              <label>Route Code</label>
              <input
                type="text"
                required
                value={rtCode}
                onChange={(e) => setRtCode(e.target.value)}
                placeholder="e.g. RT-DEL-LKO"
              />
            </div>
            <div className="form-group">
              <label>Route Name</label>
              <input
                type="text"
                required
                value={rtName}
                onChange={(e) => setRtName(e.target.value)}
                placeholder="e.g. Delhi - Agra - Lucknow Express"
              />
            </div>
          </div>

          <div className="form-group">
            <label>Total Distance (km)</label>
            <input
              type="number"
              required
              value={rtDistance}
              onChange={(e) => setRtDistance(e.target.value)}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <label style={{ fontWeight: 600 }}>Ordered Route Stops ({routeStops.length})</label>
            <button type="button" className="btn btn-xs btn-outline" onClick={addIntermediateStop}>
              + Add Intermediate Stop
            </button>
          </div>

          <div className="route-stops-list">
            {routeStops.map((rs, idx) => (
              <div key={idx} className="route-stop-row">
                <span className="badge badge-seat">Stop {idx}</span>
                <select
                  value={rs.stationId}
                  onChange={(e) => updateStop(idx, 'stationId', e.target.value)}
                  style={{ flex: 1 }}
                >
                  {stations.map((st) => (
                    <option key={st.id} value={st.id}>{st.name} ({st.city})</option>
                  ))}
                </select>
                <input
                  type="number"
                  placeholder="Km"
                  disabled={idx === 0}
                  style={{ width: '70px' }}
                  value={idx === 0 ? 0 : rs.distanceFromPrevKm}
                  onChange={(e) => updateStop(idx, 'distanceFromPrevKm', e.target.value)}
                  title="Distance from previous stop in km"
                />
                <input
                  type="number"
                  placeholder="Mins"
                  disabled={idx === 0}
                  style={{ width: '70px' }}
                  value={idx === 0 ? 0 : rs.typicalDurationMinutes}
                  onChange={(e) => updateStop(idx, 'typicalDurationMinutes', e.target.value)}
                  title="Typical transit minutes from previous stop"
                />
                {idx > 0 && idx < routeStops.length - 1 && (
                  <button type="button" className="btn btn-xs btn-danger" onClick={() => removeStop(idx)}>
                    &times;
                  </button>
                )}
              </div>
            ))}
          </div>

          <button type="submit" className="btn btn-primary btn-block">+ Save Route</button>
        </form>

        <h4 style={{ marginTop: '1.5rem' }}>Existing Routes ({routes.length})</h4>
        <div className="table-responsive" style={{ maxHeight: '200px', overflowY: 'auto' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Name</th>
                <th>Distance</th>
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {routes.map((r) => (
                <tr key={r.id}>
                  <td><code>{r.routeCode}</code></td>
                  <td><strong>{r.name}</strong></td>
                  <td>{r.totalDistanceKm} km</td>
                  <td>
                    <span className={`badge ${r.isActive ? 'badge-success' : 'badge-danger'}`}>
                      {r.isActive ? 'ACTIVE' : 'INACTIVE'}
                    </span>
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <button
                      className="btn btn-xs btn-danger"
                      onClick={() => handleDeleteRoute(r.id, r.name)}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
