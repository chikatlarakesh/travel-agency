import { useState, useCallback } from 'react';
import { ReactComponent as LocationIcon } from '../../assets/icons/Location.svg';
import { ReactComponent as CalendarIcon } from '../../assets/icons/Calendar.svg';
import { ReactComponent as FoodIcon } from '../../assets/icons/food.svg';
import { ReactComponent as PersonIcon } from '../../assets/icons/Person.svg';
import { ReactComponent as WalletIcon } from '../../assets/icons/wallet.svg';
import { ReactComponent as DocumentIcon } from '../../assets/icons/document.svg';
import StatusStepper from './StatusStepper';
import AgentInfo from './AgentInfo';
import CancelModal from './CancelModal';
import EditTourModal from './EditTourModal';
import ConfirmBookingChangesModal from './ConfirmBookingChangesModal';
import SuccessToast from './SuccessToast';
import UploadDocumentsModal from './UploadDocumentsModal';
import FeedbackModal from './FeedbackModal';
import { cancelBooking, updateBooking, getBookingDocuments, deleteBookingDocument, approveBookingChange, declineBookingChange, getBooking } from '../../services/bookingService';
import { submitFeedback, getMyFeedback } from '../../services/feedbackService';
import { formatCalendarDate } from '../../utils/dateUtils';

const fmt = (iso) => {
  if (!iso) return '—';
  return formatCalendarDate(iso, { month: 'short', day: 'numeric', year: 'numeric' });
};

const secondaryButtonStyle = (width) => ({
  width,
  height: '40px',
  gap: '4px',
  padding: '8px 16px',
  borderRadius: '8px',
  border: '2px solid #027EAC',
  backgroundColor: '#FFFFFF',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  fontSize: '14px',
  fontWeight: 700,
  lineHeight: '24px',
  color: '#027EAC',
  textAlign: 'center',
  cursor: 'pointer',
});

const primaryButtonStyle = (width) => ({
  width,
  height: '40px',
  borderRadius: '8px',
  border: 'none',
  backgroundColor: '#027EAC',
  color: '#FFFFFF',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  gap: '4px',
  padding: '8px 16px',
  fontSize: '14px',
  fontWeight: 700,
  lineHeight: '24px',
  cursor: 'pointer',
});

const ActionButtons = ({ booking, onCancelClick, onEditClick, onUploadClick, onFeedbackClick }) => {
  const { status, hasReview, documentsUploaded } = booking;
  const hasDocuments = documentsUploaded > 0;

  const renderBookLikeButtons = (primaryLabel) => (
    <>
      <button type="button" onClick={onCancelClick} className="font-nunito transition" style={secondaryButtonStyle('76px')}>
        Cancel
      </button>
      <button type="button" onClick={onEditClick} className="font-nunito transition" style={secondaryButtonStyle('58px')}>
        Edit
      </button>
      <button type="button" onClick={onUploadClick} className="font-nunito transition" style={primaryButtonStyle('155px')}>
        {primaryLabel}
      </button>
    </>
  );

  if (status === 'booked') {
    return (
      <div className="flex w-full max-w-[616px] items-start sm:items-center" style={{ minHeight: '40px' }}>
        <div className="ml-auto flex w-full max-w-[360px] flex-wrap items-center justify-end gap-2 sm:flex-nowrap" style={{ minHeight: '40px' }}>
          {renderBookLikeButtons(hasDocuments ? 'Update documents' : 'Upload documents')}
        </div>
      </div>
    );
  }

  if (status === 'confirmed') {
    return (
      <div className="flex w-full max-w-[616px] items-start sm:items-center" style={{ minHeight: '40px' }}>
        <div className="ml-auto flex w-full max-w-[360px] flex-wrap items-center justify-end gap-2 sm:flex-nowrap" style={{ minHeight: '40px' }}>
          {renderBookLikeButtons(hasDocuments ? 'Update documents' : 'Upload documents')}
        </div>
      </div>
    );
  }

  if (status === 'started') {
    const isUpdate = hasReview;
    return (
      <div className="flex w-full max-w-[616px] items-start sm:items-center" style={{ minHeight: '40px' }}>
        <div className="ml-auto flex w-full max-w-[360px] flex-wrap items-center justify-end gap-2 sm:flex-nowrap" style={{ minHeight: '40px' }}>
          <button
            type="button"
            onClick={onFeedbackClick}
            className="font-nunito transition hover:opacity-90"
            style={primaryButtonStyle(isUpdate ? '143px' : '124px')}
            aria-label={isUpdate ? 'Update your feedback' : 'Give feedback for this tour'}
          >
            {isUpdate ? 'Update feedback' : 'Give feedback'}
          </button>
        </div>
      </div>
    );
  }

  if (status === 'finished') {
    const isUpdate = hasReview;
    return (
      <div className="flex w-full max-w-[616px] items-start sm:items-center" style={{ minHeight: '40px' }}>
        <div className="ml-auto flex w-full max-w-[360px] flex-wrap items-center justify-end gap-2 sm:flex-nowrap" style={{ minHeight: '40px' }}>
          <button
            type="button"
            onClick={onFeedbackClick}
            className="font-nunito transition hover:opacity-90"
            style={primaryButtonStyle(isUpdate ? '143px' : '124px')}
            aria-label={isUpdate ? 'Update your feedback' : 'Give feedback for this tour'}
          >
            {isUpdate ? 'Update feedback' : 'Give feedback'}
          </button>
        </div>
      </div>
    );
  }

  return null;
};

