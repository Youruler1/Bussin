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
      {/* Search Bar Card */}
      <div className="card search-card">
        <h2 style={{ margin: '0 0 1rem' }}>Book Bus Tickets Across India on Bussin'</h2>
        <form onSubmit={handleSearch} className="search-form">
          <div className="form-group">
            <label>Origin Station</label>
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

          <div className="form-group">
            <label>Destination Station</label>
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

          <div className="form-group">
            <label>Travel Date</label>
            <input
              type="date"
              required
              value={travelDate}
              onChange={(e) => setTravelDate(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label>Time Window (Optional)</label>
            <div style={{ display: 'flex', gap: '0.25rem' }}>
              <input
                type="time"
                value={departureFrom}
                onChange={(e) => setDepartureFrom(e.target.value)}
                title="Earliest Departure"
              />
              <span style={{ alignSelf: 'center', color: '#64748b' }}>to</span>
              <input
                type="time"
                value={departureTo}
                onChange={(e) => setDepartureTo(e.target.value)}
                title="Latest Departure"
              />
            </div>
          </div>

          <div className="form-group search-btn-col">
            <label>&nbsp;</label>
            <button type="submit" className="btn btn-primary btn-block" disabled={searching}>
              {searching ? 'Searching...' : '🔍 Search Trips'}
            </button>
          </div>
        </form>
      </div>

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
