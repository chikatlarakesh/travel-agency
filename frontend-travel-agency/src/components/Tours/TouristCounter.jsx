// TouristCounter Component
// Plus/Minus controls for selecting adults and children
// Used in TourDetail booking panel

import React, { useState, useRef, useEffect } from 'react';
import styles from './TouristCounter.module.css';
import { ReactComponent as PeopleIcon } from '../../assets/icons/People.svg';

const TouristCounter = ({
  adults = 1,
  children = 0,
  maxAdults = Infinity,
  maxTotal = Infinity,
  onChange,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);

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

  const getDisplayValue = () => {
    const parts = [];
    if (adults > 0) {
      parts.push(`${adults} ${adults === 1 ? 'adult' : 'adults'}`);
    }
    if (children > 0) {
      parts.push(`${children} ${children === 1 ? 'child' : 'children'}`);
    }
    return parts.length > 0 ? parts.join(', ') : 'Select travelers';
  };

  const handleAdultsChange = (increment) => {
    const newAdults = increment ? adults + 1 : adults - 1;
    if (newAdults >= 1 && newAdults <= maxAdults && newAdults + children <= maxTotal) {
      onChange({ adults: newAdults, children });
    }
  };

  const handleChildrenChange = (increment) => {
    const newChildren = increment ? children + 1 : children - 1;
    if (newChildren >= 0 && adults + newChildren <= maxTotal) {
      onChange({ adults, children: newChildren });
    }
  };

  return (
    <div className={styles.container} ref={dropdownRef}>
      {/* Dropdown Trigger */}
      <button
        type="button"
        className={styles.trigger}
        onClick={() => setIsOpen(!isOpen)}
      >
        <div className={styles.triggerContent}>
          <PeopleIcon className={styles.leftIcon} />
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
          {/* Adults Counter */}
          <div className={styles.counterRow}>
            <span className={styles.counterLabel}>Adults</span>
            <div className={styles.counterControls}>
              <button
                type="button"
                className={styles.counterButton}
                onClick={() => handleAdultsChange(false)}
                disabled={adults <= 1}
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M5 12h14" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                </svg>
              </button>
              <span className={styles.counterValue}>{adults}</span>
              <button
                type="button"
                className={styles.counterButton}
                onClick={() => handleAdultsChange(true)}
                disabled={adults >= maxAdults || adults + children >= maxTotal}
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M12 5v14M5 12h14" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                </svg>
              </button>
            </div>
          </div>

          {/* Children Counter */}
          <div className={styles.counterRow}>
            <span className={styles.counterLabel}>Children</span>
            <div className={styles.counterControls}>
              <button
                type="button"
                className={styles.counterButton}
                onClick={() => handleChildrenChange(false)}
                disabled={children <= 0}
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M5 12h14" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                </svg>
              </button>
              <span className={styles.counterValue}>{children}</span>
              <button
                type="button"
                className={styles.counterButton}
                onClick={() => handleChildrenChange(true)}
                disabled={adults + children >= maxTotal}
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M12 5v14M5 12h14" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TouristCounter;

