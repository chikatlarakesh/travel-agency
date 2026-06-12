/**
 * Extract just the calendar date (YYYY-MM-DD) from any date string
 * and create a date object in UTC to avoid timezone shifts
 */
export const parseCalendarDate = (iso) => {
  if (!iso) return new Date();
  if (iso instanceof Date) {
    // Extract just the date part from the Date object
    const dateStr = iso.toISOString().split('T')[0];
    const [year, month, day] = dateStr.split('-').map(Number);
    return new Date(Date.UTC(year, month - 1, day));
  }

  const str = String(iso).split('T')[0];
  if (/^\d{4}-\d{2}-\d{2}$/.test(str)) {
    const [year, month, day] = str.split('-').map(Number);
    // Use UTC to avoid timezone conversion issues
    return new Date(Date.UTC(year, month - 1, day));
  }

  // Fallback
  return new Date(iso);
};

/**
 * Format a date to show just the date part (YYYY-MM-DD)
 * regardless of how it was stored
 */
export const formatCalendarDate = (iso, options = {}) => {
  const { month = 'short', day = 'numeric', year } = options;
  const date = parseCalendarDate(iso);

  return date.toLocaleDateString('en-US', {
    month,
    day,
    ...(year && { year }),
  });
};
