import { Navigate, Route, Routes } from 'react-router-dom';
import MainLayout from './layouts/MainLayout';
import { ROUTES } from './config/routes';
import { useAuth } from './context/AuthContext';
import Contact from './pages/Contact/Contact';
import Destinations from './pages/Destinations/Destinations';
import LoginPage from './pages/Login/Login';
import Packages from './pages/Packages/Packages';
import ConfirmEmail from './pages/Profile/ConfirmEmail';
import Profile from './pages/Profile/Profile';
import Registration from './pages/Registration/Registration';
import Tours from './pages/Tours/Tours';
import TourDetail from './pages/TourDetail/TourDetail';
import ForgotPassword from './pages/ForgotPassword/ForgotPassword';
import ResetPassword from './pages/ResetPassword/ResetPassword';
import AgentBookings from './pages/AgentBookings/AgentBookings';
import AdminReports from './pages/AdminReports/AdminReports';
import AdminFeedbackModeration from './pages/AdminFeedbackModeration/AdminFeedbackModeration';
import OAuthRedirect from './pages/OAuthRedirect/OAuthRedirect';
import OAuthSignup from './pages/OAuthSignup/OAuthSignup';
import './App.css';

const DefaultRedirect = () => {
  const { user } = useAuth();
  return <Navigate to={user?.role === 'ADMIN' ? ROUTES.ADMIN_REPORTS : ROUTES.TOURS} replace />;
};

function App() {
  return (
    <Routes>
      <Route path={ROUTES.LOGIN} element={<LoginPage />} />
      <Route path={ROUTES.REGISTER} element={<Registration />} />
      <Route path={ROUTES.FORGOT_PASSWORD} element={<ForgotPassword />} />
      <Route path={ROUTES.RESET_PASSWORD} element={<ResetPassword />} />
      <Route path={ROUTES.OAUTH_REDIRECT} element={<OAuthRedirect />} />
      <Route path={ROUTES.OAUTH_SIGNUP} element={<OAuthSignup />} />
      <Route
        path="*"
        element={(
          <MainLayout>
            <Routes>
              <Route path={ROUTES.HOME} element={<DefaultRedirect />} />
              <Route path={ROUTES.DESTINATIONS} element={<Destinations />} />
              <Route path={ROUTES.PACKAGES} element={<Packages />} />
              <Route path={ROUTES.CONTACT} element={<Contact />} />
              <Route path={ROUTES.PROFILE} element={<Profile />} />
              <Route path={ROUTES.PROFILE_CONFIRM_EMAIL} element={<ConfirmEmail />} />
              <Route path={ROUTES.TOURS} element={<Tours />} />
              <Route path={ROUTES.TOURS_AVAILABLE} element={<Tours />} />
              <Route path={ROUTES.TOUR_DETAIL} element={<TourDetail />} />
              <Route path={ROUTES.AGENT_BOOKINGS} element={<AgentBookings />} />
              <Route path={ROUTES.ADMIN_REPORTS} element={<AdminReports />} />
              <Route path={ROUTES.ADMIN_FEEDBACK} element={<AdminFeedbackModeration />} />
              <Route path="*" element={<DefaultRedirect />} />
            </Routes>
          </MainLayout>
        )}
      />
    </Routes>
  );
}

export default App;
