import React from 'react';

export default function Navbar({ activeTab, setActiveTab, currentUser, onOpenAuth, onLogout }) {
  const isAdmin = currentUser?.role === 'ROLE_ADMIN';

  return (
    <header className="header">
      <div className="header-brand">
        <span className="brand-logo">🚌</span>
        <div>
          <h1 className="brand-title">Bussin'</h1>
          <p className="brand-subtitle">India's Premier Intercity Bus Network</p>
        </div>
      </div>

      {/* Navigation Links */}
      <nav className="nav-tabs">
        <button
          className={`nav-tab-btn ${activeTab === 'search' ? 'active' : ''}`}
          onClick={() => setActiveTab('search')}
        >
          🔍 Search & Book
        </button>

        {currentUser && (
          <button
            className={`nav-tab-btn ${activeTab === 'my-bookings' ? 'active' : ''}`}
            onClick={() => setActiveTab('my-bookings')}
          >
            🎫 My Bookings
          </button>
        )}

        {isAdmin && (
          <>
            <button
              className={`nav-tab-btn ${activeTab === 'admin-categories' ? 'active' : ''}`}
              onClick={() => setActiveTab('admin-categories')}
            >
              🏷️ Categories
            </button>
            <button
              className={`nav-tab-btn ${activeTab === 'admin-fleet' ? 'active' : ''}`}
              onClick={() => setActiveTab('admin-fleet')}
            >
              🚌 Fleet
            </button>
            <button
              className={`nav-tab-btn ${activeTab === 'admin-routes' ? 'active' : ''}`}
              onClick={() => setActiveTab('admin-routes')}
            >
              🗺️ Routes
            </button>
            <button
              className={`nav-tab-btn ${activeTab === 'admin-scheduler' ? 'active' : ''}`}
              onClick={() => setActiveTab('admin-scheduler')}
            >
              ⏱️ Trips
            </button>
            <button
              className={`nav-tab-btn ${activeTab === 'admin-ledger' ? 'active' : ''}`}
              onClick={() => setActiveTab('admin-ledger')}
            >
              📋 All Bookings
            </button>
          </>
        )}
      </nav>

      {/* User Session Bar */}
      <div className="header-user-section">
        {currentUser ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div style={{ textAlign: 'right' }}>
              <strong style={{ display: 'block', fontSize: '0.9rem', color: '#0f172a' }}>
                {currentUser.fullName}
              </strong>
              <span className={`badge ${isAdmin ? 'badge-admin' : 'badge-customer'}`}>
                {isAdmin ? 'ADMINISTRATOR' : 'CUSTOMER'}
              </span>
            </div>
            <button className="btn btn-outline btn-sm" onClick={onLogout}>
              Sign Out
            </button>
          </div>
        ) : (
          <button className="btn btn-primary btn-sm" onClick={onOpenAuth}>
            Sign In / Register
          </button>
        )}
      </div>
    </header>
  );
}
