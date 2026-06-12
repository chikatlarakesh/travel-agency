import axios from 'axios';
import { BASE_URL } from '../config/constants.jsx';

const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true, // send refresh-token cookie
});

// Attach access token from localStorage to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('idToken');
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

// Shared refresh promise — ensures only one /auth/refresh call happens at a time.
// All concurrent 401s wait for the same refresh instead of each triggering their own.
let refreshPromise = null;

const doRefresh = () => {
  if (!refreshPromise) {
    refreshPromise = axios
      .post(`${BASE_URL}/auth/refresh`, {}, { withCredentials: true })
      .then((res) => {
        const newToken = res.data.idToken;
        localStorage.setItem('idToken', newToken);
        return newToken;
      })
      .catch((err) => {
        if (err.response?.status === 401) {
          localStorage.removeItem('idToken');
          localStorage.removeItem('role');
          localStorage.removeItem('userName');
          localStorage.removeItem('email');
          window.location.href = '/login';
        }
        throw err;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
};

// On 401, refresh once (shared), then replay the original request
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        const newToken = await doRefresh();
        originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

export default api;

