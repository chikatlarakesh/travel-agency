import { useMemo, useState, useCallback, useEffect } from 'react';
import MyTourCard from '../../components/MyTours/MyTourCard';
import doubleRightIcon from '../../assets/icons/double-right.svg';
import { getUserBookings } from '../../services/bookingService';
import { useAuth } from '../../context/AuthContext';

const TOURS_PER_PAGE = 6;

const SUB_TABS = [
  { key: 'all', label: 'All tours' },
  { key: 'booked', label: 'Booked' },
  { key: 'confirmed', label: 'Confirmed' },
  { key: 'started', label: 'Started' },
  { key: 'finished', label: 'Finished' },
  { key: 'canceled', label: 'Canceled' },
];

const EmptyMessage = ({ tab }) => (
  <div className="col-span-1 flex flex-col items-center justify-center gap-3 rounded-xl border border-[#E8F0F5] bg-white py-16 text-center shadow-card md:col-span-2">
    <svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="24" cy="24" r="22" fill="#E7F9FF" />
      <path d="M16 24h16M24 16v16" stroke="#027EAC" strokeWidth="2" strokeLinecap="round" />
    </svg>
    <p className="font-nunito text-[15px] font-semibold text-[#0B3857]">
      No{tab !== 'all' ? ` ${tab}` : ''} tours found
    </p>
    <p className="font-nunito text-[13px] text-[#677883]">
      Tours with &ldquo;{tab}&rdquo; status will appear here.
    </p>
  </div>
);

const MyTours = ({ user: userProp }) => {
  const { user: authUser } = useAuth();
  const user = userProp || authUser;
  const [activeSubTab, setActiveSubTab] = useState('all');
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);
  const [activeTabWidth, setActiveTabWidth] = useState(91);

  useEffect(() => {
    if (!user?.userId) {
      setLoading(false);
      return;
    }
    let dead = false;
    setLoading(true);

    getUserBookings(user.userId)
      .then((bookings) => {
        if (!dead) {
          setBookings(bookings);
          setLoading(false);
        }
      })
      .catch((err) => {
        console.error('Failed to load bookings:', err);
        if (!dead) setLoading(false);
      });

    return () => {
      dead = true;
    };
  }, [user?.userId]);

  const handleCancel = (id) => {
    setBookings((prev) =>
      prev.map((b) => (b.id === id ? { ...b, status: 'canceled', canceledBy: 'Tourist', canceledAfterStep: b.status === 'confirmed' ? 1 : 0 } : b))
    );
  };

  const filtered = useMemo(() => {
    if (activeSubTab === 'all') return bookings;
    return bookings.filter((t) => t.status === activeSubTab);
  }, [activeSubTab, bookings]);

  const totalPages = useMemo(
    () => Math.max(1, Math.ceil(filtered.length / TOURS_PER_PAGE)),
    [filtered.length]
  );

  const paginatedBookings = useMemo(() => {
    const start = (currentPage - 1) * TOURS_PER_PAGE;
    return filtered.slice(start, start + TOURS_PER_PAGE);
  }, [filtered, currentPage]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  const handleTabClick = useCallback((key, element) => {
    setActiveSubTab(key);
    setCurrentPage(1);
    if (element) {
      setActiveTabWidth(element.offsetWidth);
    }
  }, []);

  return (
    <div className="min-h-screen bg-white">
      {/* ── Container ── */}
      <div
        className="mx-auto flex flex-col gap-8 px-5 py-7 md:px-10 md:py-10"
        style={{
          maxWidth: '1360px',
          marginLeft: 'auto',
          marginRight: 'auto',
        }}
      >
        {/* ── Tabs ── */}
        <div
          className="w-full overflow-x-auto overflow-y-hidden"
          style={{
            display: 'flex',
            flexDirection: 'row',
            alignItems: 'center',
            padding: '0px',
            height: '48px',
          }}
        >
          <div
            className="min-w-max"
            style={{
              display: 'flex',
              flexDirection: 'row',
              alignItems: 'center',
              gap: '0px',
            }}
          >
            {SUB_TABS.map(({ key, label }) => {
              const isActive = activeSubTab === key;

              return (
                <button
                  ref={(el) => {
                    if (isActive && el) {
                      setActiveTabWidth(el.offsetWidth);
                    }
                  }}
                  key={key}
                  type="button"
                  onClick={(e) => handleTabClick(key, e.currentTarget)}
                  className="relative flex flex-shrink-0 flex-col items-center justify-center gap-2 border-b-[1px] px-4 py-1 transition-all duration-200 hover:bg-[#F5F9FC]"
                  style={{
                    height: '48px',
                    borderBottomColor: isActive ? '#027EAC' : '#A2AEB9',
                    borderBottomWidth: isActive ? '1px' : '1px',
                    backgroundColor: 'transparent',
                    cursor: 'pointer',
                    boxSizing: 'border-box',
                    paddingTop: '4px',
                    paddingRight: '16px',
                    paddingBottom: '4px',
                    paddingLeft: '16px',
                  }}
                >
                  <span
                    style={{
                      fontFamily: 'Nunito, sans-serif',
                      fontStyle: 'normal',
                      fontWeight: isActive ? '800' : '600',
                      fontSize: '14px',
                      lineHeight: '24px',
                      letterSpacing: '0%',
                      color: isActive ? '#0B3857' : '#677883',
                      whiteSpace: 'nowrap',
                      zIndex: 0,
                    }}
                  >
                    {label}
                  </span>

                  {isActive && (
                    <span
                      style={{
                        position: 'absolute',
                        width: `${activeTabWidth}px`,
                        height: '5px',
                        bottom: '-1px',
                        left: '50%',
                        transform: 'translateX(-50%)',
                        background: '#027EAC',
                        borderRadius: '6px',
                        zIndex: 1,
                      }}
                    />
                  )}
                </button>
              );
            })}
          </div>
        </div>

        {/* ── Cards Container ── */}
        <div
          className="grid w-full grid-cols-1 gap-4 md:grid-cols-2"
          style={{
            gap: '16px',
          }}
        >
          {loading ? null : filtered.length === 0 ? (
            <EmptyMessage tab={activeSubTab} />
          ) : (
            paginatedBookings.map((booking) => (
              <MyTourCard key={booking.id} booking={booking} onCancel={handleCancel} />
            ))
          )}
        </div>

        {!loading && totalPages > 1 && (
          <div className="mx-auto flex h-7 w-28 items-center justify-center gap-2">
            {Array.from({ length: totalPages }, (_, index) => index + 1).map((pageNumber) => (
              <button
                key={pageNumber}
                type="button"
                onClick={() => setCurrentPage(pageNumber)}
                className={`inline-flex h-7 w-6 items-center justify-center border-b-2 border-transparent pb-1 font-poppins text-[14px] font-medium leading-6 tracking-[0%] text-[#0B3857] transition-colors ${
                  pageNumber === currentPage ? 'border-b-[#027EAC]' : 'hover:text-[#027EAC]'
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
  );
};

export default MyTours;
