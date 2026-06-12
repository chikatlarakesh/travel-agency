// MealPlanSelector Component
// Single-select dropdown with checkboxes for meal plans
// Uses temporary state - changes are applied only when "Update" is clicked
// Used in TourDetail booking panel

import React, { useState, useRef, useEffect } from 'react';
import styles from './MealPlanSelector.module.css';
import { ReactComponent as FoodIcon } from '../../assets/icons/food.svg';
import { ReactComponent as CheckboxIcon } from '../../assets/icons/checkbox.svg';

const MealPlanSelector = ({ availableMealPlans = [], selectedMealPlans = [], onChange }) => {
  const [isOpen, setIsOpen] = useState(false);
  // Temporary selection state - holds changes before "Update" is clicked
  const [tempSelection, setTempSelection] = useState(selectedMealPlans);
  const dropdownRef = useRef(null);

  // ALL meal plan options as per acceptance criteria
  const allMealPlanOptions = [
    { value: 'AI', label: 'All inclusive (AI)' },
    { value: 'BB', label: 'Breakfast (BB)' },
    { value: 'FB', label: 'Full-board (HB)' },
    { value: 'HB', label: 'Half-board (HB)' },
  ];

  // Map provided values to proper labels using allMealPlanOptions; fall back to all options if none provided
  const mealPlans = availableMealPlans.length > 0
    ? availableMealPlans.map((plan) => {
        const match = allMealPlanOptions.find((o) => o.value === plan.value || o.value === plan.label);
        return match ? match : plan;
      })
    : allMealPlanOptions;

  // Sync temp selection with prop when it changes externally
  useEffect(() => {
    setTempSelection(selectedMealPlans);
  }, [selectedMealPlans]);

  // Close dropdown when clicking outside (without applying changes)
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
        // Reset temp selection on close without update
        setTempSelection(selectedMealPlans);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen, selectedMealPlans]);

  const getDisplayValue = () => {
    if (selectedMealPlans.length === 0) {
      return 'Select meal plan';
    }
    // Single selection - show the selected meal plan label
    const plan = mealPlans.find(
      (m) => selectedMealPlans.includes(m.value) || selectedMealPlans.includes(m.label)
    );
    return plan ? plan.label : selectedMealPlans[0];
  };

  // Single-select toggle: only one meal plan can be selected at a time, applies immediately
  const handleSingleSelect = (mealPlan) => {
    const value = mealPlan.value || mealPlan.label;
    const newSelection = (tempSelection.includes(value) || tempSelection.includes(mealPlan.label))
      ? []
      : [value];
    setTempSelection(newSelection);
    onChange(newSelection);
    setIsOpen(false);
  };

  const isSelected = (mealPlan) => {
    return (
      tempSelection.includes(mealPlan.value) ||
      tempSelection.includes(mealPlan.label)
    );
  };

  const handleToggleDropdown = () => {
    if (!isOpen) {
      // Sync temp with current selection when opening
      setTempSelection(selectedMealPlans);
    }
    setIsOpen(!isOpen);
  };

  return (
    <div className={styles.container} ref={dropdownRef}>
      {/* Dropdown Trigger */}
      <button
        type="button"
        className={styles.trigger}
        onClick={handleToggleDropdown}
      >
        <div className={styles.triggerContent}>
          <FoodIcon className={styles.leftIcon} />
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
          <div className={styles.optionsList}>
            {mealPlans.map((mealPlan, index) => {
              const checked = isSelected(mealPlan);
              return (
                <label key={index} className={`${styles.optionRow} ${checked ? styles.optionRowSelected : ''}`}>
                  <input
                    type="checkbox"
                    checked={checked}
                    onChange={() => handleSingleSelect(mealPlan)}
                    className={styles.optionInput}
                  />
                  <span className={`${styles.optionMark} ${checked ? styles.optionMarkChecked : ''}`}>
                    {checked && <CheckboxIcon className={styles.optionCheckIcon} />}
                  </span>
                  <span className={styles.optionText}>{mealPlan.label}</span>
                </label>
              );
            })}
          </div>

        </div>
      )}
    </div>
  );
};

export default MealPlanSelector;

