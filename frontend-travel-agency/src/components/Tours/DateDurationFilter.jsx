import { useMemo, useState } from 'react';
import { ReactComponent as CalendarIcon } from '../../assets/icons/Calendar.svg';
import { ReactComponent as CheckboxIcon } from '../../assets/icons/checkbox.svg';
import { ReactComponent as DownwardIcon } from '../../assets/icons/downward.svg';

const DURATION_OPTIONS = [
  { value: 3, label: '1-3 days' },
  { value: 7, label: '4-7 days' },
  { value: 12, label: '8-12 days' },
  { value: 13, label: '13+ days' },
];

const WEEK_DAYS = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];

const toLocalIsoDate = (year, month, day) => {
  const mm = String(month + 1).padStart(2, '0');
  const dd = String(day).padStart(2, '0');
  return `${year}-${mm}-${dd}`;
};

const normalizeMonthDate = (value) => {
  if (!value) return new Date(new Date().getFullYear(), 0, 1);
  const parsed = new Date(`${value}T00:00:00`);
  return Number.isNaN(parsed.getTime()) ? new Date(new Date().getFullYear(), 0, 1) : parsed;
};

const normalizeRange = (dates = []) => [...dates].filter(Boolean).sort();

const isBetweenRange = (value, dates) => {
  if (dates.length !== 2) return false;
  return value > dates[0] && value < dates[1];
};

const buildCalendarDays = (monthDate) => {
  const year = monthDate.getFullYear();
  const month = monthDate.getMonth();
  const firstDay = new Date(year, month, 1);
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const startOffset = (firstDay.getDay() + 6) % 7;
  const cells = [];

  for (let index = 0; index < startOffset; index += 1) {
    cells.push({ key: `empty-${index}`, label: '', value: null, muted: true });
  }

  for (let day = 1; day <= daysInMonth; day += 1) {
    const isoValue = toLocalIsoDate(year, month, day);
    cells.push({ key: isoValue, label: String(day), value: isoValue, muted: false });
  }

  return cells;
};

