import TourImg1 from '../assets/images/TourImg1.png';
import TourImg2 from '../assets/images/TourImg2.png';
import TourImg3 from '../assets/images/TourImg3.png';
import TourImg4 from '../assets/images/TourImg4.png';
import TourImg5 from '../assets/images/TourImg5.png';
import TourImg6 from '../assets/images/TourImg6.png';

const TOUR_DATA = [
  {
    id: 't-1001',
    name: 'A guided hike in the Dolomites',
    destination: 'Dolomites, Italy',
    image: TourImg1,
    price: 1400,
    rating: 5.0,
    reviews: 19,
    durations: [7, 10, 12],
    mealPlans: ['Full-board (HB)'],
    tourType: ['Hikes'],
    location: 'Dolomites, Italy',
    dates: ['2026-08-05', '2026-09-10', '2026-10-08'],
    freeCancellationDaysBefore: 10,
    maxGuests: 4,
  },
  {
    id: 't-1002',
    name: 'Tropical Caribe',
    destination: 'Punta Cana, Dominican Republic',
    image: TourImg2,
    price: 1400,
    rating: 5.0,
    reviews: 19,
    durations: [7, 10, 12],
    mealPlans: ['Breakfast (BB)', 'Half-board (HB)', 'Full-board (HB)', 'All inclusive (AI)'],
    tourType: ['Resorts'],
    location: 'Punta Cana, Dominican Republic',
    dates: ['2026-08-08', '2026-09-05', '2026-10-12'],
    freeCancellationDaysBefore: 10,
    maxGuests: 2,
  },
  {
    id: 't-1003',
    name: 'Riverside Resort',
    destination: 'Ao Nang, Thailand',
    image: TourImg3,
    price: 1400,
    rating: 5.0,
    reviews: 19,
    durations: [7, 10, 12],
    mealPlans: ['Breakfast (BB)', 'Half-board (HB)', 'Full-board (HB)', 'All inclusive (AI)'],
    tourType: ['Resorts'],
    location: 'Ao Nang, Thailand',
    dates: ['2026-08-12', '2026-09-15', '2026-10-20'],
    freeCancellationDaysBefore: 10,
    maxGuests: 3,
  },
  {
    id: 't-1004',
    name: 'Costa Cruise',
    destination: 'Mediterranean Sea',
    image: TourImg4,
    price: 1400,
    rating: 5.0,
    reviews: 19,
    durations: [7, 10, 12],
    mealPlans: ['Full-board (HB)', 'All inclusive (AI)'],
    tourType: ['Cruises'],
    location: 'Mediterranean Sea',
    dates: ['2026-08-20', '2026-09-20', '2026-10-25'],
    freeCancellationDaysBefore: 10,
    maxGuests: 5,
  },
  {
    id: 't-1005',
    name: 'Amalfi Coast Drive',
    destination: 'Amalfi, Italy',
    image: TourImg5,
    price: 980,
    rating: 4.8,
    reviews: 291,
    durations: [3, 5],
    mealPlans: ['Breakfast', 'Half Board'],
    tourType: ['Resorts'],
    location: 'Campania Coast',
    dates: ['2026-05-19', '2026-06-10', '2026-07-08'],
    freeCancellationDaysBefore: 10,
    maxGuests: 2,
  },
  {
    id: 't-1006',
    name: 'Machu Picchu Explorer',
    destination: 'Cusco, Peru',
    image: TourImg6,
    price: 2100,
    rating: 4.9,
    reviews: 387,
    durations: [7, 10],
    mealPlans: ['Full Board'],
    tourType: ['Hikes'],
    location: 'Sacred Valley',
    dates: ['2026-05-19', '2026-06-02', '2026-06-22'],
    freeCancellationDaysBefore: 10,
    maxGuests: 6,
  },
  {
    id: 't-1007',
    name: 'Alpine Lakes Weekend',
    destination: 'Hallstatt, Austria',
    image: TourImg1,
    price: 890,
    rating: 4.7,
    reviews: 164,
    durations: [3, 5],
    mealPlans: ['Breakfast (BB)', 'Half-board (HB)'],
    tourType: ['Hikes'],
    location: 'Salzkammergut Region',
    dates: ['2026-06-05', '2026-06-16', '2026-07-02'],
    freeCancellationDaysBefore: 10,
    maxGuests: 4,
  },
  {
    id: 't-1008',
    name: 'Coral Bay Escape',
    destination: 'Boracay, Philippines',
    image: TourImg2,
    price: 1520,
    rating: 4.9,
    reviews: 228,
    durations: [5, 7, 10],
    mealPlans: ['Breakfast (BB)', 'All inclusive (AI)'],
    tourType: ['Resorts'],
    location: 'White Beach',
    dates: ['2026-06-08', '2026-06-21', '2026-07-10'],
    freeCancellationDaysBefore: 10,
    maxGuests: 3,
  },
  {
    id: 't-1009',
    name: 'Rainforest River Lodge',
    destination: 'Ubud, Indonesia',
    image: TourImg3,
    price: 1180,
    rating: 4.6,
    reviews: 203,
    durations: [4, 6, 8],
    mealPlans: ['Breakfast (BB)', 'Half-board (HB)'],
    tourType: ['Resorts'],
    location: 'Ayung Valley',
    dates: ['2026-06-11', '2026-06-24', '2026-07-14'],
    freeCancellationDaysBefore: 10,
    maxGuests: 4,
  },
  {
    id: 't-1010',
    name: 'Adriatic Sunset Cruise',
    destination: 'Dubrovnik, Croatia',
    image: TourImg4,
    price: 1790,
    rating: 4.8,
    reviews: 312,
    durations: [5, 7],
    mealPlans: ['Half-board (HB)', 'Full-board (HB)'],
    tourType: ['Cruises'],
    location: 'Adriatic Coast',
    dates: ['2026-06-13', '2026-06-30', '2026-07-18'],
    freeCancellationDaysBefore: 10,
    maxGuests: 5,
  },
  {
    id: 't-1011',
    name: 'Tuscan Countryside Drive',
    destination: 'Siena, Italy',
    image: TourImg5,
    price: 1040,
    rating: 4.7,
    reviews: 187,
    durations: [3, 5, 7],
    mealPlans: ['Breakfast', 'Half Board'],
    tourType: ['Resorts'],
    location: 'Chianti Hills',
    dates: ['2026-06-07', '2026-06-19', '2026-07-05'],
    freeCancellationDaysBefore: 10,
    maxGuests: 2,
  },
  {
    id: 't-1012',
    name: 'Sacred Valley Trek Plus',
    destination: 'Ollantaytambo, Peru',
    image: TourImg6,
    price: 2240,
    rating: 4.9,
    reviews: 275,
    durations: [7, 10, 12],
    mealPlans: ['Full Board'],
    tourType: ['Hikes'],
    location: 'Andes Ridge Route',
    dates: ['2026-06-15', '2026-07-03', '2026-07-22'],
    freeCancellationDaysBefore: 10,
    maxGuests: 6,
  },
  {
    id: 't-1013',
    name: 'Dolomite Panorama Camp',
    destination: 'Cortina d Ampezzo, Italy',
    image: TourImg1,
    price: 1320,
    rating: 4.8,
    reviews: 211,
    durations: [5, 7, 9],
    mealPlans: ['Half-board (HB)', 'Full-board (HB)'],
    tourType: ['Hikes'],
    location: 'Ampezzo Valley',
    dates: ['2026-06-18', '2026-07-08', '2026-07-26'],
    freeCancellationDaysBefore: 10,
    maxGuests: 4,
  },
  {
    id: 't-1014',
    name: 'Caribbean Family Resort',
    destination: 'Bayahibe, Dominican Republic',
    image: TourImg2,
    price: 1680,
    rating: 4.6,
    reviews: 198,
    durations: [5, 7, 10],
    mealPlans: ['Breakfast (BB)', 'All inclusive (AI)'],
    tourType: ['Resorts'],
    location: 'La Romana Coast',
    dates: ['2026-06-22', '2026-07-12', '2026-07-30'],
    freeCancellationDaysBefore: 10,
    maxGuests: 5,
  },
];

