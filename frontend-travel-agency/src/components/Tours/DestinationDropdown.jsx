import { useMemo, useRef, useState } from 'react';
import { ReactComponent as CloseIcon } from '../../assets/icons/Close.svg';
import { ReactComponent as DownwardIcon } from '../../assets/icons/downward.svg';
import { ReactComponent as LocationIcon } from '../../assets/icons/Location.svg';

const DestinationDropdown = ({ options, value, query, loading, onSelect, onQueryChange }) => {
  const [open, setOpen] = useState(false);
  const inputRef = useRef(null);

  const filtered = useMemo(() => {
    if (!query) return options;
    const q = query.toLowerCase();
    return options.filter((o) => o.label.toLowerCase().includes(q));
  }, [options, query]);

  return (
    <div className="relative flex-1 min-w-[200px] xl:w-[312px]">
      <div
        role="button"
        tabIndex={0}
        onClick={() => { setOpen(true); inputRef.current?.focus(); }}
        onKeyDown={(e) => e.key === 'Enter' && setOpen(true)}
        className={`relative z-30 flex h-[56px] w-full cursor-text items-center justify-between gap-3 rounded-lg border bg-white px-3 transition hover:shadow-sm ${
          open || value ? 'border-[#027EAC] shadow-sm' : 'border-[#D3E1ED] hover:border-[#027EAC]/50'
        }`}
      >
        <div className="flex min-w-0 flex-1 items-center gap-2">
          <LocationIcon className="h-6 w-6 shrink-0" />
          {open ? (
            <input
              ref={inputRef}
              type="text"
              value={query}
              onFocus={() => setOpen(true)}
              onChange={(e) => { onQueryChange(e.target.value); setOpen(true); }}
              placeholder="Where to?"
              className="h-6 w-full bg-transparent font-nunito text-sm font-normal leading-6 text-[#0B3857] placeholder:text-slate-400 focus:outline-none"
            />
          ) : (
            <span className="block h-6 truncate font-nunito text-sm font-normal leading-6 text-[#0B3857]">
              {query || (value ? value : 'Any destination')}
            </span>
          )}
        </div>
        {open && query ? (
          <button
            type="button"
            onMouseDown={(e) => e.preventDefault()}
            onClick={(e) => { e.stopPropagation(); onQueryChange(''); onSelect(''); }}
            className="h-6 w-6 shrink-0 text-lg leading-none text-slate-400 hover:text-slate-600"
          >
          <CloseIcon className="h-6 w-6 shrink-0" />
          </button>
        ) : (
          <DownwardIcon
            className={`shrink-0 transition-transform duration-200 ${open ? 'rotate-180' : ''}`}
          />
        )}
      </div>

      {open && (
        <>
          <div className="fixed inset-0 z-20" onClick={() => setOpen(false)} />
          <div className="absolute left-0 z-30 mt-2 w-full overflow-hidden rounded-lg border border-[#D3E1ED] bg-white shadow-[0px_2px_10px_6px_rgba(2,126,172,0.2)]">
            <div className="max-h-40 overflow-y-auto">
              {loading && (
                <p className="px-3 py-2.5 font-nunito text-sm text-[#677883]">Searching&hellip;</p>
              )}
              {!loading && !filtered.length && (
                <p className="px-3 py-2.5 font-nunito text-sm text-[#677883]">No destinations found</p>
              )}
              {!loading && filtered.map((opt, index) => (
                <button
                  key={opt.value}
                  type="button"
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => { onSelect(opt.value); onQueryChange(opt.label); setOpen(false); }}
                  className={`flex h-10 w-full items-center gap-3 px-2 text-left font-nunito text-sm transition ${
                    index === 0 ? 'rounded-t-lg bg-[#EDF4FA]' : 'bg-white'
                  } ${
                    value === opt.value
                      ? 'font-semibold text-primary'
                      : 'font-normal text-[#0B3857]'
                  }`}
                >
                  <LocationIcon className="h-4 w-4 shrink-0" />
                  {opt.label}
                </button>
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default DestinationDropdown;
