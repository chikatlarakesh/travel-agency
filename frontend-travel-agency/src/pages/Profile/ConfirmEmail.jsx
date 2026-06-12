import { useEffect, useMemo, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ROUTES } from '../../config/routes';
import { useAuth } from '../../context/AuthContext';
import { confirmUserEmailChange } from '../../services/userService';

const ConfirmEmail = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { logout } = useAuth();
  const hasProcessedRef = useRef(false);

  const token = useMemo(() => searchParams.get('token') || '', [searchParams]);
  const userId = useMemo(() => searchParams.get('userId') || '', [searchParams]);

  useEffect(() => {
    if (hasProcessedRef.current) {
      return;
    }
    hasProcessedRef.current = true;

    const processConfirmation = async () => {
      if (!token || !userId) {
        navigate(ROUTES.LOGIN, {
          replace: true,
          state: {
            toastType: 'email-change-failed',
            toastMessage: 'Invalid confirmation link.',
          },
        });
        return;
      }

      // Force logout if user is currently signed in, then continue confirmation flow.
      await logout({ redirectTo: null });

      try {
        await confirmUserEmailChange(userId, token);
        navigate(ROUTES.LOGIN, {
          replace: true,
          state: { toastType: 'email-changed' },
        });
      } catch (err) {
        navigate(ROUTES.LOGIN, {
          replace: true,
          state: {
            toastType: 'email-change-failed',
            toastMessage: err?.response?.data?.message || 'This confirmation link is invalid or expired.',
          },
        });
      }
    };

    processConfirmation();
  }, [token, userId, navigate, logout]);

  return null;
};

export default ConfirmEmail;