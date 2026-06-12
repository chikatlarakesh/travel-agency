import { useState, useEffect } from 'react';
import StatusStepper from '../MyTours/StatusStepper';
import ActionButtons from './ActionButtons';
import DocumentsModal from './DocumentsModal';
import EditTourModal from '../MyTours/EditTourModal';
import SuccessToast from '../MyTours/SuccessToast';
import { updateBooking } from '../../services/bookingService';
import { ReactComponent as LocationIcon } from '../../assets/icons/Location.svg';
import { ReactComponent as CalendarIcon } from '../../assets/icons/Calendar.svg';
import { ReactComponent as FoodIcon } from '../../assets/icons/food.svg';
import { ReactComponent as WalletIcon } from '../../assets/icons/wallet.svg';
import { ReactComponent as PersonIcon } from '../../assets/icons/Person.svg';
import { ReactComponent as EmailIcon } from '../../assets/icons/mail.svg';
import { ReactComponent as DocumentIcon } from '../../assets/icons/document.svg';
import { formatCalendarDate } from '../../utils/dateUtils';
import './BookingCard.css';

const fmt = (iso) => {
  if (!iso) return '—';
  return formatCalendarDate(iso, { month: 'short', day: 'numeric', year: 'numeric' });
};

const DetailRow = ({ icon: Icon, text, isLink, onClick, boldSuffix }) => (
  <div className="booking-card__detail-row">
    <Icon className="booking-card__detail-icon" />
    {boldSuffix ? (
      <span className="booking-card__detail-text">
        {text} <span className="booking-card__detail-text--bold">{boldSuffix}</span>
      </span>
    ) : (
      <span
        className={`booking-card__detail-text ${isLink ? 'booking-card__detail-text--link' : ''}`}
        onClick={isLink ? onClick : undefined}
        role={isLink ? 'button' : undefined}
        tabIndex={isLink ? 0 : undefined}
        onKeyDown={isLink ? (e) => e.key === 'Enter' && onClick?.() : undefined}
      >
        {text}
      </span>
    )}
  </div>
);

