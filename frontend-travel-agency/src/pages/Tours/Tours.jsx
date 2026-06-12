import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import crossIcon from '../../assets/icons/Cross.svg';
import doubleRightIcon from '../../assets/icons/double-right.svg';
import travelbagIcon from '../../assets/icons/travelbag.svg';
import { ReactComponent as DownwardIcon } from '../../assets/icons/downward.svg';
import BookingModal from '../../components/Tours/BookingModal';
import ReservationConfirmationModal from '../../components/Tours/ReservationConfirmationModal';
import FilterBar from '../../components/Tours/FilterBar';
import TourList from '../../components/Tours/TourList';
import { ROUTES } from '../../config/routes';
import { useAuth } from '../../context/AuthContext';
import { getDestinations, getTours } from '../../services/travelService';
import { createBooking } from '../../services/bookingService';
import MyTours from './MyTours';

const DEFAULT_FILTERS = {
  destination: '',
  startDates: [],
  durations: [],
  tourists: { adults: 2, children: 0 },
  mealPlans: [],
  tourTypes: [],
  sortBy: 'top-rated',
};

const SORT_OPTIONS = [
  { value: 'top-rated', label: 'Top rated first' },
  { value: 'price-low-high', label: 'Lowest price first' },
  { value: 'price-high-low', label: 'Highest price first' },
];

const TOURS_PER_PAGE = 6;