const matchAny = (current = [], selected = []) =>
  !selected.length || selected.some((v) => current.includes(v));

const normalizeKey = (value = '') => value.toLowerCase().replace(/[^a-z]/g, '');

const normalizeMeal = (value = '') => {
  const key = normalizeKey(value);
  if (key.includes('breakfast')) return 'breakfast';
  if (key.includes('halfboard')) return 'halfboard';
  if (key.includes('fullboard')) return 'fullboard';
  if (key.includes('allinclusive')) return 'allinclusive';
  return key;
};

const normalizeTourType = (value = '') => {
  const key = normalizeKey(value);
  if (['resort', 'resorts'].includes(key)) return 'resorts';
  if (['cruise', 'cruises'].includes(key)) return 'cruises';
  if (['hike', 'hikes'].includes(key)) return 'hikes';
  return key;
};

const matchAnyNormalized = (current = [], selected = [], normalizer) => {
  if (!selected.length) return true;
  const currentNormalized = current.map(normalizer);
  const selectedNormalized = selected.map(normalizer);
  return selectedNormalized.some((value) => currentNormalized.includes(value));
};

const normalizeDate = (v) => {
  if (!v) return null;
  if (typeof v === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(v)) return v;

  const date = new Date(v);
  if (Number.isNaN(date.getTime())) return null;

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};
const normalizeDates = (values = []) => values.map(normalizeDate).filter(Boolean);
const isWithinRange = (value, start, end) => value >= start && value <= end;

