import { useEffect, useMemo, useRef, useState } from 'react';
import crossIcon from '../../assets/icons/Cross.svg';
import { ReactComponent as LocationIcon } from '../../assets/icons/Location.svg';
import { ReactComponent as StarIcon } from '../../assets/icons/star.svg';
import { ReactComponent as CalendarIcon } from '../../assets/icons/Calendar.svg';
import { ReactComponent as PeopleIcon } from '../../assets/icons/People.svg';
import { ReactComponent as FoodIcon } from '../../assets/icons/food.svg';
import { ReactComponent as DownwardIcon } from '../../assets/icons/downward.svg';

const fmt = (iso) => {
  if (!iso) return '';
  const [year, month, day] = iso.split('T')[0].split('-').map(Number);
  return new Date(year, month - 1, day).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
};

const DropdownRow = ({ icon: Icon, value, options, onSelect, label }) => {
  const [open, setOpen] = useState(false);
  const rootRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (rootRef.current && !rootRef.current.contains(event.target)) {
        setOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const selectedLabel = useMemo(() => {
    const match = options.find((opt) => opt.value === value);
    return match?.label || label;
  }, [label, options, value]);

  return (
    <div className="relative w-full" ref={rootRef}>
      <button
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        className={`flex h-14 w-full items-center gap-2 rounded-lg border px-3 text-left transition ${
          open ? 'border-[#027EAC]' : 'border-[#D3E1ED]'
        }`}
        aria-haspopup="listbox"
        aria-expanded={open}
      >
        <Icon className="h-6 w-6 shrink-0 text-[#0B3857]" />
        <span className="flex-1 truncate font-nunito text-[14px] leading-6 text-[#0B3857]">
          {selectedLabel}
        </span>
        <DownwardIcon
          className={`h-6 w-6 shrink-0 text-[#0B3857] transition-transform ${open ? 'rotate-180' : ''}`}
        />
      </button>

      {open && (
        <div className="absolute left-0 right-0 top-[calc(100%+8px)] z-20 max-h-56 overflow-y-auto rounded-lg border border-[#D3E1ED] bg-white p-1 shadow-[0px_8px_20px_rgba(11,56,87,0.12)]">
          {options.map((opt) => {
            const active = opt.value === value;
            return (
              <button
                key={String(opt.value)}
                type="button"
                onClick={() => {
                  onSelect(opt.value);
                  setOpen(false);
                }}
                className={`flex w-full items-center rounded-md px-3 py-2 font-nunito text-[14px] leading-6 transition ${
                  active
                    ? 'bg-[#E7F9FF] text-[#027EAC]'
                    : 'text-[#0B3857] hover:bg-[#F3F8FB]'
                }`}
                role="option"
                aria-selected={active}
              >
                {opt.label}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
};

const BookingModal = ({ tour, user, onClose, onConfirm, initialSelection = {} }) => {
  const nameParts = (user?.userName || '').split(' ');
  const today = new Date().toISOString().split('T')[0];
  const initialDateIdx = useMemo(() => {
    if (!Array.isArray(tour?.dates) || tour.dates.length === 0) return 0;
    if (initialSelection.selectedDate) {
      const idx = tour.dates.findIndex((d) => d === initialSelection.selectedDate);
      if (idx >= 0) return idx;
    }
    // Default to first future date
    const futureIdx = tour.dates.findIndex((d) => d > today);
    return futureIdx >= 0 ? futureIdx : 0;
  }, [tour?.dates, initialSelection.selectedDate, today]);

  const initialDuration = initialSelection.selectedDurations?.[0] ?? tour.durations?.[0] ?? 7;
  const maxAdults = tour.guestQuantity?.adultsMaxValue ?? tour.maxGuests ?? 8;
  const maxTotal = tour.guestQuantity?.totalMaxValue ?? tour.maxGuests ?? 8;
  const initialAdults = Math.max(1, Math.min(maxAdults, initialSelection.tourists?.adults ?? 1));
  const initialChildren = Math.max(0, initialSelection.tourists?.children ?? 0);
  const initialMealPlan = initialSelection.selectedMealPlans?.[0] ?? tour.mealPlans?.[0] ?? '';
  const initialDateDurKey = `${initialDateIdx}|${initialDuration}`;

  const buildInitialCustomers = (adultCount, childCount) => {
    const firstName = nameParts[0] || '';
    const lastName = nameParts.slice(1).join(' ') || '';
    const total = adultCount + childCount;
    return Array.from({ length: total }, (_, idx) => (
      idx === 0
        ? { firstName, lastName }
        : { firstName: '', lastName: '' }
    ));
  };

  const [dateDurKey, setDateDurKey] = useState(initialDateDurKey);
  const [adults, setAdults] = useState(initialAdults);
  const [children, setChildren] = useState(initialChildren);
  const [mealPlan, setMealPlan] = useState(initialMealPlan);
  const [customers, setCustomers] = useState(() => buildInitialCustomers(initialAdults, initialChildren));

  const [dateIdx, duration] = useMemo(() => {
    const [di, dur] = dateDurKey.split('|');
    return [parseInt(di, 10), parseInt(dur, 10)];
  }, [dateDurKey]);

  useEffect(() => {
    setCustomers((prev) => {
      const total = adults + children;
      const next = [...prev];
      while (next.length < total) next.push({ firstName: '', lastName: '' });
      return next.slice(0, total);
    });
  }, [adults, children]);

  const selectedDate = tour.dates?.[dateIdx];
  const totalPrice   = tour.price * (adults + children);

  const dateOptions = (tour.dates || []).flatMap((d, i) =>
    (tour.durations || [7]).map((dur) => ({
      value: `${i}|${dur}`,
      label: `${fmt(d)}, ${dur} days`,
    }))
  );

  const adultsOptions = Array.from({ length: maxAdults }, (_, i) => i + 1).map((n) => ({
    value: n,
    label: `${n} ${n === 1 ? 'adult' : 'adults'}`,
  }));

  const maxChildren = Math.max(0, maxTotal - adults);
  const childrenOptions = Array.from({ length: maxChildren + 1 }, (_, i) => ({
    value: i,
    label: i === 0 ? '0 children' : `${i} ${i === 1 ? 'child' : 'children'}`,
  }));

  const mealOptions = ((tour.mealPlans || []).length > 0
    ? tour.mealPlans
    : ['No meal plan']).map((plan) => ({
      value: plan,
      label: plan,
    }));

  const updateCustomer = (idx, field, value) => {
    setCustomers((prev) => {
      const next = [...prev];
      next[idx] = { ...next[idx], [field]: value };
      return next;
    });
  };

  const [attempted, setAttempted] = useState(false);
  const allDetailsFilled = customers.every(
    (c) => c.firstName.trim() !== '' && c.lastName.trim() !== ''
  );

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-[#42424280] p-4"
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="flex w-full max-w-[544px] flex-col gap-8 rounded-[12px] bg-white p-6 max-h-[calc(100vh-32px)] overflow-y-auto">
        <div className="flex w-full shrink-0 flex-col gap-1">
          <div className="flex w-full items-center justify-between gap-2">
            <div className="flex min-w-0 flex-1 items-center gap-[18px]">
              <h2 className="truncate font-nunito text-[18px] font-bold leading-10 text-[#0B3857] sm:text-[24px]">
                {tour.name}
              </h2>
              <div className="flex shrink-0 items-center gap-1">
                <StarIcon className="h-4 w-4" />
                <span className="font-nunito text-[12px] leading-4 text-[#0B3857]">
                  {tour.rating?.toFixed(1)}
                </span>
              </div>
            </div>

            <button
              type="button"
              onClick={onClose}
              className="flex h-6 w-6 shrink-0 items-center justify-center"
              aria-label="Close booking modal"
            >
              <img src={crossIcon} alt="Close" className="h-6 w-6" />
            </button>
          </div>
          <div className="flex items-center gap-1">
            <LocationIcon className="h-4 w-4 shrink-0 text-[#677883]" />
            <span className="truncate font-nunito text-[12px] leading-4 text-[#677883]">
              {tour.location}
            </span>
          </div>
        </div>
        <div className="flex flex-col gap-6">
          <div className="flex flex-col gap-4">
            {customers.map((customer, idx) => (
              <div key={`customer-${idx}`} className="flex flex-col gap-2">
                <h3 className="font-nunito text-[18px] font-bold leading-8 text-[#0B3857]">
                  {idx < adults
                    ? (adults + children > 1 ? `Personal details (Customer ${idx + 1})` : 'Personal details')
                    : `Personal details (Child ${idx - adults + 1})`
                  }
                </h3>

                <div className="flex flex-col gap-4 min-[480px]:flex-row">
                  <div className="flex flex-1 flex-col gap-1">
                    <label className="font-nunito text-[14px] font-extrabold leading-6 text-[#0B3857]">
                      First name
                    </label>
                    <div className={`flex h-14 flex-col justify-center rounded-lg border px-4 focus-within:border-[#027EAC] ${attempted && !customer.firstName.trim() ? 'border-red-500' : 'border-[#D3E1ED]'}`}>
                      <input
                        type="text"
                        value={customer.firstName}
                        onChange={(e) => updateCustomer(idx, 'firstName', e.target.value)}
                        className="w-full bg-transparent font-nunito text-[14px] leading-6 text-[#0B3857] focus:outline-none"
                      />
                    </div>
                    {attempted && !customer.firstName.trim()
                      ? <span className="font-nunito text-[12px] leading-4 text-red-500">First name is required</span>
                      : <span className="font-nunito text-[12px] leading-4 text-[#677883]">e.g. Johnson</span>
                    }
                  </div>

                  <div className="flex flex-1 flex-col gap-1">
                    <label className="font-nunito text-[14px] font-extrabold leading-6 text-[#0B3857]">
                      Last name
                    </label>
                    <div className={`flex h-14 flex-col justify-center rounded-lg border px-4 focus-within:border-[#027EAC] ${attempted && !customer.lastName.trim() ? 'border-red-500' : 'border-[#D3E1ED]'}`}>
                      <input
                        type="text"
                        value={customer.lastName}
                        onChange={(e) => updateCustomer(idx, 'lastName', e.target.value)}
                        className="w-full bg-transparent font-nunito text-[14px] leading-6 text-[#0B3857] focus:outline-none"
                      />
                    </div>
                    {attempted && !customer.lastName.trim()
                      ? <span className="font-nunito text-[12px] leading-4 text-red-500">Last name is required</span>
                      : <span className="font-nunito text-[12px] leading-4 text-[#677883]">e.g. Doe</span>
                    }
                  </div>
                </div>
              </div>
            ))}
          </div>

          <div className="flex flex-col gap-4">
            <h3 className="font-nunito text-[18px] font-bold leading-8 text-[#0B3857]">
              Tour details
            </h3>

            <div className="flex flex-col gap-3 rounded-[12px]">
              <DropdownRow
                icon={CalendarIcon}
                value={dateDurKey}
                options={dateOptions.length > 0
                  ? dateOptions
                  : [{ value: initialDateDurKey, label: selectedDate ? `${fmt(selectedDate)}, ${duration} days` : `Jan 4, ${duration} days` }]
                }
                onSelect={(key) => setDateDurKey(key)}
                label="Select date & duration"
              />

              <DropdownRow
                icon={PeopleIcon}
                value={adults}
                options={adultsOptions}
                onSelect={(nextAdults) => {
                  setAdults(nextAdults);
                  setChildren((prev) => Math.min(prev, maxTotal - nextAdults));
                }}
                label="Select adults"
              />

              <DropdownRow
                icon={PeopleIcon}
                value={children}
                options={childrenOptions}
                onSelect={(nextChildren) => setChildren(nextChildren)}
                label="Select children"
              />

              <DropdownRow
                icon={FoodIcon}
                value={mealPlan || 'No meal plan'}
                options={mealOptions}
                onSelect={(nextMeal) => setMealPlan(nextMeal === 'No meal plan' ? '' : nextMeal)}
                label="Select meal plan"
              />

              <div className="flex w-full items-center justify-end gap-2">
                <span className="font-nunito text-[14px] font-extrabold leading-6 text-[#0B3857]">
                  Total price:
                </span>
                <span className="font-nunito text-[14px] font-extrabold leading-6 text-[#0B3857]">
                  ${totalPrice.toLocaleString()}
                </span>
              </div>
            </div>
          </div>
        </div>
        <button
          type="button"
          onClick={() => {
            setAttempted(true);
            if (!allDetailsFilled) return;
            onConfirm({
              tour,
              selectedDate,
              duration,
              mealPlan,
              adults,
              children,
              customers,
              totalPrice,
            });
          }}
          className="flex h-10 w-full shrink-0 items-center justify-center gap-1 rounded-lg bg-[#027EAC] px-4 py-2 font-nunito text-[14px] font-bold leading-6 text-white transition hover:bg-[#025f82] active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Book the tour
        </button>
      </div>
    </div>
  );
};

export default BookingModal;
