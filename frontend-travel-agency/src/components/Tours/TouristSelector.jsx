import { useState } from 'react';
import { ReactComponent as DownwardIcon } from '../../assets/icons/downward.svg';
import { ReactComponent as PeopleIcon } from '../../assets/icons/People.svg';

const Counter = ({ label, value, onDec, onInc, min = 0 }) => (
  <div className="flex h-10 w-[176px] justify-between py-1">
    <div className="flex h-8 w-[176px] items-center justify-between">
      <div className="flex h-6 w-[81px] items-center gap-2">
        <p className="h-6 min-w-[42px] text-center font-nunito text-[14px] font-normal leading-6 text-[#0B3857]">
          {label}
        </p>
      </div>
      <div className="flex h-8 w-[120px] items-center gap-2">
        <button
          type="button"
          onClick={onDec}
          disabled={value <= min}
          className="flex h-8 w-8 items-center justify-center rounded-full border border-[#A2AEB9] bg-white px-2 py-1 font-nunito text-[#0B3857] disabled:opacity-30"
        >
          <span className="flex h-6 w-6 items-center justify-center text-xl leading-6">&minus;</span>
        </button>
        <span className="h-6 w-[17px] text-center font-nunito text-[14px] font-normal leading-6 text-[#0B3857]">
          {value}
        </span>
        <button
          type="button"
          onClick={onInc}
          className="flex h-8 w-8 items-center justify-center rounded-full border border-[#027EAC] bg-white px-2 py-1 font-nunito text-[#0B3857]"
        >
          <span className="flex h-6 w-6 items-center justify-center text-xl leading-6">+</span>
        </button>
      </div>
    </div>
  </div>
);

const TouristSelector = ({ value, onChange }) => {
  const [open, setOpen] = useState(false);
  const total = value.adults + value.children;
  const summary = `${value.adults} adult${value.adults !== 1 ? 's' : ''}${
    value.children ? `, ${value.children} child${value.children !== 1 ? 'ren' : ''}` : ''
  }`;

  return (
    <div className="relative w-[208px] shrink-0">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className={`flex h-[56px] w-full items-center justify-between gap-3 rounded-lg border bg-white px-3 text-left transition hover:shadow-sm focus:outline-none ${
          open || total > 2 ? 'border-[#027EAC] shadow-sm' : 'border-[#D3E1ED] hover:border-[#027EAC]/50'
        }`}
      >
        <div className="flex min-w-0 flex-1 items-center gap-2">
          <PeopleIcon className="h-6 w-6 shrink-0" />
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
          <div className="absolute left-0 z-30 mt-2 h-[112px] w-full rounded-[6px] border border-[#D3E1ED] bg-white p-4 shadow-[0px_2px_10px_6px_rgba(2,126,172,0.2)]">
            <div className="flex h-20 w-[176px] flex-col gap-3">
              <div className="h-20 w-[176px] rounded-[6px] bg-white">
                <Counter
                  label="Adults"
                  value={value.adults}
                  min={1}
                  onDec={() => onChange({ ...value, adults: Math.max(1, value.adults - 1) })}
                  onInc={() => onChange({ ...value, adults: value.adults + 1 })}
                />
                <Counter
                  label="Children"
                  value={value.children}
                  min={0}
                  onDec={() => onChange({ ...value, children: Math.max(0, value.children - 1) })}
                  onInc={() => onChange({ ...value, children: value.children + 1 })}
                />
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default TouristSelector;
