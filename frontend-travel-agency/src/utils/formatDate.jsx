/**
 * Formats a date string to a readable format.
 * @param {string|Date} date
 * @returns {string}
 */
export const formatDate = (date) => {
  if (!date) return '';
  const iso = typeof date === 'string' ? date : date.toISOString();
  const [year, month, day] = iso.split('T')[0].split('-').map(Number);
  return new Date(year, month - 1, day).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
};

