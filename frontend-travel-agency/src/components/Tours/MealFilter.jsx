import { useState } from 'react';
import { ReactComponent as CheckboxIcon } from '../../assets/icons/checkbox.svg';
import { ReactComponent as MealIcon } from '../../assets/icons/Meal.svg';
import MultiSelectShell from './MultiSelectShell';

const MEAL_OPTIONS = [
  { value: 'Breakfast', label: 'Breakfast (BB)' },
  { value: 'Half Board', label: 'Half-board (HB)' },
  { value: 'Full Board', label: 'Full-board (HB)' },
  { value: 'All Inclusive', label: 'All inclusive (AI)' },
];

const MealFilter = ({ selected, onChange }) => {
  const [open, setOpen] = useState(false);

  const toggle = (optValue) => {
    onChange(
      selected.includes(optValue)
        ? selected.filter((v) => v !== optValue)
        : [...selected, optValue]
    );
  };

  const summary = selected.length
    ? MEAL_OPTIONS.filter((opt) => selected.includes(opt.value)).map((opt) => opt.label).join(', ')
    : 'Any meal plan';

  return (
    <MultiSelectShell
      icon={<MealIcon className="h-6 w-6" />}
      label="Meal plan"
      summary={summary}
      open={open}
      setOpen={setOpen}
      wrapperClass="w-[199px] shrink-0"
      dropdownClass="w-[199px] h-[160px] overflow-hidden rounded-[6px] border border-[#D3E1ED] bg-white p-0 shadow-[0px_2px_10px_6px_rgba(2,126,172,0.2)]"
    >
      {MEAL_OPTIONS.map((opt) => (
        <label
          key={opt.value}
          className={`flex h-10 w-[199px] cursor-pointer items-center gap-2 px-2 transition ${
            selected.includes(opt.value) ? 'bg-[#EDF4FA]' : 'bg-white'
          }`}
        >
          <input
            type="checkbox"
            checked={selected.includes(opt.value)}
            onChange={() => toggle(opt.value)}
            className="sr-only"
          />
          <span
            className={`flex h-6 w-6 items-center justify-center rounded-[4px] border border-[#027EAC] ${
              selected.includes(opt.value) ? 'bg-[#027EAC]' : 'bg-white'
            }`}
          >
            {selected.includes(opt.value) && <CheckboxIcon className="h-6 w-6" />}
          </span>
          <span className="h-6 min-w-0 flex-1 align-middle font-nunito text-[14px] font-normal leading-6 text-[#0B3857]">
            {opt.label}
          </span>
        </label>
      ))}
    </MultiSelectShell>
  );
};

export default MealFilter;