export { TOUR_DATA };

export const getDestinations = async (query = '') => {
  const q = query.toLowerCase().trim();
  const list = [...new Set(TOUR_DATA.map((t) => t.destination))]
    .filter((d) => d.toLowerCase().includes(q))
    .map((d) => ({ label: d, value: d }));
  return new Promise((res) => setTimeout(() => res(list), 180));
};

export const getTours = async (filters = {}) => {
  const {
    destination,
    startDates = [],
    startDate,
    tourists = { adults: 2, children: 0 },
    durations = [],
    mealPlans = [],
    tourTypes = [],
    sortBy,
  } = filters;
  const totalTravelers = (tourists?.adults || 0) + (tourists?.children || 0);
  const dates = normalizeDates(startDates.length ? startDates : startDate ? [startDate] : []);

  let result = TOUR_DATA.filter((t) => {
    if (destination && t.destination !== destination) return false;
    if (dates.length === 1 && !t.dates.includes(dates[0])) return false;
    if (dates.length === 2 && !t.dates.some((date) => isWithinRange(date, dates[0], dates[1]))) return false;
    if (totalTravelers > (t.maxGuests || 0)) return false;
    if (!matchAny(t.durations, durations)) return false;
    if (!matchAnyNormalized(t.mealPlans, mealPlans, normalizeMeal)) return false;
    if (!matchAnyNormalized(t.tourType, tourTypes, normalizeTourType)) return false;
    return true;
  });

  if (sortBy === 'price-low-high') {
    result = [...result].sort((a, b) =>
      a.price !== b.price ? a.price - b.price : b.rating - a.rating
    );
  } else if (sortBy === 'price-high-low') {
    result = [...result].sort((a, b) =>
      a.price !== b.price ? b.price - a.price : b.rating - a.rating
    );
  } else {
    result = [...result].sort((a, b) =>
      b.rating !== a.rating ? b.rating - a.rating : b.reviews - a.reviews
    );
  }

  return new Promise((res) => setTimeout(() => res(result), 300));
};

/**
 * Get a single tour by ID
 * @param {string} tourId - The tour ID
 * @returns {Promise<Object|null>} - Tour data or null if not found
 */
export const getTourById = async (tourId) => {
  const tour = TOUR_DATA.find((t) => t.id === tourId);
  return new Promise((res) => setTimeout(() => res(tour || null), 200));
};

export const MAX_GUESTS_MAP = Object.fromEntries(
  TOUR_DATA.map((t) => [t.id, t.maxGuests ?? 8])
);

export const TOUR_DATES_MAP = Object.fromEntries(
  TOUR_DATA.map((t) => [t.id, t.dates ?? []])
);

export const TOUR_DURATIONS_MAP = Object.fromEntries(
  TOUR_DATA.map((t) => [t.id, t.durations ?? [7]])
);