const SortDropdown = ({ value, onChange }) => {
  const [open, setOpen] = useState(false);
  const rootRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (rootRef.current && !rootRef.current.contains(event.target)) {
        setOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const selectedLabel = useMemo(
    () => SORT_OPTIONS.find((option) => option.value === value)?.label || 'Top rated first',
    [value]
  );

  return (
    <div className="relative w-full max-w-[186px]" ref={rootRef}>
      <button
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        className={`flex h-10 w-full items-center gap-1.5 rounded-lg border bg-white px-2.5 text-left transition ${
          open ? 'border-[#027EAC]' : 'border-[#D3E1ED] hover:border-[#027EAC]/50'
        }`}
        aria-haspopup="listbox"
        aria-expanded={open}
      >
        <span className="flex-1 truncate font-nunito text-[14px] font-semibold leading-5 text-[#0B3857]" title={selectedLabel}>
          {selectedLabel}
        </span>
        <DownwardIcon className={`h-[6px] w-3 shrink-0 text-[#0B3857] transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>

      {open && (
        <div
          className="absolute right-0 top-[calc(100%+8px)] z-30 w-full overflow-hidden rounded-lg border border-[#D3E1ED] bg-white shadow-[0px_8px_20px_rgba(11,56,87,0.12)]"
          role="listbox"
        >
          {SORT_OPTIONS.map((option) => {
            const isActive = option.value === value;
            return (
              <button
                key={option.value}
                type="button"
                onClick={() => {
                  onChange(option.value);
                  setOpen(false);
                }}
                className={`flex w-full items-center px-3 py-2 text-left font-nunito text-[14px] leading-6 transition ${
                  isActive
                    ? 'bg-[#E7F9FF] text-[#027EAC] font-bold'
                    : 'text-[#0B3857] hover:bg-[#F3F8FB]'
                }`}
                title={option.label}
                role="option"
                aria-selected={isActive}
              >
                <span className="truncate">{option.label}</span>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
};

const Tours = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { user } = useAuth();

  const [filters, setFilters]               = useState(DEFAULT_FILTERS);
  const [appliedFilters, setAppliedFilters] = useState(DEFAULT_FILTERS);
  const [tours, setTours]                   = useState([]);
  const [loading, setLoading]               = useState(true);
  const [destQuery, setDestQuery]           = useState('');
  const [destOptions, setDestOptions]       = useState([]);
  const [loadingDest, setLoadingDest]       = useState(false);
  const [allToursCount, setAllToursCount]   = useState(0);
  const [hasSearched, setHasSearched]       = useState(false);
  const [showAuthModal, setShowAuthModal]   = useState(false);
  const [bookingTour, setBookingTour]       = useState(null);
  const [reservationBooking, setReservationBooking] = useState(null);
  const [currentPage, setCurrentPage]       = useState(1);
  const [duplicateBookingMsg, setDuplicateBookingMsg] = useState(null);

  const activeTab       = searchParams.get('tab') || 'all';
  const showSignInAlert = searchParams.get('signin') === '1' && !user;

  useEffect(() => {
    if (!user && activeTab === 'my') {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev);
        next.delete('tab');
        return next;
      });
    }
  }, [activeTab, user, setSearchParams]);

  /* destination options */
  useEffect(() => {
    let dead = false;
    setLoadingDest(true);
    getDestinations(destQuery).then((opts) => {
      if (!dead) { setDestOptions(opts); setLoadingDest(false); }
    });
    return () => { dead = true; };
  }, [destQuery]);

  /* tour results */
  useEffect(() => {
    let dead = false;
    setLoading(true);
    getTours(appliedFilters).then((data) => {
      if (!dead) { setTours(data); setLoading(false); }
    });
    return () => { dead = true; };
  }, [appliedFilters]);

  /* total tours count for dynamic "X of Y" text */
  useEffect(() => {
    let dead = false;
    getTours(DEFAULT_FILTERS).then((data) => {
      if (!dead) {
        const total = activeTab === 'my' && user ? data.filter((t) => t.rating >= 4.8).length : data.length;
        setAllToursCount(total);
      }
    });
    return () => { dead = true; };
  }, [activeTab, user]);

  const handleFilterChange = useCallback((key, val) => {
    setFilters((prev) => ({ ...prev, [key]: val }));
  }, []);

  const handleSearch = useCallback(() => {
    setAppliedFilters({ ...filters });
    setHasSearched(true);
    setCurrentPage(1);
  }, [filters]);

  const handleSortChange = useCallback((value) => {
    setFilters((prev) => ({ ...prev, sortBy: value }));
    setAppliedFilters((prev) => ({ ...prev, sortBy: value }));
    setCurrentPage(1);
  }, []);

  const handleClearAllFilters = useCallback(() => {
    setFilters(DEFAULT_FILTERS);
    setAppliedFilters(DEFAULT_FILTERS);
    setDestQuery('');
    setHasSearched(false);
    setCurrentPage(1);
  }, []);

  const handleBookTour = useCallback((tour) => {
    if (!user) { setShowAuthModal(true); return; }
    setBookingTour(tour);
  }, [user]);

  const handleSeeDetails = useCallback((tour) => {
    navigate(`/tours/${tour.id}`);
  }, [navigate]);

  const visibleTours = useMemo(() => {
    if (activeTab === 'my') return user ? tours.filter((t) => t.rating >= 4.8) : [];
    return tours;
  }, [tours, activeTab, user]);

  const totalPages = useMemo(
    () => Math.max(1, Math.ceil(visibleTours.length / TOURS_PER_PAGE)),
    [visibleTours.length]
  );

  const paginatedTours = useMemo(() => {
    const start = (currentPage - 1) * TOURS_PER_PAGE;
    return visibleTours.slice(start, start + TOURS_PER_PAGE);
  }, [visibleTours, currentPage]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  /* ── My Tours: render dedicated page ── */
  if (activeTab === 'my' && user) {
    return <MyTours user={user} />;
  }

  return (
    <>
      <div className="mx-auto flex w-[calc(100%-40px)] max-w-[1360px] flex-col gap-6 pb-10 pt-10">

      <header>
        <h1 className="flex h-10 w-full items-center justify-center text-center font-nunito text-[32px] font-semibold leading-10 text-[#0B3857]">
          Search for your next tour
        </h1>
      </header>

      {/* ── Inner frame: filter + results ── */}
      <div className="flex w-full max-w-[1360px] flex-col gap-12">

        {/* ── Sign-in banner ── */}
        {showSignInAlert && (
          <div className="flex items-center justify-between gap-4 rounded-xl border border-primary/20 bg-white px-6 py-4 shadow-card">
            <p className="font-nunito text-sm font-semibold text-[#0B3857]">
              Please sign in to book tours and see your saved selections.
            </p>
            <button
              type="button"
              onClick={() => navigate(ROUTES.LOGIN)}
              className="shrink-0 rounded-lg bg-[#027EAC] px-5 py-2 font-nunito text-sm font-bold text-white transition hover:bg-primary-dark"
            >
              Sign in
            </button>
          </div>
        )}

        {/* ── Filter bar ── */}
        <FilterBar
          filters={filters}
          destinationOptions={destOptions}
          destinationQuery={destQuery}
          loadingDestinations={loadingDest}
          onDestinationQuery={setDestQuery}
          onDestinationSelect={(v) => handleFilterChange('destination', v)}
          onFilterChange={handleFilterChange}
          onSearch={handleSearch}
        />

        {/* ── Popular tours: sorting + cards ── */}
        <div className="flex w-full max-w-[1360px] flex-col gap-4">
          <div className="flex w-full items-center justify-between gap-4">
            {hasSearched ? (
              <div className="flex h-6 items-center gap-2">
                <p className="h-6 whitespace-nowrap font-nunito text-[14px] font-extrabold leading-6 text-[#0B3857]">
                  {visibleTours.length} of {allToursCount}{' '}
                  <span className="font-nunito text-[14px] font-normal leading-6 text-[#0B3857]">tours match your filters.</span>
                </p>
                <button
                  type="button"
                  onClick={handleClearAllFilters}
                  className="h-6 whitespace-nowrap text-left font-nunito text-[14px] font-bold leading-6 text-[#0B3857] underline decoration-solid [text-underline-offset:7.5%] [text-decoration-thickness:8%]"
                >
                  Clear all filters
                </button>
              </div>
            ) : <div />}

            <div className="ml-auto flex h-10 w-full max-w-[206px] items-center justify-end gap-2">
              <span className="shrink-0 whitespace-nowrap text-center font-nunito text-[14px] font-extrabold leading-6 text-[#0B3857]">Sort by:</span>
              <SortDropdown value={filters.sortBy} onChange={handleSortChange} />
            </div>
          </div>

          <div className="flex w-full max-w-[1360px] flex-col gap-4">
            <div className="flex w-full max-w-[1360px] flex-col gap-8">
              <TourList
                tours={paginatedTours}
                loading={loading}
                onSeeDetails={handleSeeDetails}
                onBookTour={handleBookTour}
              />

              {!loading && totalPages > 1 && (
                <div className="mx-auto flex h-7 w-28 items-center justify-center gap-2">
                  {Array.from({ length: totalPages }, (_, index) => index + 1).map((pageNumber) => (
                    <button
                      key={pageNumber}
                      type="button"
                      onClick={() => setCurrentPage(pageNumber)}
                      className={`inline-flex h-7 w-6 items-center justify-center border-b-2 border-transparent pb-1 font-poppins text-[14px] font-medium leading-6 tracking-[0%] text-[#0B3857] transition-colors ${
                        pageNumber === currentPage
                          ? 'border-b-[#027EAC]'
                          : 'hover:text-[#027EAC]'
                      }`}
                      aria-current={pageNumber === currentPage ? 'page' : undefined}
                    >
                      {pageNumber}
                    </button>
                  ))}

                  <button
                    type="button"
                    onClick={() => setCurrentPage((prev) => Math.min(totalPages, prev + 1))}
                    disabled={currentPage === totalPages}
                    className="inline-flex h-7 w-6 items-center justify-center border-b-2 border-transparent pb-1 transition-opacity disabled:cursor-default disabled:opacity-45"
                    aria-label="Next page"
                  >
                    <img src={doubleRightIcon} alt="next" className="h-4 w-4" />
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>

      </div>
      </div>

      {bookingTour && (
        <BookingModal
          tour={bookingTour}
          user={user}
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
            navigate(ROUTES.TOURS_AVAILABLE);
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
                <button
                  type="button"
                  onClick={() => {
                    setShowAuthModal(false);
                    navigate(ROUTES.TOURS_AVAILABLE);
                  }}
                  className="flex h-12 w-[207px] items-center gap-0"
                >
                  <img src={travelbagIcon} alt="Travel Agency" className="h-12 w-12" />
                  <p className="flex h-[33px] w-[159px] items-center font-nunito text-[24px] font-bold leading-[100%] text-[#027EAC]">Travel Agency</p>
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setShowAuthModal(false);
                    navigate(ROUTES.TOURS);
                  }}
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
                onClick={() => {
                  setShowAuthModal(false);
                  navigate(ROUTES.LOGIN);
                }}
                className="flex h-10 w-[496px] items-center justify-center gap-1 rounded-lg bg-[#027EAC] px-4 py-2 font-nunito text-[14px] font-bold leading-6 text-white"
              >
                <span className="h-6 w-[45px] text-center font-nunito text-[14px] font-bold leading-6 text-white">
                  Sign in
                </span>
              </button>
              <button
                type="button"
                onClick={() => {
                  setShowAuthModal(false);
                  navigate(ROUTES.REGISTER);
                }}
                className="flex h-10 w-[496px] items-center justify-center gap-1 rounded-lg border-2 border-[#027EAC] bg-white px-4 py-2 font-nunito text-[14px] font-bold leading-6 text-[#027EAC]"
              >
                <span className="h-6 w-[117px] text-center font-nunito text-[14px] font-bold leading-6 text-[#027EAC]">
                  Create an account
                </span>
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default Tours;
