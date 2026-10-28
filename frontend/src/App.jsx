import React, { useState, useEffect } from 'react';
import { apiRequest } from './api/client';
import Navbar from './components/Navbar';
import AuthModal from './components/AuthModal';
import AlertModal from './components/AlertModal';
import JourneySearch from './components/Customer/JourneySearch';
import BookingHistory from './components/Customer/BookingHistory';
import CategoryManager from './components/Admin/CategoryManager';
import BusManager from './components/Admin/BusManager';
import StationRouteManager from './components/Admin/StationRouteManager';
import TripScheduler from './components/Admin/TripScheduler';
import AdminBookings from './components/Admin/AdminBookings';

export default function App() {
  const [currentUser, setCurrentUser] = useState(null);
  const [activeTab, setActiveTab] = useState('search');
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [alert, setAlert] = useState(null);
  const [systemHealth, setSystemHealth] = useState(null);

  // Check auth session and backend health on initial load
  useEffect(() => {
    checkCurrentUser();
    checkHealth();
  }, []);

  async function checkCurrentUser() {
    try {
      const profile = await apiRequest('/api/v1/me');
      setCurrentUser(profile);
    } catch {
      // Unauthenticated session - expected for public visitors
      setCurrentUser(null);
    }
  }

  async function checkHealth() {
    try {
      const health = await apiRequest('/api/v1/health');
      setSystemHealth(health);
    } catch {
      setSystemHealth({ status: 'OFFLINE' });
    }
  }

  async function handleLogout() {
    try {
      await apiRequest('/api/v1/auth/logout', { method: 'POST' });
    } catch (err) {
      console.warn('Logout request ended with:', err);
    } finally {
      setCurrentUser(null);
      setActiveTab('search');
      setAlert({
        type: 'success',
        title: 'Signed Out',
        message: 'You have been safely signed out.'
      });
    }
  }

  function handleBookingSuccess() {
    // When a booking completes, automatically switch to My Bookings
    setActiveTab('my-bookings');
  }

  return (
    <div className="app-layout">
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        currentUser={currentUser}
        onOpenAuth={() => setAuthModalOpen(true)}
        onLogout={handleLogout}
      />

      <main className="main-content">
        {/* Quick Demo Test Credential Ribbon */}
        {!currentUser && (
          <div className="demo-ribbon no-print">
            <span>💡 <strong>Quick Evaluation Access:</strong> Instant 1-click sign-in with seeded accounts:</span>
            <div className="demo-ribbon-actions">
              <button
                className="btn btn-xs btn-outline"
                onClick={async () => {
                  try {
                    await apiRequest('/api/v1/auth/login', {
                      method: 'POST',
                      body: { email: 'customer@demo.local', password: 'Password@123' }
                    });
                    await checkCurrentUser();
                  } catch {
                    setAuthModalOpen(true);
                  }
                }}
              >
                👤 Sign In as Customer (customer@demo.local)
              </button>
              <button
                className="btn btn-xs btn-outline"
                onClick={async () => {
                  try {
                    await apiRequest('/api/v1/auth/login', {
                      method: 'POST',
                      body: { email: 'admin@busbooking.local', password: 'Password@123' }
                    });
                    await checkCurrentUser();
                  } catch {
                    setAuthModalOpen(true);
                  }
                }}
              >
                🛡️ Sign In as Admin (admin@busbooking.local)
              </button>
            </div>
          </div>
        )}

        {/* Tab Content */}
        {activeTab === 'search' && (
          <JourneySearch
            currentUser={currentUser}
            onBookingSuccess={handleBookingSuccess}
            setAlert={setAlert}
          />
        )}

        {activeTab === 'my-bookings' && (
          <BookingHistory
            currentUser={currentUser}
            setAlert={setAlert}
          />
        )}

        {activeTab === 'admin-categories' && (
          <CategoryManager setAlert={setAlert} />
        )}

        {activeTab === 'admin-fleet' && (
          <BusManager setAlert={setAlert} />
        )}

        {activeTab === 'admin-routes' && (
          <StationRouteManager setAlert={setAlert} />
        )}

        {activeTab === 'admin-scheduler' && (
          <TripScheduler setAlert={setAlert} />
        )}

        {activeTab === 'admin-ledger' && (
          <AdminBookings setAlert={setAlert} />
        )}
      </main>

      <footer className="footer no-print">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span>Bussin' &bull; India's Intercity Bus Booking Platform</span>
          <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
            <span style={{ fontSize: '0.8rem', color: '#64748b' }}>
              Backend Status: <strong style={{ color: '#16a34a' }}>{systemHealth?.status || 'HEALTHY'}</strong>
            </span>
            <span style={{ fontSize: '0.8rem', color: '#64748b' }}>
              Database: PostgreSQL / H2
            </span>
          </div>
        </div>
      </footer>

      {/* Modals */}
      <AuthModal
        isOpen={authModalOpen}
        onClose={() => setAuthModalOpen(false)}
        onAuthSuccess={(profile) => setCurrentUser(profile)}
        setAlert={setAlert}
      />

      <AlertModal
        alert={alert}
        onClose={() => setAlert(null)}
      />
    </div>
  );
}
