import { useEffect, useRef, useState } from 'react';
import { ReactComponent as LocationIcon } from '../../assets/icons/Location.svg';
import { ReactComponent as StarIcon }     from '../../assets/icons/star.svg';
import { ReactComponent as CalendarIcon } from '../../assets/icons/Calendar.svg';
import { ReactComponent as PersonIcon }   from '../../assets/icons/Person.svg';
import { ReactComponent as FoodIcon }     from '../../assets/icons/food.svg';
import { ReactComponent as DownwardIcon } from '../../assets/icons/downward.svg';
import crossIcon from '../../assets/icons/Cross.svg';
import { formatCalendarDate } from '../../utils/dateUtils';

/* ── Reusable dropdown row ──────────────────────────────────── */
const DropdownRow = ({ icon: Icon, value, options, onSelect }) => {
  const [open, setOpen] = useState(false);
  const rootRef = useRef(null);

  useEffect(() => {
    const handler = (e) => {
      if (rootRef.current && !rootRef.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const selectedLabel = options.find((o) => o.value === value)?.label ?? '';

  return (
    <div className="relative w-full" ref={rootRef}>
      <button
        type="button"
        onClick={() => setOpen((p) => !p)}
        className={`flex h-14 w-full items-center gap-2 rounded-lg border px-3 text-left transition ${
          open ? 'border-[#027EAC]' : 'border-[#D3E1ED]'
        }`}
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
        <div className="absolute left-0 right-0 top-[calc(100%+6px)] z-30 max-h-56 overflow-y-auto rounded-lg border border-[#D3E1ED] bg-white p-1 shadow-[0px_8px_20px_rgba(11,56,87,0.12)]">
          {options.map((opt) => {
            const active = opt.value === value;
            return (
              <button
                key={String(opt.value)}
                type="button"
                onClick={() => { onSelect(opt.value); setOpen(false); }}
                className={`flex w-full items-center rounded-md px-3 py-2 font-nunito text-[14px] leading-6 transition ${
                  active ? 'bg-[#E7F9FF] text-[#027EAC]' : 'text-[#0B3857] hover:bg-[#F3F8FB]'
                }`}
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

/* ── Helpers ────────────────────────────────────────────────── */
const fmtDate = (iso) => {
  if (!iso) return '—';
  return formatCalendarDate(iso, { month: 'short', day: 'numeric' });
};

/* ── Personal details row ───────────────────────────────────── */
const CustomerFields = ({ index, total, adultsCount, customer, onChange }) => {
  const isChild = index >= adultsCount;
  const typeLabel = isChild ? `Child ${index - adultsCount + 1}` : `Adult ${index + 1}`;
  return (
  <div className="flex flex-col gap-3">
    {total > 1 && (
      <h3 className="font-nunito text-[16px] font-bold text-[#0B3857]">
        Personal details ({typeLabel})
      </h3>
    )}
    {index === 0 && total === 1 && (
      <h3 className="font-nunito text-[16px] font-bold text-[#0B3857]">Personal details</h3>
    )}
    <div className="flex gap-4">
      <div className="flex flex-1 flex-col gap-1">
        <label className="font-nunito text-[14px] font-extrabold text-[#0B3857]">First name</label>
        <div className="flex h-14 items-center rounded-lg border border-[#D3E1ED] px-4 focus-within:border-[#027EAC] transition">
          <input
            type="text"
            value={customer.firstName}
            onChange={(e) => onChange(index, 'firstName', e.target.value)}
            className="w-full bg-transparent font-nunito text-[14px] text-[#0B3857] focus:outline-none"
          />
        </div>
        <span className="font-nunito text-[12px] text-[#677883]">e.g. Johnson</span>
      </div>
      <div className="flex flex-1 flex-col gap-1">
        <label className="font-nunito text-[14px] font-extrabold text-[#0B3857]">Last name</label>
        <div className="flex h-14 items-center rounded-lg border border-[#D3E1ED] px-4 focus-within:border-[#027EAC] transition">
          <input
            type="text"
            value={customer.lastName}
            onChange={(e) => onChange(index, 'lastName', e.target.value)}
            className="w-full bg-transparent font-nunito text-[14px] text-[#0B3857] focus:outline-none"
          />
        </div>
        <span className="font-nunito text-[12px] text-[#677883]">e.g. Doe</span>
      </div>
    </div>
  </div>
  );
};

/* ── Main modal ─────────────────────────────────────────────── */
const EditTourModal = ({ booking, onClose, onSave }) => {
  /* Pre-fill customers array from booking */
  const initCustomers = () => {
    const names = (booking.traveler || '').split(',').map((n) => n.trim());
    const total = (booking.travelerCount ?? 1) + (booking.childrenCount ?? 0);
    return Array.from({ length: total }, (_, i) => {
      const parts = (names[i] || '').split(' ');
      return { firstName: parts[0] || '', lastName: parts.slice(1).join(' ') || '' };
    });
  };

  const [customers, setCustomers] = useState(initCustomers);
  const [adults,    setAdults]    = useState(booking.travelerCount ?? 1);
  const [children,  setChildren]  = useState(booking.childrenCount ?? 0);
  const [mealPlan,  setMealPlan]  = useState(booking.mealPlan || '');
  const [selectedDate,     setSelectedDate]     = useState(booking.startDate || '');
  const [selectedDuration, setSelectedDuration] = useState(booking.duration ?? 7);

  /* Price per person derived from original booking */
  const pricePerPerson = Math.round(
    (booking.totalPrice ?? 0) / ((booking.travelerCount || 1) + (booking.childrenCount ?? 0))
  );

  /* Reactive total — updates as adults + children change */
  const totalPrice = pricePerPerson * (adults + children);

  /* Sync customers array when adults or children count changes */
  useEffect(() => {
    setCustomers((prev) => {
      const total = adults + children;
      const next = [...prev];
      while (next.length < total) next.push({ firstName: '', lastName: '' });
      return next.slice(0, total);
    });
  }, [adults, children]);

  const updateCustomer = (idx, field, value) => {
    setCustomers((prev) => {
      const next = [...prev];
      next[idx] = { ...next[idx], [field]: value };
      return next;
    });
  };

  const maxGuests = booking.maxGuests ?? 8;
  const maxChildren = Math.max(0, maxGuests - adults);

  const tourDates     = booking.tourDates     || (booking.startDate ? [booking.startDate] : []);
  const tourDurations = booking.tourDurations || [booking.duration ?? 7];
  const dateDurKey    = `${selectedDate}|${selectedDuration}`;

  const dateOptions = tourDates.flatMap((d) =>
    tourDurations.map((dur) => ({
      value: `${d}|${dur}`,
      label: `${fmtDate(d)}, ${dur} days`,
    }))
  );
  if (dateOptions.length === 0) {
    dateOptions.push({ value: dateDurKey, label: `${fmtDate(selectedDate)}, ${selectedDuration} days` });
  }
  const adultsOptions = Array.from({ length: maxGuests }, (_, i) => i + 1).map((n) => ({
    value: n,
    label: `${n} ${n === 1 ? 'adult' : 'adults'}`,
  }));
  const childrenOptions = Array.from({ length: maxChildren + 1 }, (_, i) => ({
    value: i,
    label: i === 0 ? '0 children' : `${i} ${i === 1 ? 'child' : 'children'}`,
  }));
  const mealOptions = [
    'Breakfast (BB)', 'Half-board (HB)', 'Full-board (FB)', 'All inclusive (AI)', 'No meal plan',
  ].map((m) => ({ value: m, label: m }));

  /* Close on Escape */
  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [onClose]);

  const handleSave = () => {
    onSave({
      ...booking,
      traveler:      customers.map((c) => `${c.firstName} ${c.lastName}`.trim()).join(', '),
      travelerCount: adults,
      childrenCount: children,
      startDate:     selectedDate,
      duration:      selectedDuration,
      mealPlan,
      totalPrice,
    });
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      style={{ background: 'rgba(11, 56, 87, 0.4)' }}
      onClick={onClose}
    >
      <div
        className="relative flex w-full flex-col gap-6 rounded-2xl bg-white p-6 overflow-y-auto"
        style={{ maxWidth: '544px', maxHeight: 'calc(100vh - 32px)' }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* ── Header ── */}
        <div className="flex flex-col gap-1">
          <div className="flex items-center justify-between gap-2">
            <div className="flex min-w-0 flex-1 items-center gap-4">
              <h2 className="truncate font-nunito text-[20px] font-bold leading-8 text-[#0B3857]">
                {booking.name}
              </h2>
              <div className="flex shrink-0 items-center gap-1">
                <StarIcon className="h-4 w-4" />
                <span className="font-nunito text-[12px] text-[#0B3857]">5.0</span>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="flex h-6 w-6 shrink-0 items-center justify-center"
              aria-label="Close"
            >
              <img src={crossIcon} alt="Close" className="h-6 w-6" />
            </button>
          </div>
          <div className="flex items-center gap-1">
            <LocationIcon className="h-4 w-4 shrink-0 text-[#677883]" />
            <span className="font-nunito text-[12px] text-[#677883]">{booking.location}</span>
          </div>
        </div>

        {/* ── Personal details — one block per customer ── */}
        {customers.map((customer, idx) => (
          <CustomerFields
            key={idx}
            index={idx}
            total={adults + children}
            adultsCount={adults}
            customer={customer}
            onChange={updateCustomer}
          />
        ))}

        {/* ── Tour details ── */}
        <div className="flex flex-col gap-4">
          <h3 className="font-nunito text-[16px] font-bold text-[#0B3857]">Tour details</h3>

          <DropdownRow
            icon={CalendarIcon}
            value={dateDurKey}
            options={dateOptions}
            onSelect={(key) => {
              const [d, dur] = key.split('|');
              setSelectedDate(d);
              setSelectedDuration(parseInt(dur, 10));
            }}
          />

          <DropdownRow
            icon={PersonIcon}
            value={adults}
            options={adultsOptions}
            onSelect={(val) => {
              setAdults(val);
              setChildren((prev) => Math.min(prev, maxGuests - val));
            }}
          />

          <DropdownRow
            icon={PersonIcon}
            value={children}
            options={childrenOptions}
            onSelect={setChildren}
          />

          <DropdownRow
            icon={FoodIcon}
            value={mealPlan}
            options={mealOptions}
            onSelect={setMealPlan}
          />
        </div>

        {/* ── Total price ── */}
        <div className="flex justify-end">
          <span className="font-nunito text-[14px] text-[#0B3857]">
            Total price:{' '}
            <strong className="text-[16px]">${totalPrice != null ? totalPrice.toLocaleString() : '—'}</strong>
          </span>
        </div>

        {/* ── Save button ── */}
        <button
          type="button"
          onClick={handleSave}
          className="font-nunito text-white transition hover:opacity-90"
          style={{
            width: '100%',
            maxWidth: '496px',
            height: '40px',
            gap: '4px',
            padding: '8px 16px',
            borderRadius: '8px',
            background: '#027EAC',
            fontWeight: 700,
            fontSize: '14px',
            lineHeight: '24px',
            letterSpacing: '0px',
            textAlign: 'center',
          }}
        >
          Save changes
        </button>
      </div>
    </div>
  );
};

export default EditTourModal;
