import { useState } from 'react';
import { ReactComponent as CheckboxIcon } from '../../assets/icons/checkbox.svg';
import { ReactComponent as TourIcon } from '../../assets/icons/tour.svg';
import MultiSelectShell from './MultiSelectShell';

const TOUR_TYPE_OPTIONS = ['Resorts', 'Cruises', 'Hikes'];

const TourTypeFilter = ({ selected, onChange }) => {
  const [open, setOpen] = useState(false);

  const toggle = (opt) =>
    onChange(selected.includes(opt) ? selected.filter((v) => v !== opt) : [...selected, opt]);

  const summary = selected.length ? selected.join(', ') : 'Tour type';

  return (
    <MultiSelectShell
      icon={<TourIcon className="h-6 w-6" />}
      label="Tour type"
      summary={summary}
      open={open}
      setOpen={setOpen}
      wrapperClass="w-[158px] shrink-0"
      dropdownClass="w-[158px] h-[120px] overflow-hidden rounded-[6px] border border-[#D3E1ED] bg-white p-0 shadow-[0px_2px_10px_6px_rgba(2,126,172,0.2)]"
    >
      {TOUR_TYPE_OPTIONS.map((opt) => (
        <label
          key={opt}
          className={`flex h-10 w-full cursor-pointer items-center gap-2 rounded-[6px] px-2 transition ${
            selected.includes(opt) ? 'bg-[#EDF4FA]' : 'bg-white'
          }`}
        >
          <input
            type="checkbox"
            checked={selected.includes(opt)}
            onChange={() => toggle(opt)}
            className="sr-only"
          />
          <span
            className={`flex h-6 w-6 items-center justify-center rounded-[4px] border border-[#027EAC] ${
              selected.includes(opt) ? 'bg-[#027EAC]' : 'bg-white'
            }`}
          >
            {selected.includes(opt) && <CheckboxIcon className="h-6 w-6" />}
          </span>
          <span className="h-6 w-[102px] align-middle font-nunito text-[14px] font-normal leading-6 text-[#0B3857]">
            {opt}
          </span>
        </label>
      ))}
    </MultiSelectShell>
  );
};

export default TourTypeFilter;
