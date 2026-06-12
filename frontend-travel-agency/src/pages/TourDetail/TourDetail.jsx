// TourDetail Component - Shows detailed information about a specific tour
// Dynamically fetches tour data based on route parameter (:tourId)

import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import styles from './TourDetail.module.css';
import Button from '../../components/Button/Button';
import { getTourById } from '../../services/travelService';
import { ROUTES } from '../../config/routes';
import RightArrow from '../../assets/icons/rightarrow.svg';
import LocationIcon from '../../assets/icons/Location.svg';
import StarIcon from '../../assets/icons/star.svg';
import DoubleRightIcon from '../../assets/icons/double-right.svg';
import Img1 from '../../assets/images/Img1.jpg';
import Img2 from '../../assets/images/Img2.jpg';
import Img3 from '../../assets/images/Img3.jpg';
import Img4 from '../../assets/images/Img4.jpg';
import Img5 from '../../assets/images/Img5.jpg';
import Img6 from '../../assets/images/Img6.jpg';
import Img7 from '../../assets/images/Img7.jpg';
import Avatar1 from '../../assets/images/Avatar1.png';
import Avatar2 from '../../assets/images/Avatar2.png';
import Avatar3 from '../../assets/images/Avatar3.png';
import Avatar4 from '../../assets/images/Avatar4.png';
// Enhanced booking dropdown components
import TouristCounter from '../../components/Tours/TouristCounter';
import MealPlanSelector from '../../components/Tours/MealPlanSelector';
import BookingModal from '../../components/Tours/BookingModal';
import ReservationConfirmationModal from '../../components/Tours/ReservationConfirmationModal';
import { useAuth } from '../../context/AuthContext';
import travelbagIcon from '../../assets/icons/travelbag.svg';
import crossIcon from '../../assets/icons/Cross.svg';
import { createBooking } from '../../services/bookingService';
import { getTourFeedbacks } from '../../services/feedbackService';
import { parseCalendarDate, formatCalendarDate } from '../../utils/dateUtils';

const parseLocalDate = parseCalendarDate;

const fmt = (iso) => formatCalendarDate(iso, { month: 'short', day: 'numeric' });

const calculateCancellationStatus = (tour) => {
  if (tour.cancellationText && !tour.freeCancellationDaysBefore) {
    return { canCancel: tour.cancellationPolicy === 'available', text: tour.cancellationText };
  }
  const freeCancellationDate = tour.freeCancellationDate
    ? parseLocalDate(tour.freeCancellationDate)
    : tour.freeCancellationDaysBefore && tour.dates?.[0]
      ? new Date(parseLocalDate(tour.dates[0]).getTime() - tour.freeCancellationDaysBefore * 24 * 60 * 60 * 1000)
      : null;
  if (!freeCancellationDate) {
    return { canCancel: false, text: 'Free cancellation is no longer available' };
  }
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  freeCancellationDate.setHours(0, 0, 0, 0);
  const canCancel = today < freeCancellationDate;
  return {
    canCancel,
    text: canCancel ? `Free cancellation until ${fmt(freeCancellationDate)}` : 'Free cancellation is no longer available',
  };
};

