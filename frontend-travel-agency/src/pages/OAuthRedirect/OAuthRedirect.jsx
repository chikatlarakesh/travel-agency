import { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { ROUTES } from '../../config/routes';

/**
 * Handles the post-OAuth2 redirect from the backend for EXISTING users.
 *
 * The backend redirects here after a successful Google login:
 *   /oauth2/redirect?token=JWT&role=CUSTOMER&userName=Rakesh&email=user@example.com
 *
 * This page extracts the query params, stores the token via the same
 * AuthContext.login() used by the normal email/password flow, then
 * navigates to the appropriate landing page.
 */
export default function OAuthRedirect() {
  const [searchParams] = useSearchParams();
  const { login } = useAuth();
  const navigate = useNavigate();
  const handled = useRef(false);

  useEffect(() => {
    if (handled.current) return;
    handled.current = true;

    const token    = searchParams.get('token');
    const role     = searchParams.get('role');
    const userName = searchParams.get('userName');
    const email    = searchParams.get('email');
    const error    = searchParams.get('error');

    if (error || !token) {
      navigate(ROUTES.LOGIN, { replace: true });
      return;
    }

    // Backend sends "token"; AuthContext.login() expects "idToken" — map here.
    login({ idToken: token, role, userName, email });

    navigate(role === 'ADMIN' ? ROUTES.ADMIN_REPORTS : ROUTES.TOURS, { replace: true });
  }, [searchParams, login, navigate]);

  return null;
}

