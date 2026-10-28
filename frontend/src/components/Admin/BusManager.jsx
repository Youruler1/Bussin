import React, { useState, useEffect } from 'react';
import { apiRequest } from '../../api/client';

export default function BusManager({ setAlert }) {
  const [buses, setBuses] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);

  // Form states
  const [editingId, setEditingId] = useState(null);
  const [regNumber, setRegNumber] = useState('');
  const [brand, setBrand] = useState('Volvo');
  const [model, setModel] = useState('9600 Multi-Axle');
  const [categoryId, setCategoryId] = useState('');
  const [totalCapacity, setTotalCapacity] = useState(20);
  const [isActive, setIsActive] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    setLoading(true);
    try {
      const [busData, catData] = await Promise.all([
        apiRequest('/api/v1/admin/buses'),
        apiRequest('/api/v1/categories')
      ]);
      setBuses(busData);
      const activeCats = catData.filter((c) => c.isActive);
      setCategories(activeCats);
      if (!categoryId && activeCats.length > 0) {
        setCategoryId(String(activeCats[0].id));
      }
    } catch (err) {
      setAlert({
        type: 'error',
        title: 'Error Loading Bus Fleet',
        message: err.problem?.detail || err.message
      });
    } finally {
      setLoading(false);
    }
  }

  function startEdit(bus) {
    setEditingId(bus.id);
    setRegNumber(bus.registrationNumber || '');
    setBrand(bus.brand || '');
    setModel(bus.model || '');
    setCategoryId(String(bus.categoryId));
    setTotalCapacity(bus.totalCapacity || 20);
    setIsActive(Boolean(bus.isActive));
  }

  function resetForm() {
    setEditingId(null);
    setRegNumber('');
    setBrand('Volvo');
    setModel('9600 Multi-Axle');
    setTotalCapacity(20);
    setIsActive(true);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!categoryId) {
      setAlert({ type: 'error', title: 'Category Required', message: 'Please select a valid bus category.' });
      return;
    }

    setSubmitting(true);
    try {
      if (editingId) {
        await apiRequest(`/api/v1/admin/buses/${editingId}`, {
          method: 'PUT',
          body: {
            brand: brand.trim(),
            model: model.trim(),
            categoryId: parseInt(categoryId, 10),
            isActive
          }
        });
        setAlert({
          type: 'success',
          title: 'Bus Updated',
          message: `Bus "${regNumber}" details updated.`
        });
      } else {
        await apiRequest('/api/v1/admin/buses', {
          method: 'POST',
          body: {
            registrationNumber: regNumber.trim().toUpperCase(),
            brand: brand.trim(),
            model: model.trim(),
            categoryId: parseInt(categoryId, 10),
            totalCapacity: parseInt(totalCapacity, 10)
          }
        });
        setAlert({
          type: 'success',
          title: 'Bus Registered',
          message: `Bus "${regNumber}" with ${totalCapacity} auto-generated seats added to fleet.`
        });
      }
      resetForm();
      loadData();
    } catch (err) {
      setAlert({
        type: 'error',
        title: editingId ? 'Bus Update Failed' : 'Bus Registration Failed',
        status: err.status,
        message: err.problem?.detail || err.message
      });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(id, reg) {
    if (!window.confirm(`Delete or deactivate bus ${reg}?`)) return;

    try {
      await apiRequest(`/api/v1/admin/buses/${id}`, { method: 'DELETE' });
      setAlert({
        type: 'success',
        title: 'Bus Deleted',
        message: `Bus "${reg}" removed from fleet.`
      });
      loadData();
    } catch (err) {
      setAlert({
        type: 'error',
        title: 'Reference Conflict (Soft-Deactivated)',
        status: err.status,
        message: err.problem?.detail || err.message
      });
      loadData();
    }
  }

  return (
    <div className="admin-grid">
      {/* Create / Edit Bus Card */}
      <div className="card">
        <h3>{editingId ? 'Edit Fleet Vehicle' : 'Add Bus to Fleet'}</h3>
        <p style={{ color: '#64748b', fontSize: '0.85rem' }}>
          Registers vehicle and auto-generates sequential seat layouts.
        </p>
        <form onSubmit={handleSubmit} className="form-grid">
          <div className="form-group">
            <label>Registration Number</label>
            <input
              type="text"
              required
              disabled={Boolean(editingId)}
              value={regNumber}
              onChange={(e) => setRegNumber(e.target.value)}
              placeholder="e.g. DL-01-AB-1234"
            />
          </div>
          <div className="form-group">
            <label>Category</label>
            <select
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value)}
              required
            >
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.code})
                </option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label>Brand & Model</label>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <input
                type="text"
                required
                value={brand}
                onChange={(e) => setBrand(e.target.value)}
                placeholder="Brand (e.g. Volvo)"
              />
              <input
                type="text"
                required
                value={model}
                onChange={(e) => setModel(e.target.value)}
                placeholder="Model"
              />
            </div>
          </div>
          {!editingId ? (
            <div className="form-group">
              <label>Total Capacity (1 – 80 Seats)</label>
              <input
                type="number"
                min="1"
                max="80"
                required
                value={totalCapacity}
                onChange={(e) => setTotalCapacity(e.target.value)}
              />
            </div>
          ) : (
            <div className="form-group">
              <label>Operational Status</label>
              <select value={String(isActive)} onChange={(e) => setIsActive(e.target.value === 'true')}>
                <option value="true">ACTIVE</option>
                <option value="false">INACTIVE</option>
              </select>
            </div>
          )}
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
              {submitting ? 'Saving...' : (editingId ? 'Save Changes' : '+ Register Bus')}
            </button>
            {editingId && (
              <button type="button" className="btn btn-outline" onClick={resetForm}>
                Cancel
              </button>
            )}
          </div>
        </form>
      </div>

      {/* Buses List */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <h3 style={{ margin: 0 }}>Fleet Inventory</h3>
          <button className="btn btn-xs btn-outline" onClick={loadData}>Refresh</button>
        </div>

        {loading ? (
          <div className="loading-spinner">Loading fleet...</div>
        ) : buses.length === 0 ? (
          <p style={{ color: '#64748b' }}>No buses in fleet yet.</p>
        ) : (
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Registration</th>
                  <th>Brand / Model</th>
                  <th>Category</th>
                  <th>Capacity</th>
                  <th>Status</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {buses.map((b) => (
                  <tr key={b.id}>
                    <td><code>{b.registrationNumber}</code></td>
                    <td>{b.brand} {b.model}</td>
                    <td><span className="badge">{b.categoryName || 'General'}</span></td>
                    <td><strong>{b.totalCapacity} Seats</strong></td>
                    <td>
                      <span className={`badge ${b.isActive ? 'badge-success' : 'badge-danger'}`}>
                        {b.isActive ? 'ACTIVE' : 'INACTIVE'}
                      </span>
                    </td>
                    <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                      <button
                        className="btn btn-xs btn-outline"
                        style={{ marginRight: '0.4rem' }}
                        onClick={() => startEdit(b)}
                      >
                        Edit
                      </button>
                      <button
                        className="btn btn-xs btn-danger"
                        onClick={() => handleDelete(b.id, b.registrationNumber)}
                      >
                        Delete / Deactivate
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