const CancellationDetails = ({ booking }) => (
  <div className="flex w-full max-w-[360px] flex-col gap-3 sm:flex-row sm:gap-6" style={{ opacity: 1 }}>
    <div
      className="flex flex-col"
      style={{
        width: '89px',
        height: '52px',
        gap: '4px',
      }}
    >
      <div className="flex" style={{ width: '89px', height: '24px', gap: '40px' }}>
        <span
          className="font-nunito"
          style={{
            width: '89px',
            height: '24px',
            color: '#B70B0B',
            fontSize: '14px',
            fontWeight: 800,
            lineHeight: '24px',
            letterSpacing: '0%',
          }}
        >
          Cancelled by:
        </span>
      </div>
      <div className="flex" style={{ width: '53px', height: '24px', gap: '40px' }}>
        <span
          className="font-nunito"
          style={{
            width: '53px',
            height: '24px',
            color: '#B70B0B',
            fontSize: '14px',
            fontWeight: 800,
            lineHeight: '24px',
            letterSpacing: '0%',
          }}
        >
          Reason:
        </span>
      </div>
    </div>

    <div className="flex flex-col" style={{ width: '100%', maxWidth: '247px', height: '52px', gap: '4px' }}>
      <div className="flex items-center" style={{ width: '100%', maxWidth: '247px', height: '24px' }}>
        <span
          className="font-nunito"
          style={{
            width: '120px',
            height: '24px',
            color: '#B70B0B',
            fontSize: '14px',
            fontWeight: 400,
            lineHeight: '24px',
            letterSpacing: '0%',
            whiteSpace: 'nowrap',
          }}
        >
          {booking.canceledBy || '—'}
        </span>
      </div>
      <div className="flex items-center" style={{ width: '100%', maxWidth: '247px', height: '24px' }}>
        <span
          className="font-nunito"
          style={{
            width: '160px',
            height: '24px',
            color: '#B70B0B',
            fontSize: '14px',
            fontWeight: 400,
            lineHeight: '24px',
            letterSpacing: '0%',
            whiteSpace: 'nowrap',
          }}
        >
          {booking.cancelReason || '—'}
        </span>
      </div>
    </div>
  </div>
);

const DetailRow = ({ icon: Icon, text }) => (
  <div className="flex items-center" style={{ width: '100%', maxWidth: '296px', height: '24px', gap: '8px' }}>
    <Icon className="h-4 w-4 shrink-0 text-[#677883]" />
    <div className="flex items-center" style={{ width: '100%', maxWidth: '272px', height: '24px', gap: '8px' }}>
      <span
        className="font-nunito truncate"
        style={{
          width: '100%',
          maxWidth: '272px',
          height: '24px',
          color: '#0B3857',
          fontSize: '14px',
          lineHeight: '24px',
          letterSpacing: '0%',
          fontWeight: 400,
          whiteSpace: 'nowrap',
        }}
      >
        {text}
      </span>
    </div>
  </div>
);

