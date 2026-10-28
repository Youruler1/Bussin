import React, { useState } from 'react';
import { apiRequest } from '../api/client';

export default function AuthModal({ isOpen, onClose, onAuthSuccess, setAlert }) {
  const [isRegister, setIsRegister] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [loading, setLoading] = useState(false);

  if (!isOpen) return null;

  async function handleSubmit(e) {
    e.preventDefault();
    setLoading(true);
    try {
      if (isRegister) {
        await apiRequest('/api/v1/auth/register', {
          method: 'POST',
          body: { email, password, fullName, phone }
        });
        setAlert({
          type: 'success',
          title: 'Registration Successful',
          message: 'Account created! Logging you in...'
        });
      }
      // Log in
      await apiRequest('/api/v1/auth/login', {
        method: 'POST',
        body: { email, password }
      });
      // Fetch current profile
      const profile = await apiRequest('/api/v1/me');
      onAuthSuccess(profile);
      onClose();
    } catch (err) {
      setAlert({
        type: 'error',
        title: isRegister ? 'Registration Failed' : 'Login Failed',
        status: err.status,
        message: err.problem?.detail || err.message
      });
    } finally {
      setLoading(false);
    }
  }

  function fillPreset(presetEmail, presetPass) {
    setEmail(presetEmail);
    setPassword(presetPass);
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>{isRegister ? 'Create Customer Account' : 'Sign In to Your Account'}</h3>
          <button className="close-btn" onClick={onClose}>&times;</button>
        </div>
        <form onSubmit={handleSubmit} className="modal-body form-grid">
          {/* Quick preset pills for convenience */}
          <div className="preset-bar">
            <span style={{ fontSize: '0.8rem', color: '#64748b' }}>Quick Test Login:</span>
            <button
              type="button"
              className="btn btn-xs btn-outline"
              onClick={() => fillPreset('customer@demo.local', 'Password@123')}
            >
              👤 Customer (demo)
            </button>
            <button
              type="button"
              className="btn btn-xs btn-outline"
              onClick={() => fillPreset('admin@busbooking.local', 'Password@123')}
            >
              🛡️ Admin
            </button>
          </div>

          {isRegister && (
            <>
              <div className="form-group">
                <label>Full Name</label>
                <input
                  type="text"
                  required
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="e.g. Parijaat Gaur"
                />
              </div>
              <div className="form-group">
                <label>Phone Number</label>
                <input
                  type="text"
                  required
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="+919000000001"
                />
              </div>
            </>
          )}

          <div className="form-group">
            <label>Email Address</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="user@example.com"
            />
          </div>

          <div className="form-group">
            <label>Password</label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
            />
          </div>

          <div style={{ marginTop: '1rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
              {loading ? 'Processing...' : (isRegister ? 'Register & Sign In' : 'Sign In')}
            </button>
            <button
              type="button"
              className="btn btn-link"
              onClick={() => setIsRegister(!isRegister)}
            >
              {isRegister ? 'Already have an account? Sign In' : "Don't have an account? Register here"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
