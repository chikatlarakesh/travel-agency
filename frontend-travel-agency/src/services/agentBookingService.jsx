import api from './api';
import { MAX_GUESTS_MAP } from './travelService';
import TourImg1 from '../assets/images/TourImg1.png';
import TourImg2 from '../assets/images/TourImg2.png';
import TourImg3 from '../assets/images/TourImg3.png';
import TourImg4 from '../assets/images/TourImg4.png';
import TourImg5 from '../assets/images/TourImg5.png';
import TourImg6 from '../assets/images/TourImg6.png';

export const getAgentBookingDocuments = async (bookingId) => {
  const response = await api.get(`/travel-agent/bookings/${bookingId}/documents`);
  return response.data;
};

const TOUR_IMAGE_MAP = {
  't-1001': TourImg1, 't-1002': TourImg2, 't-1003': TourImg3,
  't-1004': TourImg4, 't-1005': TourImg5, 't-1006': TourImg6,
  't-1007': TourImg1, 't-1008': TourImg2, 't-1009': TourImg3,
  't-1010': TourImg4, 't-1011': TourImg5, 't-1012': TourImg6,
  't-1013': TourImg1, 't-1014': TourImg2,
};

const resolveImage = (tourImageUrl) => {
  const match = (tourImageUrl || '').match(/tours\/(t-\d+)\//);
  const tourId = match ? match[1] : null;
  return (tourId && TOUR_IMAGE_MAP[tourId]) || TourImg1;
};

const resolveTourId = (tourImageUrl) => {
  const match = (tourImageUrl || '').match(/tours\/(t-\d+)\//);
  return match ? match[1] : null;
};

const MEAL_LABEL_MAP = {
  BB: 'Breakfast (BB)',
  HB: 'Half-board (HB)',
  FB: 'Full-board (FB)',
  AI: 'All inclusive (AI)',
};

const toMealLabel = (code = '') => MEAL_LABEL_MAP[code.toUpperCase()] || code;

const DOCUMENT_TYPE_PASSPORT = 'PASSPORT';
const DOCUMENT_TYPE_PAYMENT = 'PAYMENT_RECEIPT';

const mapDocumentsToObject = (documents) => {
  if (!Array.isArray(documents)) return {};
  const result = {};
  const passport = documents.find((d) => d.type === DOCUMENT_TYPE_PASSPORT);
  const payment = documents.find((d) => d.type === DOCUMENT_TYPE_PAYMENT);
  if (passport) result.passport = passport.fileName || passport.fileUrl;
  if (payment) result.payment = payment.fileName || payment.fileUrl;
  return result;
};

const normalizeBooking = (booking) => {
  const cd = booking.customerDetails || {};
  const firstName = cd.firstName || '';
  const lastName = cd.lastName || '';
  const customerName = [firstName, lastName].filter(Boolean).join(' ') || 'Unknown';

  // canceledAfterStep: 1 if canceled after confirmation, 0 if canceled from booked
  const canceledAfterStep =
    booking.state === 'CANCELED' && booking.confirmation ? 1 : 0;

  const tourId = resolveTourId(booking.tourImageUrl);
  const maxGuests = (tourId && MAX_GUESTS_MAP[tourId]) ?? 8;

  // Build full traveler string from guestDetails (all adults + children)
  const travelerNames = Array.isArray(booking.guestDetails) && booking.guestDetails.length > 0
    ? booking.guestDetails.map((g) => `${g.firstName || ''} ${g.lastName || ''}`.trim()).join(', ')
    : customerName;

  return {
    id: booking.bookingId,
    hotelName: booking.tourName || '—',
    location: booking.destination || '—',
    image: resolveImage(booking.tourImageUrl),
    rating: booking.tourRating ?? null,
    status: booking.state ? booking.state.toLowerCase() : 'booked',
    startDate: booking.startDate,
    durationDays: booking.duration ? parseInt(booking.duration, 10) || booking.duration : 0,
    mealPlan: toMealLabel(booking.mealPlan || ''),
    totalPrice: booking.totalPrice,
    freeCancellationDate: booking.freeCancellationDate,
    canceledAfterStep,
    maxGuests,
    traveler: travelerNames,
    pendingCustomerApproval: !!(booking.customerApproval && !booking.customerApproval.approvalGiven),
    customer: {
      name: customerName,
      email: cd.email || '',
      adults: cd.adults ?? 1,
      children: cd.children ?? 0,
      documentsUploaded: booking.documentCount || 0,
      documents: mapDocumentsToObject(booking.documents),
    },
  };
};

export const getAgentBookings = async () => {
  const response = await api.get('/travel-agent/bookings');
  const data = response.data;
  const list = Array.isArray(data) ? data : (data.bookings || []);
  return list.map(normalizeBooking);
};

export const cancelAgentBooking = async (bookingId, cancelReason) => {
  const response = await api.delete(`/bookings/${bookingId}/cancel`, {
    data: { reason: cancelReason },
  });
  return response.data;
};

export const confirmAgentBooking = async (bookingId) => {
  await api.post(`/bookings/${bookingId}/confirm`);
};

