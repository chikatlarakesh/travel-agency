import { useEffect } from 'react';
import { formatCalendarDate } from '../../utils/dateUtils';

const fmt = (iso) => {
  if (!iso) return '—';
  return formatCalendarDate(iso, { month: 'long', day: 'numeric', year: 'numeric' });
};

const CancelModal = ({ booking, onClose, onConfirm }) => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const freeCancelDate = booking.freeCancellationDate
    ? new Date(booking.freeCancellationDate)
    : null;
  
  if (freeCancelDate) {
    freeCancelDate.setHours(0, 0, 0, 0);
  }

  const isFreeCancel = freeCancelDate && freeCancelDate >= today;

  /* Close on Escape */
  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [onClose]);

  return (
    /* ── Overlay ── */
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      style={{ background: '#42424280' }}
      onClick={onClose}
    >
      {/* ── Modal box ── */}
      <div
        className="relative flex flex-col bg-white"
        style={{
          width: '544px',
          maxWidth: 'calc(100vw - 24px)',
          height: isFreeCancel ? '308px' : '332px',
          gap: '32px',
          borderRadius: '12px',
          padding: '24px',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div
          style={{
            width: '100%',
            maxWidth: '496px',
            height: '40px',
          }}
        >
          <div
            className="flex items-center justify-between"
            style={{
              width: '100%',
              maxWidth: '496px',
              height: '40px',
            }}
          >
            <div
              className="flex"
              style={{
                width: '75px',
                height: '40px',
                gap: '18px',
              }}
            >
              <h2
                className="font-nunito text-[#0B3857]"
                style={{
                  width: '75px',
                  height: '40px',
                  fontSize: '24px',
                  fontWeight: 700,
                  lineHeight: '40px',
                  letterSpacing: '0px',
                }}
              >
                Cancel
              </h2>
            </div>

            <button
              type="button"
              onClick={onClose}
              className="flex items-center justify-center text-[#677883] transition hover:text-[#0B3857]"
              style={{
                width: '24px',
                height: '24px',
              }}
              aria-label="Close"
            >
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path
                  d="M18 6L6 18M6 6L18 18"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                />
              </svg>
            </button>
          </div>
        </div>

        {/* Main content */}
        <div
          className="flex flex-col"
          style={{
            width: '100%',
            maxWidth: '496px',
            height: '116px',
            gap: '12px',
          }}
        >
          {isFreeCancel ? (
            <div
              className="font-nunito"
              style={{
                width: '100%',
                maxWidth: '496px',
                height: '56px',
                gap: '8px',
                borderRadius: '8px',
                padding: '16px',
                background: '#FFFAD3',
                color: '#0B3857',
                fontSize: '14px',
                fontWeight: 400,
                lineHeight: '24px',
                letterSpacing: '0%',
              }}
            >
              <span
                style={{
                  width: '100%',
                  maxWidth: '464px',
                  height: '24px',
                  display: 'block',
                }}
              >
                Free cancellation is possible until {fmt(booking.freeCancellationDate)}.
              </span>
            </div>
          ) : (
            <div
              className="font-nunito"
              style={{
                width: '100%',
                maxWidth: '496px',
                height: '80px',
                gap: '8px',
                borderRadius: '8px',
                padding: '16px',
                background: '#FCE9ED',
                border: '1px solid #F5C2C7',
                color: '#B70B0B',
                fontSize: '14px',
                fontWeight: 400,
                lineHeight: '24px',
                letterSpacing: '0%',
                display: 'flex',
                flexDirection: 'column',
              }}
            >
              <span
                style={{
                  width: '100%',
                  maxWidth: '464px',
                  minHeight: '48px',
                  display: 'block',
                }}
              >
                Please note, that the free cancelation period for this booking is over, the charges are non-refundable.
              </span>
            </div>
          )}

          <p
            className="font-nunito"
            style={{
              width: '100%',
              maxWidth: '496px',
              height: '48px',
              color: '#0B3857',
              fontSize: '14px',
              fontWeight: 400,
              lineHeight: '24px',
              letterSpacing: '0%',
              margin: 0,
            }}
          >
            Are you sure you want to cancel the tour at{' '}
            <span style={{ fontWeight: 700 }}>{booking.name}</span>, starting date{' '}
            <span style={{ fontWeight: 700 }}>
              {booking.startDate
                ? `${fmt(booking.startDate)}${booking.duration ? ` (${booking.duration} days)` : ''}`
                : '—'}
            </span>
            , <span style={{ fontWeight: 700 }}>{booking.mealPlan}</span> for{' '}
            <span style={{ fontWeight: 700 }}>
              {booking.travelerCount} adult{booking.travelerCount !== 1 ? 's' : ''}
            </span>
            ?
          </p>
        </div>

        {/* Buttons */}
        <div
          className="flex items-center justify-end"
          style={{
            width: '100%',
            maxWidth: '496px',
            height: '40px',
            gap: '8px',
            marginTop: isFreeCancel ? '0px' : '22px',
          }}
        >
          <button
            type="button"
            onClick={onConfirm}
            className="font-nunito transition"
            style={{
              width: '157px',
              height: '40px',
              gap: '4px',
              padding: '8px 16px',
              borderRadius: '8px',
              border: '2px solid #027EAC',
              background: '#FFFFFF',
              color: '#027EAC',
              fontSize: '14px',
              fontWeight: 700,
              lineHeight: '24px',
              letterSpacing: '0px',
              textAlign: 'center',
              whiteSpace: 'nowrap',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            Cancel the booking
          </button>
          <button
            type="button"
            onClick={onClose}
            className="font-nunito transition hover:opacity-90"
            style={{
              width: '146px',
              height: '40px',
              gap: '4px',
              padding: '8px 16px',
              borderRadius: '8px',
              background: '#027EAC',
              color: '#FFFFFF',
              fontSize: '14px',
              fontWeight: 700,
              lineHeight: '24px',
              letterSpacing: '0px',
              textAlign: 'center',
              border: 'none',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            Keep the booking
          </button>
        </div>
      </div>
    </div>
  );
};

export default CancelModal;
