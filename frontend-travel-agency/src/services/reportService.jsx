import axios from 'axios';

const REPORT_BASE_URL = process.env.REACT_APP_REPORT_API_URL || 'http://localhost:8081/api/reports';

const getAuthHeader = () => {
  const token = localStorage.getItem('idToken');
  return token ? { Authorization: `Bearer ${token}` } : {};
};

export const fetchReport = async ({ type, fromDate, toDate, location }) => {
  const params = { type, fromDate, toDate };
  if (location) params.location = location;

  const response = await axios.get(REPORT_BASE_URL, {
    params,
    headers: getAuthHeader(),
  });
  return response.data;
};

export const downloadReport = ({ type, fromDate, toDate, format, location }) => {
  const params = new URLSearchParams({ type, fromDate, toDate, format });
  if (location) params.append('location', location);

  const url = `${REPORT_BASE_URL}/download?${params.toString()}`;
  const token = localStorage.getItem('idToken');

  return fetch(url, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  }).then((res) => {
    if (!res.ok) throw new Error('Download failed');
    return res.blob();
  });
};