const MyTourCard = ({ booking, onCancel }) => {
  const [localBooking, setLocalBooking] = useState(booking);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [showConfirmChangesModal, setShowConfirmChangesModal] = useState(false);
  const [showToast, setShowToast] = useState(false);
  const [toastMessage, setToastMessage] = useState('All changes has been saved successfully.');
  const [pendingBookingChanges, setPendingBookingChanges] = useState(null);
  const [pendingAgentEdit, setPendingAgentEdit] = useState(booking.pendingAgentEdit ?? false);
  const [approvalLoading, setApprovalLoading] = useState(false);

  // ── Feedback modal state ─────────────────────────────────────
  const [showFeedbackModal, setShowFeedbackModal] = useState(false);
  const [feedbackLoading, setFeedbackLoading]     = useState(false);
  // Persisted feedback so "Update feedback" pre-fills form
  const [savedFeedback, setSavedFeedback]         = useState(null);

  // Opens feedback modal; fetches existing review to pre-fill if hasReview
  const handleOpenFeedbackModal = useCallback(async () => {
    if (localBooking.hasReview && !savedFeedback) {
      try {
        const existing = await getMyFeedback(localBooking.tourId);
        setSavedFeedback({
          rating: existing.rate ?? existing.rating ?? 0,
          comment: existing.reviewContent ?? existing.comment ?? '',
        });
      } catch {
        // if fetch fails, open with empty form (still allows re-submit/update)
      }
    }
    setShowFeedbackModal(true);
  }, [localBooking.hasReview, localBooking.tourId, savedFeedback]);

  // ── Feedback submit handler ──────────────────────────────────
  const handleFeedbackSubmit = async ({ rating, comment }) => {
    const tourId = localBooking.tourId;
    if (!tourId) {
      setToastMessage('Unable to submit: tour ID not found.');
      setShowFeedbackModal(false);
      setShowToast(true);
      return;
    }

    setFeedbackLoading(true);
    try {
      await submitFeedback(tourId, { rating, comment });
      setSavedFeedback({ rating, comment });
      setLocalBooking((prev) => ({ ...prev, hasReview: true }));
      setShowFeedbackModal(false);
      setToastMessage('Your feedback has been submitted successfully.');
      setShowToast(true);
    } catch (err) {
      const httpStatus = err?.response?.status;
      const rawMsg =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        (typeof err?.response?.data === 'string' ? err.response.data : '');

      let displayMsg = 'Failed to submit feedback. Please try again.';
      if (httpStatus === 409 || rawMsg.toLowerCase().includes('duplicate') || rawMsg.toLowerCase().includes('already')) {
        displayMsg = 'You have already submitted feedback for this tour.';
      } else if (httpStatus === 401 || httpStatus === 403) {
        displayMsg = 'You must be logged in to submit feedback.';
      } else if (rawMsg) {
        displayMsg = rawMsg;
      }

      setShowFeedbackModal(false);
      setToastMessage(displayMsg);
      setShowToast(true);
      console.error('Feedback API error:', err?.response?.data || err.message);
    } finally {
      setFeedbackLoading(false);
    }
  };

  // ── Existing handlers (unchanged) ───────────────────────────
  const handleCancelConfirm = async () => {
    setShowCancelModal(false);
    try {
      await cancelBooking(localBooking.id);
      setLocalBooking({
        ...localBooking,
        status: 'canceled',
        canceledBy: 'Tourist',
        cancelReason: '—',
        canceledAfterStep: localBooking.status === 'confirmed' ? 1 : 0,
      });
      onCancel?.(localBooking.id);
    } catch (err) {
      console.error('Cancel booking API error:', err?.response?.data || err.message);
    }
  };

  const handleSaveEdit = (updatedBooking) => {
    setPendingBookingChanges(updatedBooking);
    setShowEditModal(false);
    setShowConfirmChangesModal(true);
  };

  const handleConfirmChanges = async () => {
    if (pendingBookingChanges) {
      try {
        // Extract just the fields we need for the update API
        const { traveler, travelerCount, childrenCount = 0, mealPlan } = pendingBookingChanges;
        console.log('🎬 handleConfirmChanges starting for booking:', localBooking.id);
        console.log('📝 Original traveler:', localBooking.traveler);
        console.log('📝 New traveler:', traveler);
        console.log('📝 Traveler count:', travelerCount);
        console.log('📝 Children count:', childrenCount);
        console.log('📝 Meal plan:', mealPlan);
        
        // Parse traveler names into personal details array
        const totalCount = travelerCount + childrenCount;
        const personalDetails = [];
        if (traveler) {
          const names = traveler.split(',').map(n => n.trim());
          for (let i = 0; i < totalCount; i++) {
            const parts = (names[i] || '').split(' ');
            personalDetails.push({
              firstName: parts[0] || 'Guest',
              lastName: parts.slice(1).join(' ') || 'User',
            });
          }
        } else {
          // Fallback: create empty personal details
          for (let i = 0; i < totalCount; i++) {
            personalDetails.push({ firstName: 'Guest', lastName: 'User' });
          }
        }

        console.log('📦 Personal details for API:', personalDetails);
        
        console.log('🌐 Calling updateBooking API...');
        await updateBooking(localBooking.id, {
          personalDetails,
          guests: travelerCount,
          children: childrenCount,
          mealPlan,
          totalPrice: pendingBookingChanges.totalPrice,
        });
        console.log('✅ API call successful!');

        // Delete documents for any travelers who were removed in this edit
        const oldNames = (localBooking.traveler || '')
          .split(',').map((n) => n.trim().toLowerCase()).filter(Boolean);
        const newNames = (pendingBookingChanges.traveler || '')
          .split(',').map((n) => n.trim().toLowerCase()).filter(Boolean);
        const removedNames = oldNames.filter((n) => !newNames.includes(n));
        if (removedNames.length > 0) {
          try {
            const existingDocs = await getBookingDocuments(localBooking.id);
            const deletePromises = [];
            if (existingDocs.guestDocuments) {
              existingDocs.guestDocuments.forEach((guest) => {
                if (removedNames.includes((guest.userName || '').toLowerCase())) {
                  (guest.documents || []).forEach((doc) => {
                    deletePromises.push(deleteBookingDocument(localBooking.id, doc.id));
                  });
                }
              });
            }
            await Promise.allSettled(deletePromises);
          } catch (_) {
            // non-critical — document cleanup failed silently
          }
        }

        let updatedDocCount = pendingBookingChanges.documentsUploaded ?? localBooking.documentsUploaded;
        try {
          const docs = await getBookingDocuments(localBooking.id);
          updatedDocCount = Array.isArray(docs) ? docs.length : updatedDocCount;
        } catch (_) {
          // non-critical; keep existing count
        }
        setLocalBooking({ ...pendingBookingChanges, documentsUploaded: updatedDocCount });
        setPendingBookingChanges(null);
        setShowConfirmChangesModal(false);
        setToastMessage('All changes has been saved successfully.');
        setShowToast(true);
      } catch (err) {
        console.error('❌ Update booking error:', err);
        setToastMessage('Failed to save changes. Please try again.');
        setShowToast(true);
        setPendingBookingChanges(null);
        setShowConfirmChangesModal(false);
      }
    }
  };

  const handleDeclineChanges = () => {
    setPendingBookingChanges(null);
    setShowConfirmChangesModal(false);
  };

  const handleApproveAgentEdit = async () => {
    setApprovalLoading(true);
    try {
      await approveBookingChange(localBooking.id);
      setPendingAgentEdit(false);
      setToastMessage('Changes approved successfully.');
      setShowToast(true);
    } catch (err) {
      console.error('Approve failed:', err?.response?.data || err.message);
    } finally {
      setApprovalLoading(false);
    }
  };

  const handleDeclineAgentEdit = async () => {
    setApprovalLoading(true);
    try {
      await declineBookingChange(localBooking.id);
      // Re-fetch to get backend-reverted data
      const reverted = await getBooking(localBooking.id);
      setLocalBooking({ ...reverted, pendingAgentEdit: false });
      setPendingAgentEdit(false);
      setToastMessage('Changes declined.');
      setShowToast(true);
    } catch (err) {
      console.error('Decline failed:', err?.response?.data || err.message);
    } finally {
      setApprovalLoading(false);
    }
  };

  return (
    <>
      {showCancelModal && (
        <CancelModal
          booking={localBooking}
          onClose={() => setShowCancelModal(false)}
          onConfirm={handleCancelConfirm}
        />
      )}
      {showEditModal && (
        <EditTourModal
          booking={localBooking}
          onClose={() => setShowEditModal(false)}
          onSave={handleSaveEdit}
        />
      )}
      {showUploadModal && (
        <UploadDocumentsModal
          onClose={() => setShowUploadModal(false)}
          travelerName={localBooking.traveler}
          bookingId={localBooking.id}
          hasExistingDocuments={localBooking.documentsUploaded > 0}
          onSaveSuccess={(newCount) => {
            setLocalBooking((prev) => ({ ...prev, documentsUploaded: newCount }));
            setToastMessage('Your document has been uploaded successfully.');
            setShowToast(true);
          }}
        />
      )}
      {showToast && <SuccessToast message={toastMessage} onClose={() => setShowToast(false)} />}
      {showConfirmChangesModal && pendingBookingChanges && (
        <ConfirmBookingChangesModal
          originalBooking={localBooking}
          updatedBooking={pendingBookingChanges}
          onClose={handleDeclineChanges}
          onDecline={handleDeclineChanges}
          onConfirm={handleConfirmChanges}
        />
      )}
      {showFeedbackModal && (
        <FeedbackModal
          onClose={() => setShowFeedbackModal(false)}
          onSubmit={handleFeedbackSubmit}
          isLoading={feedbackLoading}
          initialRating={savedFeedback?.rating ?? 0}
          initialComment={savedFeedback?.comment ?? ''}
        />
      )}

      <article
        className="flex w-full flex-col overflow-hidden rounded-[12px] bg-white p-4 transition-shadow hover:shadow-lg sm:p-6"
        style={{
          boxShadow: '0px 2px 10px 6px rgba(2, 126, 172, 0.2)',
          width: '664px',
          maxWidth: '100%',
          borderRadius: '12px',
          minHeight: '376px',
          gap: '24px',
        }}
      >
        <div className="w-full overflow-x-auto" style={{ height: '32px', boxSizing: 'border-box' }}>
          <StatusStepper
            status={localBooking.status}
            canceledAfterStep={localBooking.canceledAfterStep ?? 1}
          />
        </div>

        {pendingAgentEdit && (
          <div
            className="flex w-full items-start justify-between gap-3 rounded-xl px-4 py-3"
            style={{ background: '#FFF8E6', border: '1px solid #F0A500' }}
          >
            <div className="flex flex-col gap-1">
              <span className="font-nunito text-[14px] font-extrabold text-[#0B3857]">
                Your travel agent proposed changes
              </span>
              <span className="font-nunito text-[13px] text-[#677883]">
                Please review the updated details and approve or decline.
              </span>
            </div>
            <div className="flex shrink-0 gap-2">
              <button
                type="button"
                disabled={approvalLoading}
                onClick={handleDeclineAgentEdit}
                className="font-nunito text-[13px] font-bold transition"
                style={{
                  padding: '6px 14px', borderRadius: '8px', border: '2px solid #027EAC',
                  background: '#fff', color: '#027EAC', cursor: approvalLoading ? 'not-allowed' : 'pointer',
                }}
              >
                Decline
              </button>
              <button
                type="button"
                disabled={approvalLoading}
                onClick={handleApproveAgentEdit}
                className="font-nunito text-[13px] font-bold transition"
                style={{
                  padding: '6px 14px', borderRadius: '8px', border: '2px solid #027EAC',
                  background: '#027EAC', color: '#fff', cursor: approvalLoading ? 'not-allowed' : 'pointer',
                }}
              >
                Approve
              </button>
            </div>
          </div>
        )}

        <div
          className="flex w-full flex-col"
          style={{
            width: '100%',
            maxWidth: '616px',
            minHeight: '328px',
            gap: '24px',
            boxSizing: 'border-box',
          }}
        >
          <div className="flex w-full flex-col items-start gap-4 sm:h-[64px] sm:flex-row">
            <div className="h-[64px] w-[120px] shrink-0 overflow-hidden rounded-[12px] bg-[#E7F9FF]">
              <img
                src={localBooking.image}
                alt={localBooking.name}
                className="h-full w-full object-cover"
                loading="lazy"
              />
            </div>
            <div
              className="flex min-w-0 flex-col"
              style={{
                width: '100%',
                maxWidth: '410px',
                height: '48px',
                opacity: 1,
              }}
            >
              <h3
                className="font-nunito font-bold text-[#0B3857] truncate"
                style={{
                  width: '100%',
                  maxWidth: '410px',
                  height: '32px',
                  fontSize: '18px',
                  lineHeight: '32px',
                  letterSpacing: '0px',
                  fontWeight: 700,
                }}
              >
                {localBooking.name}
              </h3>
              <div
                className="flex items-center"
                style={{
                  width: '100%',
                  maxWidth: '410px',
                  height: '16px',
                  gap: '4px',
                }}
              >
                <LocationIcon className="h-4 w-4 shrink-0 text-[#677883]" />
                <span
                  className="font-nunito truncate"
                  style={{
                    width: '100%',
                    maxWidth: '390px',
                    height: '16px',
                    color: '#677883',
                    fontSize: '12px',
                    lineHeight: '16px',
                    letterSpacing: '0%',
                    fontWeight: 400,
                  }}
                >
                  {localBooking.location}
                </span>
              </div>
            </div>
          </div>

          <div className="flex w-full max-w-[616px] flex-col gap-6 md:flex-row" style={{ height: 'auto' }}>
            <div className="flex flex-col overflow-hidden" style={{ width: '100%', maxWidth: '296px', gap: '16px' }}>
              <div className="flex" style={{ width: '138px', height: '24px', gap: '24px' }}>
                <p
                  className="font-nunito"
                  style={{
                    width: '138px',
                    height: '24px',
                    color: '#0B3857',
                    fontSize: '14px',
                    lineHeight: '24px',
                    letterSpacing: '0%',
                    fontWeight: 800,
                  }}
                >
                  Tour details
                </p>
              </div>

              <div className="flex flex-col" style={{ width: '100%', maxWidth: '296px', gap: '4px' }}>
                <DetailRow
                  icon={CalendarIcon}
                  text={
                    localBooking.startDate
                      ? `${fmt(localBooking.startDate)}${localBooking.duration ? ` (${localBooking.duration} days)` : ''}`
                      : '—'
                  }
                />
                <DetailRow icon={FoodIcon} text={localBooking.mealPlan} />
                <div className="flex items-start" style={{ width: '100%', maxWidth: '296px', gap: '8px' }}>
                  <PersonIcon className="h-4 w-4 shrink-0 text-[#677883] mt-[4px]" />
                  <div className="flex flex-col" style={{ gap: '2px' }}>
                    <span className="font-nunito" style={{ color: '#0B3857', fontSize: '14px', lineHeight: '24px', fontWeight: 400 }}>
                      {localBooking.travelerCount} adult{localBooking.travelerCount !== 1 ? 's' : ''}{(localBooking.childrenCount ?? 0) > 0 ? `, ${localBooking.childrenCount} child${localBooking.childrenCount !== 1 ? 'ren' : ''}` : ''}
                    </span>
                  </div>
                </div>
                <DetailRow icon={WalletIcon} text={`Total price $${localBooking.totalPrice != null ? localBooking.totalPrice.toLocaleString() : '—'}`} />
                <DetailRow
                  icon={DocumentIcon}
                  text={`Documents uploaded: ${localBooking.documentsUploaded} item${localBooking.documentsUploaded !== 1 ? 's' : ''}`}
                />
              </div>
            </div>

            <div className="flex flex-col" style={{ width: '100%', maxWidth: '296px', gap: '16px' }}>
              <AgentInfo agent={localBooking.agent} />
            </div>
          </div>

          {localBooking.status === 'canceled' ? (
            <CancellationDetails booking={localBooking} />
          ) : (
            <ActionButtons
              booking={localBooking}
              onCancelClick={() => setShowCancelModal(true)}
              onEditClick={() => setShowEditModal(true)}
              onUploadClick={() => setShowUploadModal(true)}
              onFeedbackClick={handleOpenFeedbackModal}
            />
          )}
        </div>
      </article>
    </>
  );
};

export default MyTourCard;
