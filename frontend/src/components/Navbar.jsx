import React from 'react';

export default function Navbar({ activeTab, setActiveTab, currentUser, onOpenAuth, onLogout }) {
  const isAdmin = currentUser?.role === 'ROLE_ADMIN';

  return (
    <>
      <div className="promo-strip no-print">
        <span className="promo-strip-pill">Assured Seat Guarantee</span>
        <span>Instant Confirmed Intercity E-Tickets &bull; Smart 1-Transfer Connecting Routes &bull; Zero Hidden Convenience Fees</span>
      </div>
      <header className="header no-print">
        <div className="header-inner">
          <div className="header-brand" onClick={() => setActiveTab('search')} title="Bussin' Home">
            <div className="brand-emblem" aria-hidden="true">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.1" strokeLinecap="round" strokeLinejoin="round">
                <rect x="3" y="4" width="18" height="13" rx="3" />
                <path d="M3 11h18" />
                <path d="M7 17v2" />
                <path d="M17 17v2" />
                <circle cx="7.5" cy="14" r="1" fill="currentColor" />
                <circle cx="16.5" cy="14" r="1" fill="currentColor" />
              </svg>
            </div>
            <div>
              <h1 className="brand-title">Bussin'</h1>
              <p className="brand-subtitle">India's Premier Intercity Bus Network</p>
            </div>
          </div>

          {/* Navigation Links */}
          <nav className="nav-tabs" aria-label="Primary Navigation">
            <button
              className={`nav-tab-btn ${activeTab === 'search' ? 'active' : ''}`}
              onClick={() => setActiveTab('search')}
            >
              <span>🚌</span> Bus Tickets
            </button>

            {currentUser && (
              <button
                className={`nav-tab-btn ${activeTab === 'my-bookings' ? 'active' : ''}`}
                onClick={() => setActiveTab('my-bookings')}
              >
                <span>🎫</span> My Bookings
              </button>
            )}

            {isAdmin && (
              <>
                <button
                  className={`nav-tab-btn ${activeTab === 'admin-categories' ? 'active' : ''}`}
                  onClick={() => setActiveTab('admin-categories')}
                >
                  <span>🏷️</span> Categories
                </button>
                <button
                  className={`nav-tab-btn ${activeTab === 'admin-fleet' ? 'active' : ''}`}
                  onClick={() => setActiveTab('admin-fleet')}
                >
                  <span>🚍</span> Fleet
                </button>
                <button
                  className={`nav-tab-btn ${activeTab === 'admin-routes' ? 'active' : ''}`}
                  onClick={() => setActiveTab('admin-routes')}
                >
                  <span>🗺️</span> Routes
                </button>
                <button
                  className={`nav-tab-btn ${activeTab === 'admin-scheduler' ? 'active' : ''}`}
                  onClick={() => setActiveTab('admin-scheduler')}
                >
                  <span>⏱️</span> Trips
                </button>
                <button
                  className={`nav-tab-btn ${activeTab === 'admin-ledger' ? 'active' : ''}`}
                  onClick={() => setActiveTab('admin-ledger')}
                >
                  <span>📋</span> All Bookings
                </button>
              </>
            )}
          </nav>

          {/* User Session Bar */}
          <div className="header-user-section">
            {currentUser ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <div style={{ textAlign: 'right' }}>
                  <strong style={{ display: 'block', fontSize: '0.88rem', color: '#0f172a' }}>
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
              <button className="btn btn-primary btn-sm" onClick={onOpenAuth} style={{ borderRadius: '9999px', padding: '0.45rem 1.15rem' }}>
                👤 Account / Sign In
              </button>
            )}
          </div>
        </div>
      </header>
    </>
  );
}
