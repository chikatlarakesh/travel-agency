import api from './api';
import { MAX_GUESTS_MAP, TOUR_DATES_MAP, TOUR_DURATIONS_MAP } from './travelService';
import TourImg1 from '../assets/images/TourImg1.png';
import TourImg2 from '../assets/images/TourImg2.png';
import TourImg3 from '../assets/images/TourImg3.png';
import TourImg4 from '../assets/images/TourImg4.png';
import TourImg5 from '../assets/images/TourImg5.png';
import TourImg6 from '../assets/images/TourImg6.png';

const TOUR_IMAGE_MAP = {
  't-1001': TourImg1, 't-1002': TourImg2, 't-1003': TourImg3,
  't-1004': TourImg4, 't-1005': TourImg5, 't-1006': TourImg6,
  't-1007': TourImg1, 't-1008': TourImg2, 't-1009': TourImg3,
  't-1010': TourImg4, 't-1011': TourImg5, 't-1012': TourImg6,
  't-1013': TourImg1, 't-1014': TourImg2,
};

/**
 * Maps a frontend meal plan label to the backend code (BB, HB, FB, AI).
 */
const toMealCode = (mealPlan = '') => {
  const key = mealPlan.toLowerCase().replace(/[^a-z]/g, '');
  if (key.includes('allinclusive') || key === 'ai') return 'AI';
  if (key.includes('fullboard') || key === 'fb') return 'FB';
  if (key.includes('halfboard') || key === 'hb') return 'HB';
  if (key.includes('breakfast') || key === 'bb') return 'BB';
  return 'BB'; // default
};

/**
 * Maps a backend meal plan code to a friendly display label.
 */
const toMealLabel = (code = '') => {
  const map = { BB: 'Breakfast (BB)', HB: 'Half-board (HB)', FB: 'Full-board (FB)', AI: 'All inclusive (AI)' };
  return map[code.toUpperCase()] || code;
};

const CANCELLATION_REASON_LABELS = {
  CUSTOMERS_EMERGENCY: "Customer's Emergency",
  WEATHER_CONDITIONS:  'Weather Conditions',
  OPERATIONAL_ISSUE:   'Operational Issue',
  NATURAL_DISASTER:    'Natural Disaster',
  HEALTH_CONCERN:      'Health Concern',
  OTHER:               'Other',
};

/**
 * POST /api/v1/bookings
 * Creates a new booking and returns { freeCancelation, details }.
 */
export const createBooking = async ({ userId, tourId, selectedDate, duration, mealPlan, adults, children = 0, customers, totalPrice }) => {
  const payload = {
    userId,
    tourId,
    date: selectedDate,
    duration: String(duration),
    mealPlan: toMealCode(mealPlan),
    guests: { adult: adults, children },
    personalDetails: customers.map(({ firstName, lastName }) => ({
      firstName: firstName || 'Guest',
      lastName: lastName || 'User',
    })),
    totalPrice: totalPrice,
  };
  const response = await api.post('/bookings', payload);
  return response.data; // { freeCancelation, details }
};

/**
 * GET /api/v1/bookings?userId=...
 * Returns the user's bookings mapped to the frontend booking shape.
 */
export const getUserBookings = async (userId) => {
  const response = await api.get('/bookings', { params: { userId } });
  const raw = response.data.bookings || [];
  return raw.map(mapApiBookingToLocal);
};

/**
 * DELETE /api/v1/bookings/{bookingId}
 * Cancels a booking.
 */
export const cancelBooking = async (bookingId) => {
  await api.delete(`/bookings/${bookingId}`);
};

/**
 * PUT /api/v1/bookings/{bookingId}
 * Updates booking details (meal plan, personal details, guest count, total price).
 */
export const updateBooking = async (bookingId, { personalDetails, guests, children = 0, mealPlan, totalPrice }) => {
  const payload = {
    personalDetails: personalDetails.map(({ firstName, lastName }) => ({
      firstName: firstName || 'Guest',
      lastName: lastName || 'User',
    })),
    guests: { adult: guests, children },
    mealPlan: toMealCode(mealPlan),
    totalPrice: totalPrice,
  };
  console.log('🔄 Sending updateBooking request for bookingId:', bookingId);
  console.log('📦 Payload:', payload);
  try {
    const response = await api.put(`/bookings/${bookingId}`, payload);
    console.log('✅ updateBooking successful:', response);
    return response;
  } catch (error) {
    console.error('❌ updateBooking failed:', {
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data,
      message: error.message,
    });
    throw error;
  }
};

