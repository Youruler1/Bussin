import React, { useState, useEffect } from 'react';
import { apiRequest } from '../../api/client';

export default function CategoryManager({ setAlert }) {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);

  // Form states
  const [editingId, setEditingId] = useState(null);
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    loadCategories();
  }, []);

  async function loadCategories() {
    setLoading(true);
    try {
      const data = await apiRequest('/api/v1/admin/categories');
      setCategories(data);
    } catch (err) {
      setAlert({
        type: 'error',
        title: 'Error Loading Categories',
        message: err.problem?.detail || err.message
      });
    } finally {
      setLoading(false);
    }
  }

  function startEdit(category) {
    setEditingId(category.id);
    setCode(category.code || '');
    setName(category.name || '');
    setDescription(category.description || '');
  }

  function resetForm() {
    setEditingId(null);
    setCode('');
    setName('');
    setDescription('');
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSubmitting(true);
    try {
      const payload = {
        code: code.trim().toUpperCase(),
        name: name.trim(),
        description: description.trim()
      };
      if (editingId) {
        await apiRequest(`/api/v1/admin/categories/${editingId}`, {
          method: 'PUT',
          body: payload
        });
        setAlert({
          type: 'success',
          title: 'Category Updated',
          message: `Category "${name}" updated successfully.`
        });
      } else {
        await apiRequest('/api/v1/admin/categories', {
          method: 'POST',
          body: payload
        });
        setAlert({
          type: 'success',
          title: 'Category Created',
          message: `Category "${name}" created successfully.`
        });
      }
      resetForm();
      loadCategories();
    } catch (err) {
      setAlert({
        type: 'error',
        title: editingId ? 'Update Category Failed' : 'Create Category Failed',
        status: err.status,
        message: err.problem?.detail || err.message
      });
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(id, categoryName) {
    if (!window.confirm(`Delete or deactivate category "${categoryName}"?`)) return;

    try {
      await apiRequest(`/api/v1/admin/categories/${id}`, { method: 'DELETE' });
      setAlert({
        type: 'success',
        title: 'Category Deleted',
        message: `Category "${categoryName}" was removed.`
      });
      loadCategories();
    } catch (err) {
      // Handles reference-safe 409 Conflict with soft-deactivation
      setAlert({
        type: 'error',
        title: 'Reference Conflict (Soft-Deactivated)',
        status: err.status,
        message: err.problem?.detail || err.message
      });
      loadCategories();
    }
  }

  return (
    <div className="admin-grid">
      {/* Create / Edit Category Card */}
      <div className="card">
        <h3>{editingId ? 'Edit Bus Category' : 'Create New Bus Category'}</h3>
        <p style={{ color: '#64748b', fontSize: '0.85rem' }}>
          Define bus classes (e.g., AC Deluxe, Sleeper, Volvo Multi-Axle).
        </p>
        <form onSubmit={handleSubmit} className="form-grid">
          <div className="form-group">
            <label>Category Code (Uppercase & Underscores)</label>
            <input
              type="text"
              required
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="e.g. AC_VOLVO"
            />
          </div>
          <div className="form-group">
            <label>Display Name</label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. AC Multi-Axle Volvo"
            />
          </div>
          <div className="form-group">
            <label>Description</label>
            <textarea
              rows="3"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="e.g. Luxury AC coach with recliner seats and USB charging"
            />
          </div>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
              {submitting ? 'Saving...' : (editingId ? 'Save Changes' : '+ Add Category')}
            </button>
            {editingId && (
              <button type="button" className="btn btn-outline" onClick={resetForm}>
                Cancel
              </button>
            )}
          </div>
        </form>
      </div>

      {/* Categories List */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <h3 style={{ margin: 0 }}>Existing Categories</h3>
          <button className="btn btn-xs btn-outline" onClick={loadCategories}>Refresh</button>
        </div>

        {loading ? (
          <div className="loading-spinner">Loading categories...</div>
        ) : categories.length === 0 ? (
          <p style={{ color: '#64748b' }}>No categories created yet.</p>
        ) : (
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Name</th>
                  <th>Status</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {categories.map((c) => (
                  <tr key={c.id}>
                    <td><code>{c.code}</code></td>
                    <td>
                      <strong>{c.name}</strong>
                      <div style={{ fontSize: '0.8rem', color: '#64748b' }}>{c.description}</div>
                    </td>
                    <td>
                      <span className={`badge ${c.isActive ? 'badge-success' : 'badge-danger'}`}>
                        {c.isActive ? 'ACTIVE' : 'INACTIVE'}
                      </span>
                    </td>
                    <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                      <button
                        className="btn btn-xs btn-outline"
                        style={{ marginRight: '0.4rem' }}
                        onClick={() => startEdit(c)}
                      >
                        Edit
                      </button>
                      <button
                        className="btn btn-xs btn-danger"
                        onClick={() => handleDelete(c.id, c.name)}
                        title="Reference-safe delete / deactivate"
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
