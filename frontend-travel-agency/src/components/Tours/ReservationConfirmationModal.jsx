import crossIcon from '../../assets/icons/Cross.svg';
import { parseCalendarDate, formatCalendarDate } from '../../utils/dateUtils';

const parseLocalDate = parseCalendarDate;

const formatBookingDate = (isoDate) => {
  if (!isoDate) return '';
  return formatCalendarDate(isoDate, { month: 'long', day: 'numeric', year: 'numeric' });
};

const getCancellationMessage = (tour, selectedDate) => {
  // Calculate free cancellation date based on tour start date and freeCancellationDaysBefore
  if (tour?.freeCancellationDaysBefore && selectedDate) {
    const startDate = parseLocalDate(selectedDate);
    const freeCancellationDate = new Date(
      startDate.getTime() - tour.freeCancellationDaysBefore * 24 * 60 * 60 * 1000
    );
    
    // Check if cancellation date has passed
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    freeCancellationDate.setHours(0, 0, 0, 0);
    
    if (today > freeCancellationDate) {
      return 'Free cancellation is no longer available';
    }
    
    const formattedCancellationDate = formatCalendarDate(freeCancellationDate, {
      month: 'long',
      day: 'numeric',
      year: 'numeric',
    });
    
    return `Free cancellation is possible until ${formattedCancellationDate}.`;
  }

  // Fallback to tour's cancellationText if available
  if (tour?.cancellationText) {
    const text = tour.cancellationText;
    if (text.toLowerCase().includes('free cancellation until')) {
      const suffix = text.replace(/free cancellation until\s*/i, '').trim();
      return `Free cancellation is possible until ${suffix}.`;
    }
    if (text.endsWith('.')) return text;
    return `${text}.`;
  }

  return 'Free cancellation is possible until the tour start date.';
};

const ReservationConfirmationModal = ({ booking, onClose }) => {
  if (!booking) return null;

  const {
    tour,
    selectedDate,
    duration,
    mealPlan,
    adults,
    children = 0,
  } = booking;

  const formattedDate = formatBookingDate(selectedDate);
  const cancellationMessage = getCancellationMessage(tour, selectedDate);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-[#42424280] p-4"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="flex w-full max-w-[544px] flex-col gap-8 rounded-[12px] bg-white p-6 max-h-[calc(100vh-32px)] overflow-y-auto sm:h-[296px] sm:max-h-none sm:overflow-hidden">
        <div className="flex h-10 w-full items-center justify-between">
          <h2 className="font-nunito text-[24px] font-bold leading-10 text-[#0B3857]">
            Booking confirmation
          </h2>

          <button
            type="button"
            onClick={onClose}
            className="flex h-6 w-6 items-center justify-center"
            aria-label="Close booking confirmation"
          >
            <img src={crossIcon} alt="Close" className="h-6 w-6" />
          </button>
        </div>

        <div className="flex w-full flex-col gap-3">
          <div className="flex w-full items-center rounded-lg bg-[#FFFAD3] p-4">
            <p className="font-nunito text-[14px] font-normal leading-6 text-[#0B3857]">
              {cancellationMessage}
            </p>
          </div>

          <p className="font-nunito text-[14px] font-normal leading-6 text-[#0B3857]">
            You have booked a tour at <strong>{tour?.name}</strong>, starting date <strong>{formattedDate}</strong> ({duration} days), <strong>{mealPlan}</strong> for <strong>{adults} {adults === 1 ? 'adult' : 'adults'}{children > 0 ? `, ${children} ${children === 1 ? 'child' : 'children'}` : ''}</strong> successfully.
          </p>

          <p className="font-nunito text-[14px] font-normal leading-6 text-[#0B3857]">
            Please upload your travel documents to the booking on the "My Tours" page and wait for the Travel Agent to contact you.
          </p>
        </div>
      </div>
    </div>
  );
};

export default ReservationConfirmationModal;
