import { ReactComponent as DownwardIcon } from '../../assets/icons/downward.svg';

const MultiSelectShell = ({
  icon,
  label,
  summary,
  open,
  setOpen,
  children,
  wrapperClass = '',
  dropdownClass = 'min-w-[200px] rounded-xl border border-[#D3E1ED] bg-white p-2 shadow-dropdown',
}) => (
  <div className={`relative min-w-[140px] ${wrapperClass}`}>
    <button
      type="button"
      onClick={() => setOpen((v) => !v)}
      className={`flex h-[56px] w-full items-center justify-between gap-3 rounded-lg border bg-white px-3 text-left transition hover:shadow-sm focus:outline-none ${
        open ? 'border-[#027EAC] shadow-sm' : 'border-[#D3E1ED] hover:border-[#027EAC]/50'
      }`}
    >
      <div className="flex min-w-0 flex-1 items-center gap-2">
        {icon && <span className="h-6 w-6 shrink-0 text-[#0B3857]">{icon}</span>}
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
        <div className={`absolute left-0 z-30 mt-2 ${dropdownClass}`}>
          {children}
        </div>
      </>
    )}
  </div>
);

export default MultiSelectShell;