// ---------------------------------------------------------------------------
// Internal mapper: backend BookedTourDTO → frontend booking object
// ---------------------------------------------------------------------------

const parseDate = (dateStr) => {
  // format: "Jan 4, 2026 (7 days)" or ISO "2026-01-04"
  if (!dateStr) return { isoDate: null, durationNum: null };

  // ISO format
  if (/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) {
    return { isoDate: dateStr, durationNum: null };
  }

  // "Jan 4, 2026 (7 days)" or "Jan 4, 2026 (7)" format
  const match = dateStr.match(/^(.+?)\s*\((\d+)(?:\s*days?)?\)$/i);
  if (match) {
    // Parse locale string (e.g. "Aug 12, 2026") safely by extracting parts
    const localDate = new Date(match[1].trim());
    let isoDate = null;
    if (!isNaN(localDate.getTime())) {
      // Use local date parts to avoid UTC shift
      const y = localDate.getFullYear();
      const m = String(localDate.getMonth() + 1).padStart(2, '0');
      const d = String(localDate.getDate()).padStart(2, '0');
      isoDate = `${y}-${m}-${d}`;
    }
    return { isoDate, durationNum: parseInt(match[2]) };
  }

  // fallback: try raw parse — use local date parts to avoid UTC shift
  const parsed = new Date(dateStr);
  if (!isNaN(parsed.getTime())) {
    const y = parsed.getFullYear();
    const m = String(parsed.getMonth() + 1).padStart(2, '0');
    const d = String(parsed.getDate()).padStart(2, '0');
    return { isoDate: `${y}-${m}-${d}`, durationNum: null };
  }
  return { isoDate: null, durationNum: null };
};

const parseGuests = (guestsText = '') => {
  // format: "Johnson Doe (2 adults)" or "2 adults"
  const fullMatch = guestsText.match(/^(.+?)\s*\((\d+)\s*adults?\)/i);
  if (fullMatch) {
    return { travelerName: fullMatch[1].trim(), travelerCount: parseInt(fullMatch[2]) };
  }
  const countOnlyMatch = guestsText.match(/^(\d+)\s*adults?/i);
  if (countOnlyMatch) {
    return { travelerName: '', travelerCount: parseInt(countOnlyMatch[1]) };
  }
  return { travelerName: guestsText, travelerCount: 1 };
};

const parsePrice = (priceStr = '$0') => {
  const num = parseInt(priceStr.replace(/[^0-9]/g, ''), 10);
  return isNaN(num) ? 0 : num;
};

