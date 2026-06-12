import { useEffect, useRef, useState } from 'react';
import { ReactComponent as ChevronIcon } from '../../assets/icons/downward.svg';

const CANCELLATION_REASONS = [
  { label: "Customer's Emergency", value: 'CUSTOMERS_EMERGENCY' },
  { label: 'Weather Conditions',   value: 'WEATHER_CONDITIONS' },
  { label: 'Operational Issue',    value: 'OPERATIONAL_ISSUE' },
  { label: 'Natural Disaster',     value: 'NATURAL_DISASTER' },
  { label: 'Health Concern',       value: 'HEALTH_CONCERN' },
  { label: 'Other',                value: 'OTHER' },
];

const CancelReasonSelect = ({ value, onChange }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [focusedIndex, setFocusedIndex] = useState(-1);
  const containerRef = useRef(null);
  const listRef = useRef(null);

  const handleToggle = () => setIsOpen((prev) => !prev);

  const handleSelect = (reason) => {
    onChange(reason.value);
    setIsOpen(false);
  };

  const handleKeyDown = (e) => {
    if (!isOpen) {
      if (e.key === 'Enter' || e.key === ' ' || e.key === 'ArrowDown') {
        e.preventDefault();
        setIsOpen(true);
        setFocusedIndex(0);
      }
      return;
    }

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setFocusedIndex((prev) => Math.min(prev + 1, CANCELLATION_REASONS.length - 1));
        break;
      case 'ArrowUp':
        e.preventDefault();
        setFocusedIndex((prev) => Math.max(prev - 1, 0));
        break;
      case 'Enter':
      case ' ':
        e.preventDefault();
        if (focusedIndex >= 0) handleSelect(CANCELLATION_REASONS[focusedIndex]);

        break;
      case 'Escape':
        e.preventDefault();
        setIsOpen(false);
        break;
      default:
        break;
    }
  };

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    if (isOpen && listRef.current && focusedIndex >= 0) {
      const item = listRef.current.children[focusedIndex];
      item?.scrollIntoView({ block: 'nearest' });
    }
  }, [focusedIndex, isOpen]);

  return (
    <div className="cancel-modal__select-wrapper" ref={containerRef}>
      <button
        type="button"
        className={`cancel-modal__select-trigger ${isOpen ? 'cancel-modal__select-trigger--open' : ''}`}
        onClick={handleToggle}
        onKeyDown={handleKeyDown}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        aria-label="Select cancellation reason"
      >
        <span className={value ? 'cancel-modal__select-text' : 'cancel-modal__select-placeholder'}>
          {value ? (CANCELLATION_REASONS.find((r) => r.value === value)?.label || value) : 'Select a reason'}
        </span>
        <ChevronIcon
          className={`cancel-modal__select-chevron ${isOpen ? 'cancel-modal__select-chevron--open' : ''}`}
        />
      </button>

      {isOpen && (
        <ul
          ref={listRef}
          className="cancel-modal__select-dropdown"
          role="listbox"
          aria-label="Cancellation reasons"
        >
          {CANCELLATION_REASONS.map((reason, index) => {
            const isSelected = value === reason.value;
            const isFocused = focusedIndex === index;

            return (
              <li
                key={reason.value}
                role="option"
                aria-selected={isSelected}
                className={`cancel-modal__select-option ${
                  isFocused ? 'cancel-modal__select-option--focused' : ''
                } ${isSelected ? 'cancel-modal__select-option--selected' : ''}`}
                onClick={() => handleSelect(reason)}
                onMouseEnter={() => setFocusedIndex(index)}
              >
                <span
                  className={`cancel-modal__radio ${isSelected ? 'cancel-modal__radio--checked' : ''}`}
                  aria-hidden="true"
                >
                  {isSelected && <span className="cancel-modal__radio-dot" />}
                </span>
                <span className="cancel-modal__option-text">{reason.label}</span>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
};

export default CancelReasonSelect;

