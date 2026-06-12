import { ReactComponent as CalendarIcon } from '../../assets/icons/Calendar.svg';

const DatePicker = ({ value, onChange }) => (
  <div className="relative min-w-[220px] flex-1">
    <label
      className={`flex h-14 w-full cursor-pointer items-center gap-2 rounded-lg border bg-white px-3 transition hover:shadow-sm ${
        value ? 'border-[#027EAC] shadow-sm' : 'border-[#D3E1ED] hover:border-[#027EAC]/50'
      }`}
    >
      <CalendarIcon className="h-4 w-4 shrink-0 text-primary" />
      <span className="flex-1 overflow-hidden">
        <span className="block font-nunito text-[10px] font-extrabold uppercase tracking-wider text-[#677883]">
          Start date
        </span>
        <input
          type="date"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="w-full bg-transparent font-nunito text-sm font-normal text-[#0B3857] focus:outline-none"
        />
      </span>
    </label>
  </div>
);

export default DatePicker;
