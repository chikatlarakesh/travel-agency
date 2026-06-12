import { createContext, useContext, useState, useCallback } from 'react';
import { signOut } from '../services/authService';

const AuthContext = createContext(null);

const SESSION_DURATION_MS = 7 * 24 * 60 * 60 * 1000; // 7 days

const SESSION_KEYS = ['idToken', 'role', 'userName', 'email', 'imageUrl', 'loginTimestamp'];

const clearStorage = () => SESSION_KEYS.forEach((k) => localStorage.removeItem(k));

/** Decodes the JWT payload (without signature verification) to extract the sub claim. */
const decodeJwtSubject = (token) => {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const payload = JSON.parse(atob(base64));
    return payload.sub || null;
  } catch {
    return null;
  }
};

const loadFromStorage = () => {
  const idToken    = localStorage.getItem('idToken');
  const role       = localStorage.getItem('role');
  const userName   = localStorage.getItem('userName');
  const email      = localStorage.getItem('email');
  const imageUrl   = localStorage.getItem('imageUrl');
  const timestamp  = localStorage.getItem('loginTimestamp');

  if (!idToken) return null;

  if (!timestamp || Date.now() - Number(timestamp) > SESSION_DURATION_MS) {
    clearStorage();
    return null;
  }

  const userId = decodeJwtSubject(idToken);
  return { idToken, role, userName, email, imageUrl, userId };
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(loadFromStorage);

  const login = useCallback((userData) => {
    localStorage.setItem('idToken',       userData.idToken);
    localStorage.setItem('role',          userData.role);
    localStorage.setItem('userName',      userData.userName);
    localStorage.setItem('email',         userData.email);
    localStorage.setItem('imageUrl',      userData.imageUrl || '');
    localStorage.setItem('loginTimestamp', Date.now().toString());
    const userId = decodeJwtSubject(userData.idToken);
    setUser({ ...userData, userId });
  }, []);

  const logout = useCallback(async (options = {}) => {
    const { redirectTo = '/', skipServer = false } = options;
    if (!skipServer) {
      try { await signOut(); } catch { /* ignore */ }
    }
    clearStorage();
    setUser(null);
    if (redirectTo) {
      window.location.href = redirectTo;
    }
  }, []);

  const updateUser = useCallback((partial) => {
    if (partial.userName !== undefined) localStorage.setItem('userName', partial.userName);
    if (partial.email !== undefined)    localStorage.setItem('email', partial.email);
    if (partial.imageUrl !== undefined) localStorage.setItem('imageUrl', partial.imageUrl || '');
    setUser((prev) => ({ ...prev, ...partial }));
  }, []);

  return (
    <AuthContext.Provider value={{ user, login, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
