import { useEffect, useRef, useState } from 'react';
import { NavLink, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { ROUTES } from '../../config/routes';
import { useAuth } from '../../context/AuthContext';
import personIcon from '../../assets/icons/Person.svg';
import userAvatar from '../../assets/icons/user.svg';
import logOutIcon from '../../assets/icons/log-out.svg';
import travelbagIcon from '../../assets/icons/travelbag.svg';

const Navbar = () => {
  const { pathname } = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const profileMenuRef = useRef(null);

  const onToursPage = pathname.startsWith(ROUTES.TOURS);
  const onAgentBookingsPage = pathname.startsWith(ROUTES.AGENT_BOOKINGS);
  const onAdminReportsPage  = pathname.startsWith(ROUTES.ADMIN_REPORTS);
  const onAdminFeedbackPage = pathname.startsWith(ROUTES.ADMIN_FEEDBACK);
  const showTourTabs = onToursPage || pathname === ROUTES.PROFILE;
  const activeTab = searchParams.get('tab') || 'all';
  const isAdmin = user?.role === 'ADMIN';
  const isTravelAgent = user?.role === 'TRAVEL_AGENT';

  const triggerWidth = isAdmin ? '104px' : isTravelAgent ? '167px' : 'auto';
  const roleTextWidth = isAdmin ? '72px' : isTravelAgent ? '135px' : '135px';

  const roleLabel = user?.role === 'ADMIN'
    ? 'Admin'
    : user?.role === 'TRAVEL_AGENT'
      ? 'Travel Agent'
      : null;

  const switchTab = (tab) => {
    if (pathname !== ROUTES.TOURS_AVAILABLE) {
      navigate(tab === 'all' ? ROUTES.TOURS_AVAILABLE : `${ROUTES.TOURS_AVAILABLE}?tab=${tab}`);
    } else if (tab === 'all') {
      setSearchParams({});
    } else {
      setSearchParams({ tab });
    }
  };

  useEffect(() => {
    if (!menuOpen) return;

    const handlePointerDown = (event) => {
      if (profileMenuRef.current && !profileMenuRef.current.contains(event.target)) {
        setMenuOpen(false);
      }
    };

    const handleEscape = (event) => {
      if (event.key === 'Escape') setMenuOpen(false);
    };

    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('keydown', handleEscape);

    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [menuOpen]);

  const handleSignOut = async () => {
    setMenuOpen(false);
    await logout();
    setSearchParams({});
    navigate(ROUTES.LOGIN, { replace: true });
  };

  const handleProfileOpen = () => {
    setMenuOpen(false);
    navigate(`${ROUTES.PROFILE}?status=general-information-updated-successfully`);
  };

  return (
    <nav className="sticky top-0 z-50 border-b border-[#E7F9FF] bg-white">
      <div className="mx-auto w-full max-w-[1440px]">
      {/* Frame */}
      <div className="relative flex h-[72px] w-full items-center px-3 sm:px-6 lg:px-10">

        {/* Logo */}
        <NavLink
          to={ROUTES.HOME}
          className="flex h-12 w-fit shrink-0 items-center gap-0 font-nunito"
        >
          <span className="flex h-12 w-12 items-center justify-center">
            <img src={travelbagIcon} alt="Travel Agency logo" className="h-10 w-7" />
          </span>
          <span className="ml-0.5 hidden text-2xl font-bold leading-none text-[#027EAC] sm:inline">Travel Agency</span>
        </NavLink>

        {/* Center navigation — absolutely centered in the navbar */}
        <div className="absolute left-1/2 hidden -translate-x-1/2 md:block">
          {isAdmin ? (
            <div className="relative flex h-[72px] items-center">
              <button
                type="button"
                onClick={() => navigate(ROUTES.ADMIN_REPORTS)}
                className={`h-[71px] w-[127px] px-4 py-2 font-nunito text-2xl font-medium leading-8 transition-colors ${
                  onAdminReportsPage ? 'text-[#0B3857]' : 'text-[#677883] hover:text-[#0B3857]'
                }`}
              >
                Reports
              </button>
              <button
                type="button"
                onClick={() => navigate(ROUTES.ADMIN_FEEDBACK)}
                className={`h-[71px] w-[127px] px-4 py-2 font-nunito text-2xl font-medium leading-8 transition-colors ${
                  onAdminFeedbackPage ? 'text-[#0B3857]' : 'text-[#677883] hover:text-[#0B3857]'
                }`}
              >
                Feedback
              </button>
              <span
                className={`pointer-events-none absolute bottom-0 h-[5px] w-[127px] rounded-[6px] bg-[#027EAC] transition-transform duration-200 ${
                  onAdminFeedbackPage ? 'translate-x-[127px]' : 'translate-x-0'
                } ${!onAdminReportsPage && !onAdminFeedbackPage ? 'opacity-0' : ''}`}
              />
            </div>
          ) : isTravelAgent && (onToursPage || onAgentBookingsPage) ? (
            <div className="relative flex h-[72px] items-center">
              <button
                type="button"
                onClick={() => navigate(ROUTES.TOURS)}
                className={`h-[71px] w-[127px] px-4 py-2 font-nunito text-2xl font-medium leading-8 transition-colors ${
                  onToursPage ? 'text-[#0B3857]' : 'text-[#677883] hover:text-[#0B3857]'
                }`}
              >
                All tours
              </button>
              <button
                type="button"
                onClick={() => navigate(ROUTES.AGENT_BOOKINGS)}
                className={`h-[71px] w-[127px] px-4 py-2 font-nunito text-2xl font-medium leading-8 transition-colors ${
                  onAgentBookingsPage ? 'text-[#0B3857]' : 'text-[#677883] hover:text-[#0B3857]'
                }`}
              >
                Bookings
              </button>
              <span
                className={`pointer-events-none absolute bottom-0 h-[5px] w-[127px] rounded-[6px] bg-[#027EAC] transition-transform duration-200 ${
                  onAgentBookingsPage ? 'translate-x-[127px]' : 'translate-x-0'
                }`}
              />
            </div>
          ) : showTourTabs ? (
            <div className="relative flex h-[72px] items-center">
              <button
                type="button"
                onClick={() => switchTab('all')}
                className={`h-[71px] w-[127px] px-4 py-2 font-nunito text-2xl font-medium leading-8 transition-colors ${
                  !onToursPage || activeTab === 'all' ? 'text-[#0B3857]' : 'text-[#677883] hover:text-[#0B3857]'
                }`}
              >
                All tours
              </button>
              {user && (
                <button
                  type="button"
                  onClick={() => switchTab('my')}
                  className={`h-[71px] w-[127px] px-4 py-2 font-nunito text-2xl font-medium leading-8 transition-colors ${
                    !onToursPage || activeTab === 'my' ? 'text-[#0B3857]' : 'text-[#677883] hover:text-[#0B3857]'
                  }`}
                >
                  My tours
                </button>
              )}
              {user && onToursPage && (
                <span
                  className={`pointer-events-none absolute bottom-0 h-[5px] w-[127px] rounded-[6px] bg-[#027EAC] transition-transform duration-200 ${
                    activeTab === 'my' ? 'translate-x-[127px]' : 'translate-x-0'
                  }`}
                />
              )}
            </div>
          ) : user ? (
            <div className="hidden items-center gap-1 rounded-full bg-slate-100 p-1 md:flex">
              {[
                { to: ROUTES.HOME, label: 'Home', end: true },
                { to: ROUTES.DESTINATIONS, label: 'Destinations' },
                { to: ROUTES.PACKAGES, label: 'Packages' },
                { to: ROUTES.CONTACT, label: 'Contact' },
                { to: ROUTES.TOURS, label: 'Tours' },
              ].map(({ to, label, end }) => (
                <NavLink
                  key={to}
                  to={to}
                  end={end}
                  className={({ isActive }) =>
                    `rounded-full px-4 py-2 font-poppins text-sm font-medium transition ${
                      isActive ? 'bg-primary text-white shadow-sm' : 'text-brand-muted hover:text-brand-text'
                    }`
                  }
                >
                  {label}
                </NavLink>
              ))}
            </div>
          ) : null}
        </div>

        {/* Right: auth */}
        {user ? (
          <div className="relative ml-auto shrink-0" ref={profileMenuRef}>
            {/* Profile trigger */}
            <button
              type="button"
              onClick={() => setMenuOpen((prev) => !prev)}
              aria-label="Profile menu"
              title={user.userName || 'Profile'}
              style={{
                display: 'flex',
                flexDirection: 'row',
                alignItems: 'center',
                padding: '0px',
                gap: '8px',
                width: triggerWidth,
                height: '32px',
                flex: 'none',
                order: 1,
                flexGrow: 0,
                zIndex: 1,
                background: 'none',
                border: 'none',
                transition: 'none',
                cursor: 'pointer',
              }}
            >
              {/* Avatar image: 24×24, opacity 1, 0deg */}
              <img
                src={user.imageUrl || userAvatar}
                alt={user.userName || 'Profile'}
                style={{
                  width: '24px',
                  height: '24px',
                  opacity: 1,
                  transform: 'rotate(0deg)',
                  flexShrink: 0,
                  borderRadius: '9999px',
                  border: '1px solid #0B3857',
                  objectFit: 'cover',
                }}
              />
              {/* Role text shown only for ADMIN/TRAVEL_AGENT */}
              {roleLabel && (
                <span
                  style={{
                    width: roleTextWidth,
                    height: '32px',
                    fontFamily: 'Nunito',
                    fontStyle: 'normal',
                    fontWeight: 500,
                    fontSize: '24px',
                    lineHeight: '32px',
                    color: '#0B3857',
                    flex: 'none',
                    order: 1,
                    flexGrow: 0,
                    whiteSpace: 'nowrap',
                  }}
                >
                  {roleLabel}
                </span>
              )}
            </button>

            {/* Dropdown */}
            {menuOpen && (
              <div
                className="absolute right-0 z-[100]"
                style={{
                  boxSizing: 'border-box',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'flex-start',
                  padding: '16px',
                  gap: '16px',
                  width: '216px',
                  height: '184px',
                  top: '57px',
                  background: '#FFFFFF',
                  border: '1px solid #D3E1ED',
                  boxShadow: '0px 2px 10px 6px rgba(2, 126, 172, 0.2)',
                  borderRadius: '8px',
                }}
              >
                {/* User info */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', width: '184px' }}>
                  <p className="font-nunito text-[14px] font-extrabold leading-6 text-[#0B3857] overflow-hidden whitespace-nowrap"
                    style={{ textOverflow: 'ellipsis', maxWidth: '184px' }}>
                    {user.userName || 'User'}
                  </p>
                  <p className="font-nunito text-[14px] font-normal leading-6 text-[#677883] overflow-hidden whitespace-nowrap"
                    style={{ textOverflow: 'ellipsis', maxWidth: '184px' }}>
                    {user.email || 'Signed in'}
                  </p>
                </div>

                <div className="h-0 w-[184px] border-t border-[#D3E1ED]" />

                <div className="flex w-[184px] flex-col gap-2">
                  <button type="button" onClick={handleProfileOpen}
                    className="flex h-8 w-[184px] items-center gap-2 px-2 py-1 text-left">
                    <img src={personIcon} alt="My Profile" className="h-6 w-6" />
                    <span className="h-6 font-nunito text-[14px] font-extrabold leading-6 text-[#0B3857]">My Profile</span>
                  </button>
                  <button type="button" onClick={handleSignOut}
                    className="flex h-8 w-[184px] items-center gap-2 px-2 py-1 text-left">
                    <img src={logOutIcon} alt="Sign Out" className="h-6 w-6" />
                    <span className="h-6 font-nunito text-[14px] font-extrabold leading-6 text-[#0B3857]">Sign Out</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        ) : (
          <button
            type="button"
            onClick={() => navigate(ROUTES.LOGIN)}
            className="ml-auto flex h-8 w-[117px] items-center justify-center rounded-lg border-2 border-[#027EAC] bg-white px-2 py-1 font-nunito text-sm font-bold leading-6 text-[#027EAC] transition hover:bg-[#E7F9FF]"
          >
            Sign in
          </button>
        )}
      </div>

      {onToursPage && (
        <div className="flex h-12 items-end justify-center border-t border-[#E7F9FF] px-3 md:hidden">
          <div className="relative flex h-11 items-center">
            <button
              type="button"
              onClick={() => switchTab('all')}
              className={`h-11 w-[96px] px-3 py-1 font-nunito text-[18px] font-medium leading-7 transition-colors ${
                activeTab === 'all' ? 'text-[#0B3857]' : 'text-[#677883] hover:text-[#0B3857]'
              }`}
            >
              All tours
            </button>
            {user && (
              <button
                type="button"
                onClick={() => switchTab('my')}
                className={`h-11 w-[96px] px-3 py-1 font-nunito text-[18px] font-medium leading-7 transition-colors ${
                  activeTab === 'my' ? 'text-[#0B3857]' : 'text-[#677883] hover:text-[#0B3857]'
                }`}
              >
                My tours
              </button>
            )}
            {user && (
              <span
                className={`pointer-events-none absolute bottom-0 h-[4px] w-[96px] rounded-[6px] bg-[#027EAC] transition-transform duration-200 ${
                  activeTab === 'my' ? 'translate-x-[96px]' : 'translate-x-0'
                }`}
              />
            )}
          </div>
        </div>
      )}
      </div>
    </nav>
  );
};

export default Navbar;