function mapApiBookingToLocal(dto) {
  const { isoDate, durationNum } = parseDate(dto.tourDetails?.date);
  const { travelerName, travelerCount } = parseGuests(dto.tourDetails?.guests);
  const totalPrice = parsePrice(dto.tourDetails?.totalPrice);
  const state = (dto.state || 'BOOKED').toUpperCase();
  const status = state.toLowerCase();
  const tourIdMatch = (dto.tourImageUrl || '').match(/tours\/(t-\d+)\//);
  const tourId = tourIdMatch ? tourIdMatch[1] : null;
  const maxGuests = (tourId && MAX_GUESTS_MAP[tourId]) ?? dto.guestQuantity?.totalMaxValue ?? 8;
  const tourDates = (tourId && TOUR_DATES_MAP[tourId]) ?? [];
  const tourDurations = (tourId && TOUR_DURATIONS_MAP[tourId]) ?? [7];

  // Use personalDetails from API if available, otherwise fall back to parsing guests string
  let finalTravelerName = travelerName;
  let finalTravelerCount = travelerCount;
  
  if (dto.personalDetails && dto.personalDetails.length > 0) {
    // Build traveler name from personal details
    finalTravelerName = dto.personalDetails
      .map(p => `${p.firstName} ${p.lastName}`.trim())
      .join(', ');
    finalTravelerCount = dto.travelerCount || dto.personalDetails.length;
  }

  return {
    id: dto.id,
    tourId: dto.tourId || tourId,   // backend tour ID (e.g. "t-1001") for feedback endpoint
    name: dto.name,
    location: dto.destination,
    image: (tourId ? TOUR_IMAGE_MAP[tourId] : null) || TourImg1,
    status,
    startDate: isoDate,
    duration: durationNum,
    mealPlan: toMealLabel(dto.tourDetails?.mealPlan || ''),
    traveler: finalTravelerName,
    travelerCount: finalTravelerCount,
    childrenCount: dto.childrenCount ?? dto.tourDetails?.guests?.children ?? 0,
    totalPrice,
    documentsUploaded: Array.isArray(dto.documents) ? dto.documents.length : 0,
    agent: {
      name: dto.travelAgent?.name || '',
      email: dto.travelAgent?.email || '',
      phone: dto.travelAgent?.phone || '',
    },
    canceledBy: dto.cancellationDetails?.reason ? 'Travel Agent' : (dto.canceledBy ? 'Tourist' : null),
    cancelReason: dto.cancellationDetails?.reason
      ? (CANCELLATION_REASON_LABELS[dto.cancellationDetails.reason] || dto.cancellationDetails.reason)
      : '—',
    hasReview: dto.hasReview ?? (state === 'FINISHED'),
    canceledAfterStep: state === 'CANCELED' ? (dto.confirmation ? 1 : 0) : null,
    pendingAgentEdit: !!(dto.customerApproval && !dto.customerApproval.approvalGiven),
    maxGuests,
    tourDates,
    tourDurations,
    freeCancellationDate: (() => {
      if (isoDate && dto.tourDetails?.freeCancellationDaysBefore) {
        const [y, m, d] = isoDate.split('-').map(Number);
        const startDate = new Date(y, m - 1, d);
        const days = dto.tourDetails.freeCancellationDaysBefore;
        const freeCancelDate = new Date(startDate.getTime() - days * 24 * 60 * 60 * 1000);
        return `${freeCancelDate.getFullYear()}-${String(freeCancelDate.getMonth() + 1).padStart(2, '0')}-${String(freeCancelDate.getDate()).padStart(2, '0')}`;
      }
      return dto.freeCancellationDate || null;
    })(),
  };
}

/**
 * DELETE /api/v1/bookings/{bookingId}/documents/{documentId}
 * Deletes a single document from a booking.
 */
export const deleteBookingDocument = async (bookingId, documentId) => {
  await api.delete(`/bookings/${bookingId}/documents/${documentId}`);
};

/**
 * GET /api/v1/bookings/{bookingId}/documents
 * Retrieves existing documents for a booking.
 * Returns { payments: [{id, fileName}], guestDocuments: [{userName, documents: [{id, fileName}]}] }
 */
export const getBookingDocuments = async (bookingId) => {
  const response = await api.get(`/bookings/${bookingId}/documents`);
  return response.data;
};

/**
 * POST /api/v1/bookings/{bookingId}/documents
 * Uploads documents (passports + payment confirmation) for a booking.
 *
 * @param {string} bookingId
 * @param {{ payments: Array, guestDocuments: Array }} payload - shaped per UploadDocumentsRequestDTO
 */
export const uploadBookingDocuments = async (bookingId, payload) => {
  const response = await api.post(`/bookings/${bookingId}/documents`, payload);
  return response.data;
};

/**
 * PATCH /api/v1/bookings/{bookingId}/documents/{documentId}
 * Updates an existing document for a booking.
 *
 * @param {string} bookingId
 * @param {string} documentId
 * @param {{ fileName: string, type: string, base64encodedDocument: string }} payload
 */
export const updateBookingDocument = async (bookingId, documentId, payload) => {
  const response = await api.patch(`/bookings/${bookingId}/documents/${documentId}`, payload);
  return response.data;
};

export const approveBookingChange = async (bookingId) => {
  await api.patch(`/bookings/${bookingId}/approve`);
};

export const declineBookingChange = async (bookingId) => {
  await api.patch(`/bookings/${bookingId}/decline`);
};

export const getBooking = async (bookingId) => {
  const response = await api.get(`/bookings/${bookingId}`);
  return mapApiBookingToLocal(response.data);
};
