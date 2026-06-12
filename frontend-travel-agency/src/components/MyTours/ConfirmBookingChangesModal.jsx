import { parseCalendarDate } from '../../utils/dateUtils';

const formatDateWithOrdinal = (iso) => {
  if (!iso) return '—';
  const date = parseCalendarDate(iso);
  if (Number.isNaN(date.getTime())) return '—';

  const d = date.getUTCDate();
  const v = d % 100;
  const suffix = (v >= 11 && v <= 13) ? 'th' : (['th', 'st', 'nd', 'rd'][Math.min(d % 10, 4)] || 'th');

  return `${date.toLocaleDateString('en-US', { month: 'long' })} ${d}${suffix}, ${date.getUTCFullYear()}`;
};

const getTravelerLabel = (count, childrenCount, names) => {
  const safeCount = count ?? 0;
  const safeChildrenCount = childrenCount ?? 0;
  const safeNames = names || '—';
  
  // Split names into adult and children portions
  const nameList = safeNames.split(',').map(n => n.trim()).filter(n => n);
  const adultNames = nameList.slice(0, safeCount).join(', ') || '—';
  const childrenNames = nameList.slice(safeCount).join(', ') || '—';
  
  let label = `${safeCount} adult${safeCount === 1 ? '' : 's'} (${adultNames})`;
  if (safeChildrenCount > 0) {
    label += `, ${safeChildrenCount} child${safeChildrenCount === 1 ? '' : 'ren'} (${childrenNames})`;
  }
  
  return label;
};

const ConfirmBookingChangesModal = ({
  originalBooking,
  updatedBooking,
  onClose,
  onDecline,
  onConfirm,
}) => {
  const tourName = updatedBooking?.name || originalBooking?.name || '—';
  const tourDate = formatDateWithOrdinal(updatedBooking?.startDate || originalBooking?.startDate);

  const originalTourists = getTravelerLabel(originalBooking?.travelerCount, originalBooking?.childrenCount, originalBooking?.traveler);
  const updatedTourists = getTravelerLabel(updatedBooking?.travelerCount, updatedBooking?.childrenCount, updatedBooking?.traveler);

  const originalMealPlan = originalBooking?.mealPlan || '—';
  const updatedMealPlan = updatedBooking?.mealPlan || '—';

  // Determine which fields actually changed
  const touristCountChanged = originalBooking?.travelerCount !== updatedBooking?.travelerCount 
    || originalBooking?.childrenCount !== updatedBooking?.childrenCount
    || originalBooking?.traveler !== updatedBooking?.traveler;
  const mealPlanChanged = originalMealPlan !== updatedMealPlan;

  return (
    <div
      className="fixed inset-0 z-[70] flex items-center justify-center"
      style={{ background: '#42424280' }}
      onClick={onClose}
    >
      <div
        className="relative flex flex-col overflow-y-auto bg-white"
        style={{
          width: '544px',
          maxWidth: 'calc(100vw - 24px)',
          height: '456px',
          maxHeight: 'calc(100vh - 24px)',
          gap: '32px',
          borderRadius: '12px',
          padding: '24px',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{ width: '496px', height: '40px' }}>
          <div style={{ width: '496px', height: '40px' }}>
            <div className="flex items-center justify-between" style={{ width: '496px', height: '40px' }}>
              <div className="flex" style={{ width: '336px', height: '40px', gap: '18px' }}>
                <h2
                  className="font-nunito"
                  style={{
                    width: '336px',
                    height: '40px',
                    color: '#0B3857',
                    fontSize: '24px',
                    fontWeight: 700,
                    lineHeight: '40px',
                    letterSpacing: '0px',
                  }}
                >
                  Confirm tour booking changes
                </h2>
              </div>

              <button
                type="button"
                onClick={onClose}
                className="flex items-center justify-center text-[#0B3857]"
                style={{ width: '24px', height: '24px' }}
                aria-label="Close"
              >
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                </svg>
              </button>
            </div>
          </div>
        </div>

        <div className="flex flex-col" style={{ width: '496px', height: '264px', gap: '12px' }}>
          <p
            className="font-nunito"
            style={{
              color: '#0B3857',
              fontSize: '14px',
              fontWeight: 400,
              lineHeight: '24px',
              letterSpacing: '0px',
            }}
          >
            Your booking for <strong>{tourName}</strong> on <strong>{tourDate}</strong> has been updated by the
            travel agent.
          </p>

          <p
            className="font-nunito"
            style={{
              color: '#0B3857',
              fontSize: '14px',
              fontWeight: 700,
              lineHeight: '24px',
              letterSpacing: '0px',
            }}
          >
            Changes:
          </p>

          <ul
            className="list-disc pl-5"
            style={{
              color: '#0B3857',
              fontSize: '14px',
              fontWeight: 400,
              lineHeight: '24px',
              letterSpacing: '0px',
              marginTop: '-8px',
            }}
          >
            {touristCountChanged && (
              <li>Number of tourists: {originalTourists} -&gt; {updatedTourists}.</li>
            )}
            {mealPlanChanged && (
              <li>Meal plan: {originalMealPlan} -&gt; {updatedMealPlan}.</li>
            )}
          </ul>

          <p
            className="font-nunito"
            style={{
              color: '#0B3857',
              fontSize: '14px',
              fontWeight: 400,
              lineHeight: '24px',
              letterSpacing: '0px',
            }}
          >
            All other details of this booking remain the same.
          </p>

          <p
            className="font-nunito"
            style={{
              color: '#0B3857',
              fontSize: '14px',
              fontWeight: 400,
              lineHeight: '24px',
              letterSpacing: '0px',
            }}
          >
            Please review the changes and confirm your updated booking, or contact us if you need further assistance.
          </p>
        </div>

        <div className="ml-auto flex items-center" style={{ width: '496px', height: '40px', gap: '8px', justifyContent: 'flex-end' }}>
          <button
            type="button"
            onClick={onDecline}
            className="font-nunito"
            style={{
              width: '138px',
              height: '40px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              whiteSpace: 'nowrap',
              borderRadius: '8px',
              border: '2px solid #027EAC',
              background: '#FFFFFF',
              color: '#027EAC',
              fontSize: '14px',
              fontWeight: 700,
              lineHeight: '24px',
              textAlign: 'center',
              padding: '8px 16px',
            }}
          >
            Decline changes
          </button>

          <button
            type="button"
            onClick={onConfirm}
            className="font-nunito"
            style={{
              width: '142px',
              height: '40px',
              borderRadius: '8px',
              border: 'none',
              background: '#027EAC',
              color: '#FFFFFF',
              fontSize: '14px',
              fontWeight: 700,
              lineHeight: '24px',
              textAlign: 'center',
              padding: '8px 16px',
            }}
          >
            Confirm changes
          </button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmBookingChangesModal;
