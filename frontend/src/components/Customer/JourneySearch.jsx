import React, { useState, useEffect } from 'react';
import { apiRequest } from '../../api/client';
import SeatMapModal from './SeatMapModal';
import PrintableTicket from './PrintableTicket';

export default function JourneySearch({ currentUser, onBookingSuccess, setAlert }) {
  const [stations, setStations] = useState([]);
  const [originStationId, setOriginStationId] = useState('');
  const [destinationStationId, setDestinationStationId] = useState('');
  const [travelDate, setTravelDate] = useState('2026-11-01');
  const [departureFrom, setDepartureFrom] = useState('');
  const [departureTo, setDepartureTo] = useState('');
  const [loadingStations, setLoadingStations] = useState(true);
  const [searching, setSearching] = useState(false);
  const [searchResult, setSearchResult] = useState(null);

  // Selected journey for seat map modal
  const [selectedJourney, setSelectedJourney] = useState(null);
  const [confirmedBooking, setConfirmedBooking] = useState(null);

  useEffect(() => {
    async function loadStations() {
      try {
        const data = await apiRequest('/api/v1/stations');
        setStations(data);
        if (data.length >= 2) {
          setOriginStationId(String(data[0].id));
          setDestinationStationId(String(data[data.length - 1].id));
        }
      } catch (err) {
        setAlert({
          type: 'error',
          title: 'Station Load Error',
          message: err.problem?.detail || err.message
        });
      } finally {
        setLoadingStations(false);
      }
    }
    loadStations();
  }, []);

  async function handleSearch(e) {
    if (e) e.preventDefault();
    if (!originStationId || !destinationStationId) {
      setAlert({ type: 'error', title: 'Invalid Selection', message: 'Please select both origin and destination stations.' });
      return;
    }
    if (originStationId === destinationStationId) {
      setAlert({ type: 'error', title: 'Invalid Route', message: 'Origin and destination stations must be different.' });
      return;
    }

    setSearching(true);
    try {
      let query = `/api/v1/journeys/search?originStationId=${originStationId}&destinationStationId=${destinationStationId}&travelDate=${travelDate}`;
      if (departureFrom) query += `&departureFrom=${departureFrom}:00`;
      if (departureTo) query += `&departureTo=${departureTo}:00`;

      const result = await apiRequest(query);
      setSearchResult(result);
    } catch (err) {
      setAlert({
        type: 'error',
        title: 'Search Failed',
        status: err.status,
        message: err.problem?.detail || err.message
      });
    } finally {
      setSearching(false);
    }
  }

  function handleBookingDone(booking) {
    setConfirmedBooking(booking);
    if (onBookingSuccess) onBookingSuccess(booking);
    // Refresh search results to show updated seat availability
    handleSearch();
  }

  function formatDuration(minutes) {
    const hrs = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return `${hrs}h ${mins > 0 ? mins + 'm' : ''}`;
  }

  return (
    <div>
      {/* Hero Banner Section */}
      <section className="hero-section">
        <div className="hero-inner">
          <div>
            <div className="hero-eyebrow">
              <span>🇮🇳</span> India's Trusted Intercity Transit Network
            </div>
            <h2 className="hero-title">
              India's No. 1 Online Bus Ticket Booking Site
            </h2>
            <p className="hero-subtitle">
              Compare AC Sleeper, Multi-Axle Volvo, and Pushback coaches across major Indian corridors with real-time seat layouts and automated connecting itineraries.
            </p>
            <div className="hero-trust-pills">
              <span className="hero-trust-pill">⚡ Live Seat Lock</span>
              <span className="hero-trust-pill">🔄 Smart 1-Transfer Fallback</span>
              <span className="hero-trust-pill">🛡️ Instant E-Ticket &amp; Free Cancellation</span>
            </div>
          </div>

          {/* Original Travel Coach & Highway Artwork */}
          <div className="hero-artwork" aria-hidden="true">
            <svg className="hero-svg-illustration" viewBox="0 0 380 210" fill="none" xmlns="http://www.w3.org/2000/svg">
              {/* Sunset Glow & Distant Hills */}
              <circle cx="290" cy="62" r="42" fill="#FDE047" fillOpacity="0.28" />
              <circle cx="290" cy="62" r="26" fill="#FEF08A" fillOpacity="0.45" />
              <path d="M10 165 Q95 105 185 155 T370 148 L370 185 L10 185 Z" fill="#450A0A" fillOpacity="0.35" />
              <path d="M40 172 Q155 120 260 162 T370 158 L370 185 L40 185 Z" fill="#1E293B" fillOpacity="0.45" />

              {/* Highway Road */}
              <rect x="10" y="168" width="360" height="22" rx="11" fill="#0F172A" fillOpacity="0.7" />
              <line x1="35" y1="179" x2="75" y2="179" stroke="#FACC15" strokeWidth="3" strokeDasharray="10 8" strokeLinecap="round" />
              <line x1="125" y1="179" x2="185" y2="179" stroke="#FACC15" strokeWidth="3" strokeDasharray="10 8" strokeLinecap="round" />
              <line x1="235" y1="179" x2="335" y2="179" stroke="#FACC15" strokeWidth="3" strokeDasharray="10 8" strokeLinecap="round" />

              {/* Modern Intercity Luxury Coach */}
              <g transform="translate(55, 78)">
                {/* Coach Body */}
                <rect x="0" y="10" width="255" height="74" rx="14" fill="#FFFFFF" />
                <path d="M0 54 H255 V70 C255 77.732 248.732 84 241 84 H14 C6.268 84 0 77.732 0 70 V54 Z" fill="#F1F5F9" />
                {/* Crimson Speed Stripe */}
                <path d="M8 54 H248 L238 64 H8 Z" fill="#D84E55" />
                <path d="M30 67 H195 L188 72 H30 Z" fill="#F59E0B" />

                {/* Panoramic Windows */}
                <rect x="14" y="20" width="42" height="26" rx="5" fill="#1E293B" />
                <rect x="62" y="20" width="42" height="26" rx="5" fill="#1E293B" />
                <rect x="110" y="20" width="42" height="26" rx="5" fill="#1E293B" />
                <rect x="158" y="20" width="42" height="26" rx="5" fill="#1E293B" />
                <path d="M206 20 H238 C243 20 247 24 248 29 L250 46 H206 V20 Z" fill="#0F172A" />

                {/* Headlight Beam */}
                <polygon points="255,64 255,76 315,86 315,52" fill="#FEF08A" fillOpacity="0.35" />

                {/* Wheels */}
                <circle cx="52" cy="84" r="15" fill="#0F172A" />
                <circle cx="52" cy="84" r="7" fill="#CBD5E1" />
                <circle cx="88" cy="84" r="15" fill="#0F172A" />
                <circle cx="88" cy="84" r="7" fill="#CBD5E1" />
                <circle cx="204" cy="84" r="15" fill="#0F172A" />
                <circle cx="204" cy="84" r="7" fill="#CBD5E1" />
              </g>
            </svg>
          </div>
        </div>
      </section>

      {/* Floating Booking Search Widget */}
      <div className="search-widget-wrapper">
        <div className="card search-card">
          <form onSubmit={handleSearch} className="search-form">
            <div className="search-segment">
              <label className="search-segment-label">
                <span>🚏</span> From Station
              </label>
              <select
                value={originStationId}
                onChange={(e) => setOriginStationId(e.target.value)}
                disabled={loadingStations}
              >
                {stations.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} ({s.code}) - {s.city}
                  </option>
                ))}
              </select>
            </div>

            <div className="search-segment">
              <label className="search-segment-label">
                <span>📍</span> To Station
              </label>
              <select
                value={destinationStationId}
                onChange={(e) => setDestinationStationId(e.target.value)}
                disabled={loadingStations}
              >
                {stations.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} ({s.code}) - {s.city}
                  </option>
                ))}
              </select>
            </div>

            <div className="search-segment">
              <label className="search-segment-label">
                <span>📅</span> Date of Journey
              </label>
              <input
                type="date"
                required
                value={travelDate}
                onChange={(e) => setTravelDate(e.target.value)}
              />
            </div>

            <div className="search-segment">
              <label className="search-segment-label">
                <span>🕒</span> Time Window (Optional)
              </label>
              <div className="search-time-range">
                <input
                  type="time"
                  value={departureFrom}
                  onChange={(e) => setDepartureFrom(e.target.value)}
                  title="Earliest Departure"
                />
                <span style={{ color: '#94a3b8', fontSize: '0.78rem', fontWeight: 600 }}>to</span>
                <input
                  type="time"
                  value={departureTo}
                  onChange={(e) => setDepartureTo(e.target.value)}
                  title="Latest Departure"
                />
              </div>
            </div>

            <div className="search-btn-col">
              <button type="submit" className="search-cta-btn" disabled={searching}>
                {searching ? 'Searching...' : 'SEARCH BUSES'}
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Value Highlights Strip */}
      {!searchResult && !searching && (
        <div className="feature-highlights">
          <div className="feature-highlight-card">
            <div className="feature-icon-box">💺</div>
            <div>
              <strong style={{ display: 'block', fontSize: '0.92rem' }}>Segment-Level Seat Resale</strong>
              <span style={{ fontSize: '0.8rem', color: '#64748b' }}>Book intermediate stops with real-time interval availability.</span>
            </div>
          </div>
          <div className="feature-highlight-card">
            <div className="feature-icon-box">🔄</div>
            <div>
              <strong style={{ display: 'block', fontSize: '0.92rem' }}>Automated Connecting Routes</strong>
              <span style={{ fontSize: '0.8rem', color: '#64748b' }}>Smart 1-transfer fallback when no direct bus operates.</span>
            </div>
          </div>
          <div className="feature-highlight-card">
            <div className="feature-icon-box">🔒</div>
            <div>
              <strong style={{ display: 'block', fontSize: '0.92rem' }}>Atomic Multi-Leg Locking</strong>
              <span style={{ fontSize: '0.8rem', color: '#64748b' }}>Deadlock-free seat reservation with instant printable passes.</span>
            </div>
          </div>
        </div>
      )}

      {/* Search Results */}
      {searching && (
        <div className="loading-spinner">Querying real-time routes and available seats...</div>
      )}

      {searchResult && !searching && (
        <div className="results-container">
          <div className="results-header">
            <h3>
              {searchResult.journeyType === 'DIRECT' && '🎯 Direct Journeys Available'}
              {searchResult.journeyType === 'CONNECTING' && '🔄 Connecting Journeys (1 Transfer Fallback)'}
              {searchResult.journeyType === 'NONE' && '❌ No Trips Found'}
            </h3>
            <span className="badge">
              {searchResult.options?.length || 0} Option(s) Found
            </span>
          </div>

          {searchResult.options?.length === 0 ? (
            <div className="card empty-state">
              <span style={{ fontSize: '2.5rem' }}>🚌</span>
              <h3>No schedules available for this route & date</h3>
              <p style={{ color: '#64748b' }}>
                Try selecting a different date or clearing time window filters.
              </p>
            </div>
          ) : (
            <div className="journey-list">
              {searchResult.options.map((option, idx) => (
                <div key={idx} className="card journey-card">
                  <div className="journey-card-main">
                    <div className="journey-summary">
                      <div className="journey-type-badge">
                        {option.itineraryType === 'DIRECT' ? (
                          <span className="badge badge-direct">DIRECT TRIP</span>
                        ) : (
                          <span className="badge badge-connecting">
                            CONNECTING ({option.legs.length} LEGS)
                          </span>
                        )}
                        <span style={{ fontSize: '0.85rem', color: '#64748b', marginLeft: '0.5rem' }}>
                          Total Duration: <strong>{formatDuration(option.totalDurationMinutes)}</strong>
                        </span>
                      </div>

                      {/* Legs Display */}
                      <div className="legs-display">
                        {option.legs.map((leg, lIdx) => (
                          <div key={lIdx} className="leg-display-row">
                            <div className="leg-route">
                              <strong>{leg.originStationName}</strong>
                              <span className="arrow">&rarr;</span>
                              <strong>{leg.destinationStationName}</strong>
                              <span className="leg-bus-badge">{leg.busCategory} ({leg.tripNumber})</span>
                            </div>
                            <div className="leg-timing">
                              <span>Dep: {new Date(leg.departureTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                              <span style={{ margin: '0 0.5rem' }}>|</span>
                              <span>Arr: {new Date(leg.arrivalTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                            </div>
                            <div className="leg-seats">
                              <span className={`seat-count-tag ${leg.availableSeats > 5 ? 'seats-green' : 'seats-orange'}`}>
                                {leg.availableSeats} Seats Left
                              </span>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>

                    <div className="journey-fare-action">
                      <div className="journey-price">
                        <span style={{ fontSize: '0.8rem', color: '#64748b' }}>Starting from</span>
                        <div className="price-amount">₹{option.totalFareInr.toFixed(2)}</div>
                        <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>per passenger</span>
                      </div>
                      <button
                        className="btn btn-primary btn-block"
                        onClick={() => setSelectedJourney(option)}
                      >
                        Select Seats &rarr;
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Interactive Seat Map Modal */}
      {selectedJourney && (
        <SeatMapModal
          isOpen={!!selectedJourney}
          onClose={() => setSelectedJourney(null)}
          journey={selectedJourney}
          currentUser={currentUser}
          onBookingConfirmed={handleBookingDone}
          setAlert={setAlert}
        />
      )}

      {/* Confirmed Printable Ticket Modal */}
      {confirmedBooking && (
        <PrintableTicket
          booking={confirmedBooking}
          onClose={() => setConfirmedBooking(null)}
        />
      )}
    </div>
  );
}