const DateDurationFilter = ({ startDates = [], durations, onDatesChange, onDurationsChange }) => {
  const [open, setOpen] = useState(false);
  const [visibleMonth, setVisibleMonth] = useState(() => normalizeMonthDate(startDates[0]));

  const toggle = (val) =>
    onDurationsChange(durations.includes(val) ? [] : [val]);

  const handleMonthShift = (offset) => {
    setVisibleMonth((current) => new Date(current.getFullYear(), current.getMonth() + offset, 1));
  };

  const toggleDate = (value) => {
    const currentDates = normalizeRange(startDates);

    if (currentDates.length === 0) {
      onDatesChange([value]);
      return;
    }

    if (currentDates.length === 1) {
      if (currentDates[0] === value) {
        onDatesChange([]);
        return;
      }

      onDatesChange(normalizeRange([currentDates[0], value]));
      return;
    }

    onDatesChange([value]);
  };

  const monthTitle = useMemo(
    () => visibleMonth.toLocaleDateString('en-US', { month: 'long', year: 'numeric' }),
    [visibleMonth]
  );

  const normalizedStartDates = useMemo(() => normalizeRange(startDates), [startDates]);
  const calendarDays = useMemo(() => buildCalendarDays(visibleMonth), [visibleMonth]);

  const datePart = normalizedStartDates.length
    ? normalizedStartDates
      .map((date) => new Date(`${date}T00:00:00`).toLocaleDateString('en-GB', {
        day: '2-digit', month: 'short', year: 'numeric',
      }))
      .join(' - ')
    : 'Any start date';

  const durationPart = durations.length
    ? DURATION_OPTIONS.filter((o) => durations.includes(o.value)).map((o) => o.label).join(', ')
    : 'any duration';

  const summary = `${datePart}, ${durationPart}`;
  const hasDateRange = normalizedStartDates.length === 2;
  const isActive = normalizedStartDates.length > 0 || durations.length > 0;

  return (
    <div className="relative flex-1 min-w-[200px] xl:w-[256px]">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className={`flex h-[56px] w-full items-center justify-between gap-3 rounded-lg border bg-white px-3 text-left transition hover:shadow-sm focus:outline-none ${
          open || isActive ? 'border-[#027EAC] shadow-sm' : 'border-[#D3E1ED] hover:border-[#027EAC]/50'
        }`}
      >
        <div className="flex min-w-0 flex-1 items-center gap-2">
          <CalendarIcon className="h-6 w-6 shrink-0 text-[#0B3857]" />
          <span className="block h-6 truncate font-nunito text-sm font-normal leading-6 text-[#0B3857]">
            {summary}
          </span>
        </div>
        <DownwardIcon
          className={`shrink-0 transition-transform duration-200 ${open ? 'rotate-180' : ''}`}
        />
      </button>

      {open && (
        <>
          <div className="fixed inset-0 z-20" onClick={() => setOpen(false)} />
          <div className="absolute left-0 z-30 mt-2 h-[334px] w-[496px] rounded-[6px] border border-[#D3E1ED] bg-white p-4 shadow-[0px_2px_10px_6px_rgba(2,126,172,0.2)]">
            <div className="flex h-full gap-4">
              <div className="flex h-[302px] w-[296px] flex-col gap-4 border-r border-[#D3E1ED] pr-6">
                <label className="h-8 w-[85px] font-nunito text-[18px] font-bold leading-8 text-[#0B3857]">
                  Start date
                </label>
                <div className="flex h-6 w-[272px] items-center justify-between">
                  <button
                    type="button"
                    onClick={() => handleMonthShift(-1)}
                    className="flex h-6 w-6 items-center justify-center text-[#0B3857]"
                  >
                    <DownwardIcon className="h-[6px] w-3 rotate-90 text-[#A2AEB9]" />
                  </button>
                  <p className="h-6 font-nunito text-[14px] font-extrabold leading-6 text-[#0B3857]">
                    {monthTitle}
                  </p>
                  <button
                    type="button"
                    onClick={() => handleMonthShift(1)}
                    className="flex h-6 w-6 items-center justify-center text-[#0B3857]"
                  >
                    <DownwardIcon className="h-[6px] w-3 -rotate-90 text-[#0B3857]" />
                  </button>
                </div>
                <div className="flex w-[272px] flex-col gap-2">
                  <div className="grid h-6 w-[272px] grid-cols-7 items-center text-center font-nunito text-[14px] font-normal leading-6 text-[#0B3857]">
                    {WEEK_DAYS.map((day) => (
                      <span key={day}>{day}</span>
                    ))}
                  </div>
                  <div className="h-px w-[272px] bg-[#D3E1ED]" />
                  <div className="grid h-[214px] w-[272px] grid-cols-7 gap-y-2 text-center font-nunito text-[14px] leading-8">
                    {calendarDays.map((day) => {
                      const isSelected = normalizedStartDates.includes(day.value);
                      const isInRange = day.value && isBetweenRange(day.value, normalizedStartDates);
                      const isRangeStart = hasDateRange && day.value === normalizedStartDates[0];
                      const isRangeEnd = hasDateRange && day.value === normalizedStartDates[1];

                      if (!day.value) {
                        return <span key={day.key} className="h-8 w-8" />;
                      }

                      return (
                        <div key={day.key} className="relative flex h-8 w-full items-center justify-center">
                          {(isInRange || isRangeStart || isRangeEnd) && (
                            <span
                              className={`absolute inset-y-0 bg-[#EDF4FA] ${
                                isInRange
                                  ? 'left-0 right-0'
                                  : isRangeStart
                                    ? 'left-1/2 right-0'
                                    : 'left-0 right-1/2'
                              }`}
                            />
                          )}
                          <button
                            type="button"
                            onClick={() => toggleDate(day.value)}
                            className={`relative z-10 flex h-8 w-8 items-center justify-center font-nunito text-[14px] leading-8 ${
                              isSelected
                                ? 'rounded-full bg-[#027EAC] text-white'
                                : 'text-[#0B3857] hover:rounded-full hover:bg-[#EDF4FA]'
                            }`}
                          >
                            {day.label}
                          </button>
                        </div>
                      );
                    })}
                  </div>
                </div>
              </div>

              <div className="flex h-[208px] w-[152px] flex-col gap-4">
                <div className="flex h-8 w-[152px] items-center gap-[10px] px-2">
                  <p className="h-8 w-[74px] font-nunito text-[18px] font-bold leading-8 text-[#0B3857]">
                    Duration
                  </p>
                </div>
                <div className="h-[160px] w-[152px] rounded-[6px] bg-white">
                  {DURATION_OPTIONS.map((option) => (
                    <label
                      key={option.value}
                      className={`flex h-10 w-[152px] cursor-pointer items-center gap-2 px-2 transition ${
                        durations.includes(option.value) ? 'bg-[#EDF4FA]' : 'bg-white'
                      } font-nunito text-sm`}
                    >
                      <input
                        type="checkbox"
                        checked={durations.includes(option.value)}
                        onChange={() => toggle(option.value)}
                        className="sr-only"
                      />
                      <span
                        className={`flex h-6 w-6 items-center justify-center rounded-[4px] border border-[#027EAC] ${
                          durations.includes(option.value) ? 'bg-[#027EAC]' : 'bg-white'
                        }`}
                      >
                        {durations.includes(option.value) && <CheckboxIcon className="h-6 w-6" />}
                      </span>
                      <span className="h-6 font-nunito text-[14px] font-normal leading-6 text-[#0B3857]">
                        {option.label}
                      </span>
                    </label>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default DateDurationFilter;