const TOUR_DETAIL_CONTENT = {
  't-1001': {
    description: 'Experience the excitement among the wonders of the Brenta Dolomites! Unforgettable excursions, breathtaking views, adventure in a paradise.',
    accommodation: 'During your hike in the Dolomites, you will be accommodated in cozy mountain huts. The tour package includes stays in shared dormitory-style rooms. Solo travelers will be paired with another traveler of the same gender, as indicated by the gender marker on their passports.',
    mealPlansText: 'Full-board (HB).',
    tourGuide: 'Italian, English.',
    transfer: 'Organized transfer from Milan airport.',
  },
  't-1002': {
    description: 'Recharge in the Caribbean sun with turquoise beaches, local culture, and relaxed island days designed for comfort and fun.',
    accommodation: 'You will stay at a beachfront resort in Punta Cana with private bathrooms, pool access, and daily housekeeping throughout the tour.',
    mealPlansText: 'Breakfast (BB), Half-board (HB), Full-board (HB), All inclusive (AI).',
    tourGuide: 'English, Spanish.',
    transfer: 'Round-trip airport transfer from Punta Cana International Airport.',
  },
  't-1003': {
    description: 'Discover jungle trails, limestone cliffs, and peaceful riverside scenery in a tropical Thailand getaway made for nature lovers.',
    accommodation: 'Accommodation is at a riverside resort in Ao Nang with shared and private options, air-conditioned rooms, and mountain views.',
    mealPlansText: 'Breakfast (BB), Half-board (HB), Full-board (HB), All inclusive (AI).',
    tourGuide: 'English, Thai.',
    transfer: 'Organized transfer from Krabi Airport and local shuttle service.',
  },
  't-1004': {
    description: 'Sail across the Mediterranean with curated coastal stops, cultural city walks, and sunset decks for a classic cruise experience.',
    accommodation: 'You will stay in twin-share cruise cabins with ensuite bathroom, onboard entertainment, and full ship amenities.',
    mealPlansText: 'Full-board (HB), All inclusive (AI).',
    tourGuide: 'English, Italian.',
    transfer: 'Port transfer support from major embarkation points is included.',
  },
  't-1005': {
    description: 'Drive the Amalfi coast road with scenic viewpoints, colorful villages, and authentic southern Italian food experiences.',
    accommodation: 'Accommodation includes boutique guesthouses in Amalfi with private rooms and breakfast service.',
    mealPlansText: 'Breakfast, Half Board.',
    tourGuide: 'English, Italian.',
    transfer: 'Organized transfer from Naples airport and intercity route transfers.',
  },
  't-1006': {
    description: 'Trek through Peru’s Sacred Valley and reach Machu Picchu with expert support, acclimatized pacing, and mountain panoramas.',
    accommodation: 'You will stay in trekking lodges and mountain camps with shared facilities and essential comfort services.',
    mealPlansText: 'Full Board.',
    tourGuide: 'English, Spanish.',
    transfer: 'Airport transfer from Cusco and rail transfer to the Machu Picchu region.',
  },
  't-1007': {
    description: 'Enjoy a short alpine break among Austria’s lake villages with calm trails, photography spots, and local cuisine.',
    accommodation: 'Accommodation is in family-run alpine hotels with twin-share rooms and mountain lake access.',
    mealPlansText: 'Breakfast (BB), Half-board (HB).',
    tourGuide: 'English, German.',
    transfer: 'Regional transfer from Salzburg Airport and local shuttle rides.',
  },
  't-1008': {
    description: 'Escape to white-sand beaches and coral bays with island hopping, water activities, and sunset relaxation.',
    accommodation: 'You will stay at a coastal resort on White Beach with private room options and beach access.',
    mealPlansText: 'Breakfast (BB), All inclusive (AI).',
    tourGuide: 'English, Filipino.',
    transfer: 'Organized transfer from Caticlan Airport with boat connection.',
  },
  't-1009': {
    description: 'Unwind in a rainforest lodge experience with river walks, wellness mornings, and cultural village visits in Ubud.',
    accommodation: 'Accommodation includes eco-lodge rooms in Ayung Valley with jungle views and daily housekeeping.',
    mealPlansText: 'Breakfast (BB), Half-board (HB).',
    tourGuide: 'English, Indonesian.',
    transfer: 'Airport transfer from Ngurah Rai and all internal route transfers.',
  },
  't-1010': {
    description: 'Cruise along the Adriatic coastline with old-town ports, island stops, and evening sea-view dining.',
    accommodation: 'You will stay in modern cruise cabins with private bathrooms and shared deck facilities.',
    mealPlansText: 'Half-board (HB), Full-board (HB).',
    tourGuide: 'English, Croatian.',
    transfer: 'Organized transfer between Dubrovnik airport and departure port.',
  },
  't-1011': {
    description: 'Explore Tuscany by road through vineyards, medieval towns, and countryside routes designed for relaxed discovery.',
    accommodation: 'Accommodation includes countryside inns in the Chianti area with private rooms and breakfast options.',
    mealPlansText: 'Breakfast, Half Board.',
    tourGuide: 'English, Italian.',
    transfer: 'Transfer support from Florence airport and route shuttles.',
  },
  't-1012': {
    description: 'Challenge yourself on high-altitude Andean routes with panoramic ridgelines, cultural sites, and guided trek logistics.',
    accommodation: 'Accommodation includes trek camps and mountain lodges with shared facilities and guided support staff.',
    mealPlansText: 'Full Board.',
    tourGuide: 'English, Spanish.',
    transfer: 'Cusco airport transfer and all trek route connections included.',
  },
  't-1013': {
    description: 'Camp and hike through Dolomite panoramas with ridge trails, glacier viewpoints, and alpine village evenings.',
    accommodation: 'You will stay in mountain camps and alpine huts with shared sleeping quarters and warm meal service.',
    mealPlansText: 'Half-board (HB), Full-board (HB).',
    tourGuide: 'Italian, English.',
    transfer: 'Organized transfer from Venice airport to Ampezzo Valley.',
  },
  't-1014': {
    description: 'A family-friendly Caribbean resort trip with calm beaches, kid-friendly activities, and relaxed all-day schedules.',
    accommodation: 'Accommodation includes family resort rooms near La Romana Coast with pool and beach access.',
    mealPlansText: 'Breakfast (BB), All inclusive (AI).',
    tourGuide: 'English, Spanish.',
    transfer: 'Organized transfer from Santo Domingo/Punta Cana airports based on arrival.',
  },
};

