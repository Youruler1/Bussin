import React from 'react';

export default function AlertModal({ alert, onClose }) {
  if (!alert) return null;

  const isError = alert.type === 'error' || alert.status >= 400;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card alert-modal" onClick={(e) => e.stopPropagation()}>
        <div className={`alert-header ${isError ? 'alert-danger' : 'alert-success'}`}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span style={{ fontSize: '1.4rem' }}>{isError ? '⚠️' : '✅'}</span>
            <h3 style={{ margin: 0 }}>{alert.title || (isError ? 'Action Failed' : 'Success')}</h3>
          </div>
          <button className="close-btn" onClick={onClose}>&times;</button>
        </div>
        <div className="alert-body">
          <p style={{ margin: '0 0 0.75rem', fontSize: '0.95rem', color: '#1e293b' }}>
            {alert.message || alert.detail || 'An unexpected condition occurred.'}
          </p>
          {alert.status && (
            <div className="alert-meta">
              <span><strong>HTTP Status:</strong> {alert.status}</span>
              {alert.typeUri && <span><strong>Problem Type:</strong> {alert.typeUri}</span>}
            </div>
          )}
        </div>
        <div className="alert-footer">
          <button className="btn btn-primary" onClick={onClose}>Dismiss</button>
        </div>
      </div>
    </div>
  );
}