const BookingCard = ({ booking, onCheckAndConfirm, onCancel }) => {
  const [showDocs, setShowDocs] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showToast, setShowToast] = useState(false);
  const [localBooking, setLocalBooking] = useState(booking);

  // Sync when parent updates the booking (e.g. after confirm/cancel)
  useEffect(() => {
    setLocalBooking(booking);
  }, [booking]); // eslint-disable-line react-hooks/exhaustive-deps

  const {
    hotelName,
    location,
    image,
    status,
    startDate,
    durationDays,
    mealPlan,
    totalPrice,
    customer,
    canceledAfterStep,
    freeCancellationDate,
    pendingCustomerApproval,
  } = localBooking;

  // Adapt agent booking shape to what EditTourModal expects
  const editBookingAdapter = {
    ...localBooking,
    name: localBooking.hotelName,
    duration: localBooking.durationDays,
    traveler: localBooking.traveler || localBooking.customer?.name || '',
    travelerCount: localBooking.customer?.adults ?? 1,
    childrenCount: localBooking.customer?.children ?? 0,
    maxGuests: localBooking.maxGuests ?? 8,
  };

  const handleSaveEdit = async (updated) => {
    const totalCount = (updated.travelerCount ?? 1) + (updated.childrenCount ?? 0);
    const names = (updated.traveler || '').split(',').map((n) => n.trim());
    const personalDetails = Array.from({ length: totalCount }, (_, i) => {
      const parts = (names[i] || '').split(' ');
      return { firstName: parts[0] || 'Guest', lastName: parts.slice(1).join(' ') || 'User' };
    });
    try {
      await updateBooking(localBooking.id, {
        personalDetails,
        guests: updated.travelerCount,
        children: updated.childrenCount ?? 0,
        mealPlan: updated.mealPlan,
        totalPrice: updated.totalPrice,
      });
      setLocalBooking((prev) => ({
        ...prev,
        mealPlan: updated.mealPlan,
        totalPrice: updated.totalPrice,
        durationDays: updated.duration ?? prev.durationDays,
        startDate: updated.startDate ?? prev.startDate,
        traveler: updated.traveler,
        pendingCustomerApproval: true,
        customer: {
          ...prev.customer,
          name: (updated.traveler || '').split(',')[0].trim() || prev.customer.name,
          adults: updated.travelerCount ?? prev.customer.adults,
          children: updated.childrenCount ?? prev.customer.children,
        },
      }));
      setShowEditModal(false);
      setShowToast(true);
    } catch (err) {
      console.error('Edit booking failed:', err?.response?.data || err.message);
      setShowEditModal(false);
    }
  };

  const showActions = status !== 'canceled' && status !== 'started' && status !== 'finished';

  return (
    <article className="booking-card">
      {showDocs && (
        <DocumentsModal bookingId={localBooking.id} onClose={() => setShowDocs(false)} />
      )}
      {showEditModal && (
        <EditTourModal
          booking={editBookingAdapter}
          onClose={() => setShowEditModal(false)}
          onSave={handleSaveEdit}
        />
      )}
      {showToast && (
        <SuccessToast
          message="All changes has been saved successfully."
          onClose={() => setShowToast(false)}
        />
      )}
      {/* Progress Stepper */}
      <div className="booking-card__stepper">
        <StatusStepper status={status} canceledAfterStep={canceledAfterStep ?? 1} />
      </div>

      {/* Card Content */}
      <div className="booking-card__content">
        {/* Header: image + hotel name + location */}
        <div className="booking-card__header">
          <img
            src={image}
            alt={hotelName}
            className="booking-card__image"
            loading="lazy"
          />
          <div className="booking-card__info">
            <h3 className="booking-card__hotel-name">{hotelName}</h3>
            <div className="booking-card__location">
              <LocationIcon className="booking-card__location-icon" />
              <span className="booking-card__location-text">{location}</span>
            </div>
          </div>
        </div>

        {/* Two-column details */}
        <div className="booking-card__details">
          {/* Tour details */}
          <div className="booking-card__details-column">
            <p className="booking-card__details-title">Tour details</p>
            <div className="booking-card__details-list">
              <DetailRow icon={CalendarIcon} text={`${fmt(startDate)} (${durationDays} days)`} />
              <DetailRow icon={FoodIcon} text={mealPlan} />
              <DetailRow icon={WalletIcon} text="Total price" boldSuffix={`$${totalPrice != null ? totalPrice.toLocaleString() : '—'}`} />
            </div>
          </div>

          {/* Customer details */}
          <div className="booking-card__details-column">
            <p className="booking-card__details-title">Customer details</p>
            <div className="booking-card__details-list">
              <DetailRow icon={PersonIcon} text={`${customer.name} (${customer.adults ?? 1} adult${(customer.adults ?? 1) !== 1 ? 's' : ''}${customer.children > 0 ? `, ${customer.children} child${customer.children !== 1 ? 'ren' : ''}` : ''})`} />
              <DetailRow icon={EmailIcon} text={customer.email} />
              <DetailRow
                icon={DocumentIcon}
                text={`Documents uploaded: ${customer.documentsUploaded} items`}
                isLink={customer.documentsUploaded > 0}
                onClick={() => setShowDocs(true)}
              />
            </div>
          </div>
        </div>

        {/* Footer: free-cancel message + actions */}
        {showActions && (
          <div className="booking-card__actions">
            {freeCancellationDate ? (
              <div className="booking-card__cancellation">
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg" className="booking-card__cancellation-icon" aria-hidden="true">
                  <path d="M14 4.5L6.5 12L2.5 8" stroke="#118819" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
                <span className="booking-card__cancellation-text">
                  Free cancellation until {fmt(freeCancellationDate)}
                </span>
              </div>
            ) : null}
            <ActionButtons
              status={status}
              pendingCustomerApproval={pendingCustomerApproval}
              onCancel={() => onCancel?.(localBooking)}
              onEdit={() => setShowEditModal(true)}
              onConfirm={() => onCheckAndConfirm?.(localBooking)}
            />
          </div>
        )}
      </div>
    </article>
  );
};

export default BookingCard;
