import { useEffect, useCallback, useRef, useState } from 'react';
import { ReactComponent as CloseIcon } from '../../assets/icons/Close.svg';
import CancelReasonSelect from './CancelReasonSelect';
import { formatCalendarDate } from '../../utils/dateUtils';
import './CancelBookingModal.css';

const fmtLong = (iso) => {
  if (!iso) return '—';
  return formatCalendarDate(iso, { month: 'long', day: 'numeric', year: 'numeric' });
};

/* ── Detail Row ── */
const DetailRow = ({ label, value }) => (
  <div className="cancel-modal__detail-row">
    <span className="cancel-modal__detail-label">{label}</span>
    <span className="cancel-modal__detail-value">{value}</span>
  </div>
);

/* ── Cancel Booking Modal ── */
const CancelBookingModal = ({ booking, onClose, onConfirmCancel }) => {
  const [cancelReason, setCancelReason] = useState('');
  const modalRef = useRef(null);

  const handleKeyDown = useCallback(
    (e) => {
      if (e.key === 'Escape') onClose();

      /* ── Focus trap ── */
      if (e.key === 'Tab' && modalRef.current) {
        const focusable = modalRef.current.querySelectorAll(
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

    // Focus trap - focus modal on open
    if (modalRef.current) modalRef.current.focus();

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = '';
    };
  }, [handleKeyDown]);

  if (!booking) return null;

  const {
    hotelName,
    location,
    startDate,
    durationDays,
    mealPlan,
    customer,
  } = booking;

  const handleCancelBooking = () => {
    onConfirmCancel?.({ ...booking, cancelReason });
    onClose();
  };

  return (
    <div
      className="cancel-modal__overlay"
      onClick={onClose}
    >
      <div
        ref={modalRef}
        className="cancel-modal__panel"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="cancel-modal-title"
        tabIndex={-1}
      >
        {/* Header */}
        <div className="cancel-modal__header">
          <h2 id="cancel-modal-title" className="cancel-modal__title">
            Cancel
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="cancel-modal__close-btn"
            aria-label="Close modal"
          >
            <CloseIcon className="cancel-modal__close-icon" />
          </button>
        </div>

        {/* Content */}
        <div className="cancel-modal__content">
          {/* Booking Details */}
          <div className="cancel-modal__section">
            <h3 className="cancel-modal__section-title">Booking details</h3>
            <div className="cancel-modal__details-grid">
              <DetailRow label="Customer:" value={`${customer.name} ( 1 adult)`} />
              <DetailRow label="Contact email:" value={customer.email} />
              <DetailRow label="Tour name:" value={hotelName} />
              <DetailRow label="Location:" value={location} />
              <DetailRow label="Start date:" value={fmtLong(startDate)} />
              <DetailRow label="Duration:" value={`${durationDays} days`} />
              <DetailRow label="Tour type:" value="Resort" />
              <DetailRow label="Meal plan:" value={mealPlan} />
            </div>
          </div>

          {/* Cancellation Reason */}
          <div className="cancel-modal__section">
            <label className="cancel-modal__field-label">Cancellation reason</label>
            <CancelReasonSelect value={cancelReason} onChange={setCancelReason} />
          </div>
        </div>

        {/* Actions */}
        <div className="cancel-modal__actions">
          <button
            type="button"
            onClick={onClose}
            className="cancel-modal__btn cancel-modal__btn--secondary"
          >
            Keep the booking
          </button>
          <button
            type="button"
            onClick={handleCancelBooking}
            className="cancel-modal__btn cancel-modal__btn--primary"
            disabled={!cancelReason}
          >
            Cancel the booking
          </button>
        </div>
      </div>
    </div>
  );
};

export default CancelBookingModal;
