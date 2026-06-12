import { useEffect, useCallback, useRef, useState } from 'react';
import { ReactComponent as CloseIcon } from '../../assets/icons/Close.svg';
import { ReactComponent as StarIcon } from '../../assets/icons/star.svg';
import { formatCalendarDate } from '../../utils/dateUtils';
import { getAgentBookingDocuments } from '../../services/agentBookingService';
import './BookingModal.css';

const fmtLong = (iso) => {
  if (!iso) return '—';
  return formatCalendarDate(iso, { month: 'long', day: 'numeric', year: 'numeric' });
};

/* ── Reusable label → value row ── */
const DetailItem = ({ label, value, isLink }) => (
  <div className="booking-modal__detail-row">
    <span className="booking-modal__detail-label">{label}</span>
    {isLink ? (
      <button
        type="button"
        className="booking-modal__detail-link"
      >
        {value}
      </button>
    ) : (
      <span className="booking-modal__detail-value">{value}</span>
    )}
  </div>
);

const BookingModal = ({ booking, onClose, onConfirm }) => {
  const panelRef = useRef(null);
  const [docs, setDocs] = useState([]);

  useEffect(() => {
    if (!booking?.id) return;
    getAgentBookingDocuments(booking.id)
      .then((data) => {
        const grouped = [];
        if (data.guestDocuments) {
          data.guestDocuments.forEach((g) =>
            (g.documents || []).forEach((d) => grouped.push({ label: `Passport ${g.userName}:`, name: d.fileName }))
          );
        }
        if (data.payments) {
          data.payments.forEach((d) => grouped.push({ label: 'Payment:', name: d.fileName }));
        }
        setDocs(grouped);
      })
      .catch(() => setDocs([]));
  }, [booking?.id]);

  /* ── Close on Escape ── */
  const handleKeyDown = useCallback(
    (e) => {
      if (e.key === 'Escape') onClose();

      /* ── Focus trap ── */
      if (e.key === 'Tab' && panelRef.current) {
        const focusable = panelRef.current.querySelectorAll(
          'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
        );
        if (focusable.length === 0) return;
        const first = focusable[0];
        const last = focusable[focusable.length - 1];

        if (e.shiftKey) {
          if (document.activeElement === first) {
            e.preventDefault();
            last.focus();
          }
        } else {
          if (document.activeElement === last) {
            e.preventDefault();
            first.focus();
          }
        }
      }
    },
    [onClose],
  );

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    document.body.style.overflow = 'hidden';

    // Focus the panel on mount
    if (panelRef.current) panelRef.current.focus();

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [handleKeyDown]);

  if (!booking) return null;

  const {
    hotelName,
    location,
    rating,
    startDate,
    durationDays,
    mealPlan,
    totalPrice,
    customer,
  } = booking;


  return (
    /* ── Overlay ── */
    <div
      className="booking-modal__overlay"
      onClick={onClose}
    >
      {/* ── Modal panel ── */}
      <div
        ref={panelRef}
        className="booking-modal__panel"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="booking-modal-title"
        tabIndex={-1}
      >
        {/* ── Top: name + rating + close ── */}
        <div className="booking-modal__header">
          <div className="booking-modal__header-top">
            <div className="booking-modal__title-group">
              <h2 id="booking-modal-title" className="booking-modal__title">
                {hotelName}
              </h2>
              <div className="booking-modal__rating">
                <StarIcon className="booking-modal__rating-icon" />
                <span className="booking-modal__rating-text">
                  {rating?.toFixed(1) ?? '—'}
                </span>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="booking-modal__close-btn"
              aria-label="Close modal"
            >
              <CloseIcon className="booking-modal__close-icon" />
            </button>
          </div>
          <span className="booking-modal__location">{location}</span>
        </div>

        {/* ── Container: details + docs + price + button ── */}
        <div className="booking-modal__content">
          {/* ── Booking details ── */}
          <div className="booking-modal__section">
            <h3 className="booking-modal__section-title">Booking details</h3>
            <div className="booking-modal__details-grid">
              <DetailItem label="Customer:" value={`${customer.name} (${customer.adults ?? 1} adult${(customer.adults ?? 1) !== 1 ? 's' : ''}${customer.children > 0 ? `, ${customer.children} child${customer.children !== 1 ? 'ren' : ''}` : ''})`} />
              <DetailItem label="Contact email:" value={customer.email} />
              <DetailItem label="Start date:" value={fmtLong(startDate)} />
              <DetailItem label="Duration:" value={`${durationDays} days`} />
              <DetailItem label="Meal plan:" value={mealPlan} />
            </div>
          </div>

          {/* ── Documents ── */}
          <div className="booking-modal__section">
            <h3 className="booking-modal__section-title">Documents</h3>
            {docs.length === 0 ? (
              <span className="booking-modal__detail-value" style={{ color: '#677883', fontSize: '14px' }}>No documents uploaded</span>
            ) : (
              <div className="booking-modal__details-grid">
                {docs.map((doc, i) => (
                  <DetailItem key={i} label={doc.label} value={doc.name} isLink />
                ))}
              </div>
            )}
          </div>

          {/* ── Total price ── */}
          <div className="booking-modal__total">
            <span className="booking-modal__total-label">Total price:</span>
            <span className="booking-modal__total-value">
              ${totalPrice?.toLocaleString()}
            </span>
          </div>

          {/* ── Confirm button ── */}
          <button
            type="button"
            onClick={() => {
              onConfirm?.(booking);
              onClose();
            }}
            className="booking-modal__confirm-btn"
          >
            Confirm
          </button>
        </div>
      </div>
    </div>
  );
};

export default BookingModal;
