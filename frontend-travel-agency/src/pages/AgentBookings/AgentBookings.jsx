import { useCallback, useEffect, useMemo, useState } from 'react';
import FilterTabs from '../../components/AgentBookings/FilterTabs';
import BookingCard from '../../components/AgentBookings/BookingCard';
import BookingModal from '../../components/AgentBookings/BookingModal';
import CancelBookingModal from '../../components/AgentBookings/CancelBookingModal';
import { getAgentBookings, cancelAgentBooking, confirmAgentBooking } from '../../services/agentBookingService';
import './AgentBookings.css';

const BOOKINGS_PER_PAGE = 6;

const AgentBookings = () => {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [activeTab, setActiveTab] = useState('all');
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedBooking, setSelectedBooking] = useState(null);
  const [cancelBooking, setCancelBooking] = useState(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getAgentBookings().then((data) => {
      if (!cancelled) {
        setBookings(data);
        setLoading(false);
      }
    }).catch(() => {
      if (!cancelled) {
        setError(true);
        setLoading(false);
      }
    });
    return () => { cancelled = true; };
  }, []);

  const filtered = useMemo(() => {
    if (activeTab === 'all') return bookings;
    return bookings.filter((b) => b.status === activeTab);
  }, [bookings, activeTab]);

  const totalPages = useMemo(() => Math.max(1, Math.ceil(filtered.length / BOOKINGS_PER_PAGE)), [filtered.length]);

  const paginated = useMemo(() => {
    const start = (currentPage - 1) * BOOKINGS_PER_PAGE;
    return filtered.slice(start, start + BOOKINGS_PER_PAGE);
  }, [filtered, currentPage]);

  useEffect(() => {
    if (currentPage > totalPages) setCurrentPage(totalPages);
  }, [currentPage, totalPages]);

  const handleTabChange = useCallback((key) => {
    setActiveTab(key);
    setCurrentPage(1);
  }, []);

  const handleCheckAndConfirm = useCallback((booking) => {
    setSelectedBooking(booking);
  }, []);

  const handleCloseModal = useCallback(() => {
    setSelectedBooking(null);
  }, []);

  const handleConfirmBooking = useCallback(async (booking) => {
    try {
      await confirmAgentBooking(booking.id);
      setBookings((prev) =>
        prev.map((b) => (b.id === booking.id ? { ...b, status: 'confirmed' } : b))
      );
    } catch (err) {
      console.error('Confirm booking failed:', err?.response?.data || err.message);
      alert(err?.response?.data?.message || 'Failed to confirm booking.');
    }
  }, []);

  const handleCancelClick = useCallback((booking) => {
    setCancelBooking(booking);
  }, []);

  const handleCloseCancelModal = useCallback(() => {
    setCancelBooking(null);
  }, []);

  const handleConfirmCancel = useCallback(async (bookingWithReason) => {
    try {
      await cancelAgentBooking(bookingWithReason.id, bookingWithReason.cancelReason);
    } catch (err) {
      console.error('Cancel booking failed:', err?.response?.data || err.message);
    }
    setBookings((prev) =>
      prev.map((b) =>
        b.id === bookingWithReason.id
          ? { ...b, status: 'canceled', cancelReason: bookingWithReason.cancelReason, canceledAfterStep: b.status === 'confirmed' ? 1 : 0 }
          : b
      )
    );
    setCancelBooking(null);
  }, []);

  return (
    <div className="min-h-screen bg-blue-03">
      <div className="bookings-container">
        {/* Filter tabs */}
        <FilterTabs activeTab={activeTab} onChange={handleTabChange} />

        {/* Cards grid */}
        <div className="bookings-grid">
          {loading
            ? Array.from({ length: 4 }).map((_, i) => (
                <div key={i} className="bookings-card-skeleton" aria-hidden="true" />
              ))
            : error
              ? (
                <div className="bookings-empty-state" role="alert">
                  <p className="bookings-empty-title">Something went wrong</p>
                  <p className="bookings-empty-subtitle">
                    Unable to load bookings. Please try again later.
                  </p>
                </div>
              )
              : filtered.length === 0
              ? (
                <div className="bookings-empty-state">
                  <p className="bookings-empty-title">
                    No {activeTab !== 'all' ? activeTab : ''} bookings found
                  </p>
                  <p className="bookings-empty-subtitle">
                    Bookings with &ldquo;{activeTab}&rdquo; status will appear here.
                  </p>
                </div>
              )
              : paginated.map((booking) => (
                  <BookingCard
                    key={booking.id}
                    booking={booking}
                    onCheckAndConfirm={handleCheckAndConfirm}
                    onCancel={handleCancelClick}
                  />
                ))}
        </div>

        {/* Pagination */}
        {!loading && totalPages > 1 && (
          <nav className="bookings-pagination" aria-label="Bookings pagination">
            {Array.from({ length: totalPages }, (_, i) => i + 1).map((n) => (
              <button
                key={n}
                type="button"
                onClick={() => setCurrentPage(n)}
                className={`bookings-pagination__btn ${n === currentPage ? 'bookings-pagination__btn--active' : ''}`}
                aria-current={n === currentPage ? 'page' : undefined}
                aria-label={`Go to page ${n}`}
              >
                {n}
              </button>
            ))}
          </nav>
        )}
      </div>

      {/* Booking detail modal */}
      {selectedBooking && (
        <BookingModal
          booking={selectedBooking}
          onClose={handleCloseModal}
          onConfirm={handleConfirmBooking}
        />
      )}

      {/* Cancel booking modal */}
      {cancelBooking && (
        <CancelBookingModal
          booking={cancelBooking}
          onClose={handleCloseCancelModal}
          onConfirmCancel={handleConfirmCancel}
        />
      )}
    </div>
  );
};

export default AgentBookings;