const fmtDate = (iso) => formatCalendarDate(iso, { month: 'short', day: 'numeric' });

const SimpleDateDurationDropdown = ({ dates, durations, selectedDate, selectedDuration, onChange }) => {
  const [isOpen, setIsOpen] = React.useState(false);
  const ref = React.useRef(null);

  React.useEffect(() => {
    const handler = (e) => { if (ref.current && !ref.current.contains(e.target)) setIsOpen(false); };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const options = dates.flatMap((date) =>
    durations.map((dur) => ({ date, duration: dur, label: `${fmtDate(date)}, ${dur} days` }))
  );

  const selectedLabel = selectedDate
    ? `${fmtDate(selectedDate)}, ${selectedDuration} days`
    : 'Select date & duration';

  return (
    <div style={{ position: 'relative', width: '100%' }} ref={ref}>
      <button
        type="button"
        onClick={() => setIsOpen((p) => !p)}
        style={{
          display: 'flex', alignItems: 'center', gap: '8px',
          width: '100%', height: '56px', padding: '0 12px',
          border: `1px solid ${isOpen ? '#027EAC' : '#D3E1ED'}`, borderRadius: '8px',
          background: 'white', cursor: 'pointer', textAlign: 'left',
        }}
      >
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ flexShrink: 0 }}>
          <rect x="3" y="4" width="18" height="17" rx="2" stroke="#0B3857" strokeWidth="1.75"/>
          <path d="M3 9h18" stroke="#0B3857" strokeWidth="1.75"/>
          <path d="M8 2v4M16 2v4" stroke="#0B3857" strokeWidth="1.75" strokeLinecap="round"/>
        </svg>
        <span style={{ flex: 1, fontFamily: 'Nunito', fontSize: '14px', color: '#0B3857', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {selectedLabel}
        </span>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"
          style={{ flexShrink: 0, transform: isOpen ? 'rotate(180deg)' : 'none', transition: 'transform 0.2s' }}>
          <path d="M6 9l6 6 6-6" stroke="#0B3857" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
      </button>

      {isOpen && (
        <div style={{
          position: 'absolute', top: 'calc(100% + 8px)', left: 0, right: 0, zIndex: 20,
          background: 'white', border: '1px solid #D3E1ED', borderRadius: '8px',
          boxShadow: '0px 8px 20px rgba(11,56,87,0.12)', overflow: 'hidden',
        }}>
          {options.map((opt) => {
            const active = opt.date === selectedDate && opt.duration === selectedDuration;
            return (
              <button
                key={opt.label}
                type="button"
                onClick={() => { onChange({ date: opt.date, duration: opt.duration }); setIsOpen(false); }}
                style={{
                  display: 'block', width: '100%', padding: '10px 12px', textAlign: 'left',
                  fontFamily: 'Nunito', fontSize: '14px', lineHeight: '24px', border: 'none', cursor: 'pointer',
                  background: active ? '#E7F9FF' : 'white', color: active ? '#027EAC' : '#0B3857',
                }}
              >
                {opt.label}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
};

const TourDetail = () => {
  const { tourId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [tour, setTour] = useState(null);
  const [loading, setLoading] = useState(true);

  // Enhanced booking state
  const [selectedDate, setSelectedDate] = useState('');
  const [selectedDurations, setSelectedDurations] = useState([]);
  const [tourists, setTourists] = useState({ adults: 1, children: 0 });
  const [selectedMealPlans, setSelectedMealPlans] = useState([]);

  const [sortReviews, setSortReviews] = useState('topRated');
  const [showSortDropdown, setShowSortDropdown] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const REVIEWS_PER_PAGE = 4;
  const [apiReviews, setApiReviews] = useState([]);

  // Booking modal state
  const [bookingTour, setBookingTour] = useState(null);
  const [reservationBooking, setReservationBooking] = useState(null);
  const [showAuthModal, setShowAuthModal] = useState(false);
  const [duplicateBookingMsg, setDuplicateBookingMsg] = useState(null);

  // Handle "Book the tour" click - opens booking form modal
  const handleBookTour = () => {
    if (!user) { setShowAuthModal(true); return; }
    setBookingTour(tour);
  };

  // Fetch tour data by ID from route params
  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' });

    let isMounted = true;

    const fetchTour = async () => {
      setLoading(true);
      try {
        const tourData = await getTourById(tourId);
        if (isMounted) {
          if (!tourData) {
            // Tour not found, redirect to tours page
            navigate(ROUTES.TOURS, { replace: true });
          } else {
            setTour(tourData);
            // Set default values from tour data
            if (tourData.dates?.length > 0) {
              setSelectedDate(tourData.dates[0]);
            }
            if (tourData.durations?.length > 0) {
              // Set first duration as default (7, 10, or 12 days)
              setSelectedDurations([tourData.durations[0]]);
            }
            // Set default meal plan to first available standard option
            if (tourData.mealPlans?.length > 0) {
              setSelectedMealPlans([tourData.mealPlans[0]]);
            }
          }
        }
      } catch (error) {
        console.error('Error fetching tour:', error);
        if (isMounted) {
          navigate(ROUTES.TOURS, { replace: true });
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
        };
    fetchTour();

    return () => {
      isMounted = false;
    };
  }, [tourId, navigate]);

  // Fetch real reviews from the backend and merge with hardcoded ones
  useEffect(() => {
    if (!tourId) return;
    getTourFeedbacks(tourId)
      .then((data) => {
        const avatars = [Avatar1, Avatar2, Avatar3, Avatar4];
        const fetched = (data?.reviews || []).map((r, idx) => ({
          id: `api-${r.id || idx}`,
          name: r.authorName || 'Traveler',
          date: r.createdAt ? new Date(r.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : '',
          rating: r.rate ?? r.rating ?? 5,
          text: r.reviewContent || r.comment || '',
          avatar: r.authorImageUrl || avatars[idx % 4],
        }));
        setApiReviews(fetched);
      })
      .catch(() => { /* silently fall back to hardcoded only */ });
  }, [tourId]);

  // Mock reviews data (hardcoded, always shown)
  const hardcodedReviews = [
    {
      id: 1,
      name: 'David',
      date: 'Aug 3, 2024',
      rating: 5,
      text: 'Incredible experience! The trails were cozy, and hiking the Brenta Dolomites was beyond breathtaking. Highly recommended for adventurous souls!',
      avatar: Avatar1,
    },
    {
      id: 2,
      name: 'Giorgi',
      date: 'Jul 4, 2024',
      rating: 5,
      text: 'Seamless adventure from start to finish! The Dolomites offered stunning views, and the cozy mountain huts added a rustic charm. Hiking was phenomenal, enhanced by knowledgeable, friendly guides.',
      avatar: Avatar2,
    },
    {
      id: 3,
      name: 'Camila',
      date: 'Jul 4, 2024',
      rating: 3,
      text: 'Good hiking experience, yet the small group size made social interactions limited. I enjoyed the trails but hoped for a bit more variety in daily activities. Worth going, but room for improvement!',
      avatar: Avatar3,
    },
    {
      id: 4,
      name: 'Anna',
      date: 'Jun 29, 2024',
      rating: 5,
      text: 'An exceptional week with perfectly chosen trails leading to incredible views. Staying in the mountain huts truly enriched the experience, and the meals provided a delightful taste of local cuisine.',
      avatar: Avatar4,
    },
    {
      id: 5,
      name: 'Marco',
      date: 'Jun 15, 2024',
      rating: 4,
      text: 'Beautiful landscapes and well-organized itinerary. The guide was very knowledgeable and made the whole trip feel safe and enjoyable. Would definitely book again.',
      avatar: Avatar1,
    },
    {
      id: 6,
      name: 'Sophie',
      date: 'May 28, 2024',
      rating: 5,
      text: 'Absolutely magical! Every detail was taken care of. The sunsets from the mountain peaks were unlike anything I have ever seen. A life-changing journey.',
      avatar: Avatar2,
    },
    {
      id: 7,
      name: 'Luca',
      date: 'May 10, 2024',
      rating: 2,
      text: 'The scenery was stunning but the logistics were a bit disorganized. We had to wait quite a bit between activities. Needs improvement on time management.',
      avatar: Avatar3,
    },
    {
      id: 8,
      name: 'Elena',
      date: 'Apr 22, 2024',
      rating: 5,
      text: 'One of the best trips of my life. The combination of nature, culture, and food was perfect. I loved the small group setting — felt very personal and comfortable.',
      avatar: Avatar4,
    },
    {
      id: 9,
      name: 'James',
      date: 'Apr 5, 2024',
      rating: 4,
      text: 'Great value for the price. The accommodation was cozy, food was delicious, and the hiking routes were well-planned. Minor issues with transport but overall very satisfied.',
      avatar: Avatar1,
    },
    {
      id: 10,
      name: 'Nina',
      date: 'Mar 18, 2024',
      rating: 5,
      text: 'Truly unforgettable. The tour exceeded all my expectations. Every stop had something special to offer and our guide had stories for every trail. Highly recommended!',
      avatar: Avatar2,
    },
    {
      id: 11,
      name: 'Carlos',
      date: 'Mar 3, 2024',
      rating: 3,
      text: 'Decent experience overall. The natural beauty is undeniable but some activities felt rushed. More free time at certain stops would have made it much better.',
      avatar: Avatar3,
    },
    {
      id: 12,
      name: 'Yuki',
      date: 'Feb 14, 2024',
      rating: 5,
      text: 'I came alone and left with new friends. The group atmosphere was warm and welcoming. The views, the food, the guides — everything was top notch. Will return next year!',
      avatar: Avatar4,
    },
  ];

  // Merge hardcoded + real API reviews (real ones appear after hardcoded)
  const reviews = [...hardcodedReviews, ...apiReviews];

  const sortOptions = [
    { value: 'topRated', label: 'Top rated first' },
    { value: 'lowRated', label: 'Low rated first' },
    { value: 'newest', label: 'Newest first' },
    { value: 'oldest', label: 'Oldest first' },
  ];

  const sortedReviews = [...reviews].sort((a, b) => {
    if (sortReviews === 'topRated') return b.rating - a.rating;
    if (sortReviews === 'lowRated') return a.rating - b.rating;
    if (sortReviews === 'newest') return new Date(b.date) - new Date(a.date);
    if (sortReviews === 'oldest') return new Date(a.date) - new Date(b.date);
    return 0;
  });

  const totalPages = Math.ceil(sortedReviews.length / REVIEWS_PER_PAGE);
  const pagedReviews = sortedReviews.slice(
    (currentPage - 1) * REVIEWS_PER_PAGE,
    currentPage * REVIEWS_PER_PAGE
  );

  const renderStars = (rating) => {
    return Array.from({ length: 5 }, (_, i) => (
      <span key={i} className={styles.starBox}>
        <img
          src={StarIcon}
          alt=""
          className={i < rating ? styles.starFilled : styles.starEmpty}
        />
      </span>
    ));
  };

  // Format dates for display
  // const formatDate = (dateStr) => {
  // const date = new Date(dateStr);
  //   return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  // };

  // Loading state
  if (loading) {
    return (
      <div className={styles.tourDetailPage}>
        <div className={styles.container}>
          <div style={{ textAlign: 'center', padding: '60px 20px' }}>
            <p className="font-nunito text-[18px] text-[#677883]">Loading tour details...</p>
          </div>
        </div>
      </div>
    );
  }

  // Tour not found (shouldn't happen due to redirect, but just in case)
  if (!tour) {
    return null;
  }

  const tourContent = TOUR_DETAIL_CONTENT[tour.id] || {
    description: `Experience the excitement among the wonders of ${tour.destination}! Unforgettable excursions, breathtaking views, adventure in a paradise.`,
    accommodation: `During your trip to ${tour.destination}, you will be accommodated in carefully selected properties. The tour package includes comfortable stays with all necessary amenities.`,
    mealPlansText: Array.isArray(tour.mealPlans) ? tour.mealPlans.join(', ') : 'Meal plan details available on request.',
    tourGuide: 'English.',
    transfer: `Organized transfer from nearest airport to ${tour.location}.`,
  };

  const { text: cancellationDetails } = calculateCancellationStatus(tour);

  const imageGallery = [Img1, Img2, Img3, Img4, Img5, Img6, Img7];

  return (
    <div className={styles.tourDetailPage}>
      {/* Main Content Container */}
      <div className={styles.container}>
        {/* Breadcrumb Navigation */}
        <div className={styles.breadcrumb}>
          <Link to={ROUTES.TOURS} className={styles.breadcrumbLink}>Main page</Link>
          <img src={RightArrow} alt="separator" className={styles.breadcrumbSeparator} />
          <span className={styles.breadcrumbCurrent}>{tour.name}</span>
        </div>

        {/* Tour Header: Title, Location, Rating */}
        <div className={styles.tourHeader}>
          <div className={styles.tourTitleSection}>
            <h1 className={styles.tourTitle}>{tour.name}</h1>
            <div className={styles.tourLocation}>
              <img src={LocationIcon} alt="location" className={styles.locationIcon} />
              <span>{tour.location}</span>
            </div>
          </div>
          <div className={styles.tourRating}>
            <div className={styles.starIconBox}>
              <img src={StarIcon} alt="rating star" className={styles.starIcon} />
            </div>
            <span className={styles.ratingValue}>{tour.rating.toFixed(2)}</span>
          </div>
        </div>

        {/* Image Gallery: 8 images in 4x2 grid */}
        <div className={styles.imageGallery}>
          {imageGallery.map((image, index) => (
            <div key={index} className={`${styles.galleryItem} ${styles[`galleryItem${index + 1}`]}`}>
              {/* PLACEHOLDER: Add 8 unique tour images to /public/assets/images/ */}
              <img src={image} alt={`${tour.name} view ${index + 1}`} />
            </div>
          ))}
        </div>

        {/* Two-column layout: Tour Details + Booking Panel */}
        <div className={styles.contentWrapper}>
          {/* LEFT COLUMN - Tour Details */}
          <div className={styles.tourDetails}>
            <div className={styles.tourDescription}>
              <p>
                {tourContent.description}
              </p>
            </div>

            <div className={styles.aboutSection}>
              <h2 className={styles.sectionTitle}>About the tour</h2>

              <div className={styles.aboutDetailsFrame}>

              {/* Cancellation Policy */}
              <div className={styles.detailItem}>
                <h3 className={styles.detailTitle}>Free cancellation policy</h3>
                <p className={styles.detailText}>{cancellationDetails}</p>
              </div>

              {/* Duration Options */}
              <div className={styles.detailItem}>
                <h3 className={styles.detailTitle}>
                  Duration - {tour.durations.map(d => `${d} days`).join(', ')}
                </h3>
              </div>

              {/* Accommodation Details */}
              <div className={styles.detailItem}>
                <h3 className={styles.detailTitle}>Accomodation</h3>
                <p className={styles.detailText}>
                  {tourContent.accommodation}
                </p>
              </div>

              {/* Meal Plans */}
              <div className={styles.detailItem}>
                <h3 className={styles.detailTitle}>Meal plans</h3>
                <p className={styles.detailText}>{tourContent.mealPlansText}</p>
              </div>

              {/* Tour Guide */}
              <div className={styles.detailItem}>
                <h3 className={styles.detailTitle}>Tourguide</h3>
                <p className={styles.detailText}>{tourContent.tourGuide}</p>
              </div>

              {/* Transfer Info */}
              <div className={styles.detailItem}>
                <h3 className={styles.detailTitle}>Transfer</h3>
                <p className={styles.detailText}>{tourContent.transfer}</p>
              </div>

              {/* Group Size */}
              <div className={styles.detailItem}>
                <h3 className={styles.detailTitle}>Small group</h3>
                <p className={styles.detailText}>Maximum number of participants: {tour.maxGuests}.</p>
              </div>

              </div>
            </div>
          </div>

          {/* RIGHT COLUMN - Booking Panel */}
          <div className={styles.bookingPanel}>
            <div className={styles.bookingCard}>
              <div className={styles.bookingFilters}>
                {/* Date & Duration Picker - Simple dropdown */}
                <div className={styles.bookingOption}>
                  <SimpleDateDurationDropdown
                    dates={tour.dates || []}
                    durations={tour.durations || [7]}
                    selectedDate={selectedDate}
                    selectedDuration={selectedDurations[0] ?? (tour.durations?.[0] || 7)}
                    onChange={({ date, duration }) => {
                      setSelectedDate(date);
                      setSelectedDurations([duration]);
                    }}
                  />
                </div>

                {/* Adults & Children Counter - NEW ENHANCED DROPDOWN */}
                <div className={styles.bookingOption}>
                  <TouristCounter
                    adults={tourists.adults}
                    children={tourists.children}
                    maxAdults={tour.guestQuantity?.adultsMaxValue ?? tour.maxGuests}
                    maxTotal={tour.guestQuantity?.totalMaxValue ?? tour.maxGuests}
                    onChange={setTourists}
                  />
                </div>

                {/* Meal Plan Selector - NEW ENHANCED DROPDOWN */}
                <div className={styles.bookingOption}>
                  <MealPlanSelector
                    availableMealPlans={tour.mealPlans?.map((label) => ({ value: label, label })) ?? []}
                    selectedMealPlans={selectedMealPlans}
                    onChange={setSelectedMealPlans}
                  />
                </div>

                {/* Total Price (Dynamic based on number of adults + children) */}
                <div className={styles.totalPrice}>
                  <span className={styles.priceLabel}>Total price:</span>
                  <span className={styles.priceValue}>${tour.price * (tourists.adults + tourists.children)}</span>
                </div>
              </div>

              {/* Book Button */}
              <Button className={styles.bookButton} onClick={handleBookTour}>Book the tour</Button>
            </div>
          </div>
        </div>

        {/* Customer Reviews Section */}
        <div className={styles.reviewsSection}>
          <div className={styles.reviewsHeader}>
            <h2 className={styles.reviewsTitle}>Customer Reviews</h2>

            <div className={styles.sortContainer}>
              <span className={styles.sortLabel}>Sort by:</span>
              <div className={styles.sortMenu}>
                <button
                  className={styles.sortButton}
                  onClick={() => setShowSortDropdown(!showSortDropdown)}
                >
                  <span className={styles.sortValue}>
                    {sortOptions.find(opt => opt.value === sortReviews)?.label}
                  </span>
                  <span className={styles.sortIcon} aria-hidden="true">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M6 9L12 15L18 9" stroke="#027EAC" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  </span>
                </button>
                {showSortDropdown && (
                  <div className={styles.sortDropdown}>
                    {sortOptions.map((option) => (
                      <button
                        key={option.value}
                        className={`${styles.sortOption} ${sortReviews === option.value ? styles.sortOptionActive : ''}`}
                        onClick={() => {
                          setSortReviews(option.value);
                          setShowSortDropdown(false);
                          setCurrentPage(1);
                        }}
                      >
                        {option.label}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>

          <div className={styles.reviewsContent}>
            {/* Reviews Grid (4 columns) */}
            <div className={styles.reviewsGrid}>
              {pagedReviews.map((review) => (
                <div key={review.id} className={styles.reviewCard}>
                  <div className={styles.reviewHeadline}>
                    <div className={styles.reviewHeader}>
                      <img src={review.avatar} alt={review.name} className={styles.avatar} />
                      <div className={styles.reviewHeadlineGroup}>
                        <div className={styles.reviewNameStars}>
                          <span className={styles.reviewerName}>{review.name}</span>
                          <div className={styles.reviewStars}>
                            {renderStars(review.rating)}
                          </div>
                        </div>
                        <div className={styles.reviewDate}>{review.date}</div>
                      </div>
                    </div>
                  </div>
                  <div className={styles.reviewBody}>
                    <p className={styles.reviewText}>{review.text}</p>
                  </div>
                </div>
              ))}
            </div>

            {/* Pagination Controls */}
            <div className={styles.pagination}>
              {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                <button
                  key={page}
                  className={`${styles.pageButton} ${currentPage === page ? styles.pageButtonActive : ''}`}
                  onClick={() => setCurrentPage(page)}
                >
                  {page}
                </button>
              ))}
              <button
                className={`${styles.pageButton} ${styles.pageArrowButton}`}
                onClick={() => setCurrentPage((p) => Math.min(p + 1, totalPages))}
                disabled={currentPage === totalPages}
                aria-label="Next page"
              >
                <img src={DoubleRightIcon} alt="next" className={styles.pageArrowIcon} />
              </button>
            </div>
          </div>
        </div>
      </div>

      {bookingTour && (
        <BookingModal
          tour={bookingTour}
          user={user}
          initialSelection={{
            selectedDate,
            selectedDurations,
            tourists,
            selectedMealPlans,
          }}
          onClose={() => setBookingTour(null)}
          onConfirm={async (bookingDetails) => {
            setBookingTour(null);
            try {
              await createBooking({
                userId: user.userId,
                tourId: bookingDetails.tour.id,
                selectedDate: bookingDetails.selectedDate,
                duration: bookingDetails.duration,
                mealPlan: bookingDetails.mealPlan,
                adults: bookingDetails.adults,
                children: bookingDetails.children,
                customers: bookingDetails.customers,
                totalPrice: bookingDetails.totalPrice,
              });
              setReservationBooking(bookingDetails);
            } catch (err) {
              const msg = err?.response?.data?.error || err?.response?.data?.message || err.message || '';
              if (msg.toLowerCase().includes('already have an active booking')) {
                setDuplicateBookingMsg(msg);
              } else {
                console.error('Booking API error:', err?.response?.data || err.message);
              }
            }
          }}
        />
      )}

      {reservationBooking && (
        <ReservationConfirmationModal
          booking={reservationBooking}
          onClose={() => {
            setReservationBooking(null);
          }}
        />
      )}

      {duplicateBookingMsg && (
        <div className="fixed inset-0 z-50 flex items-center justify-center" style={{ background: 'rgba(11,56,87,0.4)' }}>
          <div className="flex flex-col gap-6 rounded-2xl bg-white p-6" style={{ width: '100%', maxWidth: '440px' }}>
            <div className="flex items-start justify-between gap-4">
              <h2 className="font-nunito text-[20px] font-bold text-[#0B3857]">Already Booked</h2>
              <button type="button" onClick={() => setDuplicateBookingMsg(null)} aria-label="Close"
                className="flex h-6 w-6 shrink-0 items-center justify-center">
                <img src={crossIcon} alt="Close" className="h-6 w-6" />
              </button>
            </div>
            <p className="font-nunito text-[14px] text-[#677883] leading-6">
              You already have an active booking for this tour on the selected date. Please choose a different date or view your existing bookings.
            </p>
            <button type="button" onClick={() => setDuplicateBookingMsg(null)}
              className="h-10 w-full rounded-lg font-nunito text-[14px] font-bold text-white"
              style={{ background: '#027EAC' }}>
              OK
            </button>
          </div>
        </div>
      )}

      {showAuthModal && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-[#42424280]">
          <div className="flex h-[272px] w-[544px] flex-col gap-8 rounded-[12px] bg-white p-6">
            <div className="flex h-24 w-[496px] flex-col gap-4">
              <div className="flex h-12 w-[496px] items-center justify-between">
                <div className="flex h-12 w-[207px] items-center gap-0">
                  <img src={travelbagIcon} alt="Travel Agency" className="h-12 w-12" />
                  <p className="flex h-[33px] w-[159px] items-center font-nunito text-[24px] font-bold leading-[100%] text-[#027EAC]">Travel Agency</p>
                </div>
                <button
                  type="button"
                  onClick={() => setShowAuthModal(false)}
                  className="flex h-6 w-6 items-center justify-center"
                  aria-label="Close"
                >
                  <img src={crossIcon} alt="Close" className="h-6 w-6" />
                </button>
              </div>
              <div className="flex h-8 w-[496px] items-center">
                <p className="h-8 w-[414px] font-nunito text-[18px] font-bold leading-8 text-[#0B3857]">
                  To book a tour please sign in or create an account
                </p>
              </div>
            </div>
            <div className="flex h-24 w-[496px] flex-col gap-4">
              <button
                type="button"
                onClick={() => { setShowAuthModal(false); navigate(ROUTES.LOGIN); }}
                className="flex h-10 w-[496px] items-center justify-center gap-1 rounded-lg bg-[#027EAC] px-4 py-2 font-nunito text-[14px] font-bold leading-6 text-white"
              >
                <span className="h-6 w-[45px] text-center font-nunito text-[14px] font-bold leading-6 text-white">Sign in</span>
              </button>
              <button
                type="button"
                onClick={() => { setShowAuthModal(false); navigate(ROUTES.REGISTER); }}
                className="flex h-10 w-[496px] items-center justify-center gap-1 rounded-lg border-2 border-[#027EAC] bg-white px-4 py-2 font-nunito text-[14px] font-bold leading-6 text-[#027EAC]"
              >
                <span className="h-6 w-[117px] text-center font-nunito text-[14px] font-bold leading-6 text-[#027EAC]">Create an account</span>
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};

export default TourDetail;

