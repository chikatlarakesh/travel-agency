// DateDurationPicker Component
// Calendar for selecting start date + Duration checkboxes
// Used in TourDetail booking panel

import React, { useState, useRef, useEffect } from 'react';
import styles from './DateDurationPicker.module.css';
import { ReactComponent as CalendarIcon } from '../../assets/icons/Calendar.svg';
import { ReactComponent as CheckboxIcon } from '../../assets/icons/checkbox.svg';

const normalizeRange = (dates = []) => {
  const uniqueSorted = [...new Set(dates)].sort((a, b) => new Date(a) - new Date(b));
  return uniqueSorted.slice(0, 2);
};

const DateDurationPicker = ({
  availableDates = [],
  selectedDate,
  selectedDates = [],
  selectedDurations = [],
  onDateSelect,
  onDatesChange,
  onDurationsChange,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const dropdownRef = useRef(null);

  // Duration options - matches tour data structure
  const durationOptions = [
    { value: 7, label: '7 days' },
    { value: 10, label: '10 days' },
    { value: 12, label: '12 days' },
  ];

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  // Format display value
  const getDisplayValue = () => {
    const normalizedDates = normalizeRange(
      selectedDates.length > 0 ? selectedDates : (selectedDate ? [selectedDate] : [])
    );

    if (normalizedDates.length > 0 && selectedDurations.length > 0) {
      const formatDate = (value) => {
        const date = new Date(value);
        const monthName = date.toLocaleDateString('en-US', { month: 'short' });
        const day = date.getDate();
        return `${monthName} ${day}`;
      };

      const datePart = normalizedDates.length === 2
        ? `${formatDate(normalizedDates[0])} - ${formatDate(normalizedDates[1])}`
        : formatDate(normalizedDates[0]);

      const duration = selectedDurations[0]; // Only one duration now
      return `${datePart}, ${duration} days`;
    }
    return 'Select date & duration';
  };

  // Generate calendar days
  const getDaysInMonth = (date) => {
    const year = date.getFullYear();
    const month = date.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const daysInMonth = lastDay.getDate();
    const startingDayOfWeek = firstDay.getDay();

    const days = [];

    // Empty cells for days before month starts
    for (let i = 0; i < startingDayOfWeek; i++) {
      days.push(null);
    }

    // Actual days
    for (let day = 1; day <= daysInMonth; day++) {
      days.push(new Date(year, month, day));
    }

    return days;
  };

  const handlePrevMonth = () => {
    setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1, 1));
  };

  const handleNextMonth = () => {
    setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 1));
  };

  const handleDateClick = (date) => {
    if (date && isDateAvailable(date)) {
      // Format as YYYY-MM-DD avoiding timezone issues
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const value = `${year}-${month}-${day}`;

      const currentDates = normalizeRange(
        selectedDates.length > 0 ? selectedDates : (selectedDate ? [selectedDate] : [])
      );

      let nextDates = [];

      if (currentDates.length === 0) {
        nextDates = [value];
      } else if (currentDates.length === 1) {
        if (currentDates[0] === value) {
          nextDates = [];
        } else {
          nextDates = normalizeRange([currentDates[0], value]);
        }
      } else {
        nextDates = [value];
      }

      if (onDatesChange) {
        onDatesChange(nextDates);
      }

      if (onDateSelect) {
        onDateSelect(nextDates[0] || '');
      }
    }
  };

  const isDateAvailable = (date) => {
    // Enable all dates from today onwards (no restriction by availableDates)
    // This allows the user to select any valid future date
    if (!date) return false;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const compareDate = new Date(date);
    compareDate.setHours(0, 0, 0, 0);
    return compareDate >= today;
  };

  const isDateSelected = (date) => {
    if (!date) return false;
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const dateStr = `${year}-${month}-${day}`;
    const normalizedDates = normalizeRange(
      selectedDates.length > 0 ? selectedDates : (selectedDate ? [selectedDate] : [])
    );
    return normalizedDates.includes(dateStr);
  };

  const isDateInRange = (date) => {
    if (!date) return false;
    const normalizedDates = normalizeRange(
      selectedDates.length > 0 ? selectedDates : (selectedDate ? [selectedDate] : [])
    );

    if (normalizedDates.length !== 2) return false;

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const dateStr = `${year}-${month}-${day}`;

    return dateStr > normalizedDates[0] && dateStr < normalizedDates[1];
  };

  const handleDurationToggle = (durationValue) => {
    // Single selection - replace current selection
    onDurationsChange([durationValue]);
  };

  const days = getDaysInMonth(currentMonth);
  const monthYear = currentMonth.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });

  return (
    <div className={styles.container} ref={dropdownRef}>
      {/* Dropdown Trigger */}
      <button
        type="button"
        className={`${styles.trigger} ${isOpen ? styles.triggerOpen : ''}`}
        onClick={() => setIsOpen(!isOpen)}
      >
        <div className={styles.triggerContent}>
          <CalendarIcon className={styles.leftIcon} />
          <span className={styles.triggerText}>{getDisplayValue()}</span>
        </div>
        {/* Chevron Icon */}
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" className={isOpen ? styles.chevronUp : styles.chevronDown}>
          <path d="M6 9l6 6 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
      </button>

      {/* Dropdown Content */}
      {isOpen && (
        <div className={styles.dropdown}>
          <div className={styles.dropdownContent}>
            {/* Left: Calendar */}
            <div className={styles.calendarSection}>
              <div className={styles.calendarHeader}>
                <h4 className={styles.sectionTitle}>Start date</h4>
                <div className={styles.monthNavigation}>
                  <button type="button" onClick={handlePrevMonth} className={styles.navButton}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                      <path d="M15 18l-6-6 6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                    </svg>
                  </button>
                  <span className={styles.monthYear}>{monthYear}</span>
                  <button type="button" onClick={handleNextMonth} className={styles.navButton}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                      <path d="M9 18l6-6-6-6" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                    </svg>
                  </button>
                </div>
              </div>

              {/* Calendar Grid */}
              <div className={styles.calendar}>
                <div className={styles.weekDaysRow}>
                  {['M', 'T', 'W', 'T', 'F', 'S', 'S'].map((day, idx) => (
                    <div key={idx} className={styles.dayHeader}>{day}</div>
                  ))}
                </div>

                <div className={styles.calendarDivider} />

                <div className={styles.dateGrid}>
                  {days.map((date, idx) => {
                    if (!date) {
                      return <span key={idx} className={styles.emptyCell}></span>;
                    }

                    const isAvailable = isDateAvailable(date);
                    const isSelected = isDateSelected(date);
                    const isInRange = isDateInRange(date);

                    const year = date.getFullYear();
                    const month = String(date.getMonth() + 1).padStart(2, '0');
                    const day = String(date.getDate()).padStart(2, '0');
                    const dateStr = `${year}-${month}-${day}`;

                    const normalizedDates = normalizeRange(
                      selectedDates.length > 0 ? selectedDates : (selectedDate ? [selectedDate] : [])
                    );
                    const hasRange = normalizedDates.length === 2;
                    const isRangeStart = hasRange && dateStr === normalizedDates[0];
                    const isRangeEnd = hasRange && dateStr === normalizedDates[1];

                    return (
                      <div key={idx} className={styles.dateSlot}>
                        {(isInRange || isRangeStart || isRangeEnd) && (
                          <span
                            className={`${styles.rangeFill} ${
                              isInRange
                                ? styles.rangeFillMiddle
                                : isRangeStart
                                  ? styles.rangeFillStart
                                  : styles.rangeFillEnd
                            }`}
                          />
                        )}

                        <button
                          type="button"
                          className={`${styles.dateCell} ${isAvailable ? styles.available : styles.unavailable} ${isSelected ? styles.selected : ''}`}
                          onClick={() => handleDateClick(date)}
                          disabled={!isAvailable}
                        >
                          {date.getDate()}
                        </button>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>

            {/* Right: Duration */}
            <div className={styles.durationSection}>
              <h4 className={styles.sectionTitle}>Duration</h4>
              <div className={styles.durationOptions}>
                {durationOptions.map((option) => {
                  const checked = selectedDurations.includes(option.value);
                  return (
                  <label key={option.value} className={`${styles.durationOptionRow} ${checked ? styles.durationOptionSelected : ''}`}>
                    <input
                      type="radio"
                      name="duration"
                      checked={checked}
                      onChange={() => handleDurationToggle(option.value)}
                      className={styles.optionInput}
                    />
                    <span className={`${styles.optionMark} ${checked ? styles.optionMarkChecked : ''}`}>
                      {checked && <CheckboxIcon className={styles.optionCheckIcon} />}
                    </span>
                    <span className={styles.durationOptionText}>{option.label}</span>
                  </label>
                );})}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DateDurationPicker;

