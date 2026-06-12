import { useState } from 'react';
import { fetchReport, downloadReport } from '../../services/reportService';
import { TOUR_DATA } from '../../services/travelService';

const reportTypes = ['Staff performance', 'Sales'];

// Extract unique countries from tour data and group locations by country
const getLocations = () => {
  const locationsByCountry = {};
  
  TOUR_DATA.forEach(tour => {
    // destination format: "City, Country" or "Region, Country"
    const parts = tour.destination.split(', ');
    const country = parts[parts.length - 1]; // Get the last part as country
    
    if (!locationsByCountry[country]) {
      locationsByCountry[country] = new Set();
    }
    locationsByCountry[country].add(tour.location);
  });
  
  // Return country names sorted, with "All locations" first
  const countries = Object.keys(locationsByCountry).sort();
  return ['All locations', ...countries];
};

const locations = getLocations();
const WEEK_DAYS   = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];

const toApiType = (label) => (label === 'Staff performance' ? 'staff' : 'sales');

const fmtIso = (d) => {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
};

/* ─── icons ─── */
const ChevronDown = ({ color = '#677883' }) => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M6 9l6 6 6-6" stroke={color} strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);
const ChevronLeft = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
    <path d="M10 4L6 8l4 4" stroke="#0B3857" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);
const ChevronRight = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
    <path d="M6 4l4 4-4 4" stroke="#0B3857" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

/* ─── simple select dropdown ─── */
const Dropdown = ({ placeholder, options, value, onChange }) => {
  const [open, setOpen] = useState(false);
  return (
    <div className="relative min-w-0 flex-1">
      <button
        type="button"
        onClick={() => setOpen((p) => !p)}
        className={`flex h-14 w-full items-center justify-between gap-1 rounded-lg border bg-white px-3 py-4 transition-colors ${
          open ? 'border-[#027EAC]' : 'border-[#D3E1ED] hover:border-[#027EAC]'
        }`}
      >
        <span className="truncate font-nunito text-[14px] font-normal leading-6 text-[#677883]">
          {value || placeholder}
        </span>
        <span className={`shrink-0 transition-transform duration-150 ${open ? 'rotate-180' : ''}`}>
          <ChevronDown />
        </span>
      </button>
      {open && (
        <>
          <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} />
          <ul className="absolute left-0 top-[calc(100%+4px)] z-20 w-full overflow-hidden rounded-lg border border-[#D3E1ED] bg-white shadow-[0px_2px_10px_6px_rgba(2,126,172,0.12)]">
            {options.map((opt) => (
              <li key={opt}>
                <button
                  type="button"
                  onClick={() => { onChange(opt); setOpen(false); }}
                  className={`w-full px-3 py-3 text-left font-nunito text-[14px] leading-6 hover:bg-[#E7F9FF] ${
                    opt === value ? 'bg-[#F5F8FA] font-bold text-[#0B3857]' : 'font-normal text-[#0B3857]'
                  }`}
                >
                  {opt}
                </button>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
};

/* ─── date-range calendar picker ─── */
const DateRangePicker = ({ value, onChange }) => {
  const [open, setOpen]           = useState(false);
  const [hoverDate, setHoverDate] = useState(null);
  const now = new Date();
  const [viewYear, setViewYear]   = useState(now.getFullYear());
  const [viewMonth, setViewMonth] = useState(now.getMonth());

  const fmt = (d) => d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  const display = value.start && value.end
    ? `${fmt(value.start)} - ${fmt(value.end)}`
    : value.start ? fmt(value.start) : '';

  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
  const rawFirst    = new Date(viewYear, viewMonth, 1).getDay();
  const firstDay    = rawFirst === 0 ? 6 : rawFirst - 1;
  const monthLabel  = new Date(viewYear, viewMonth).toLocaleDateString('en-US', { month: 'long', year: 'numeric' });

  const prevMonth = () => viewMonth === 0 ? (setViewYear((y) => y - 1), setViewMonth(11)) : setViewMonth((m) => m - 1);
  const nextMonth = () => viewMonth === 11 ? (setViewYear((y) => y + 1), setViewMonth(0))  : setViewMonth((m) => m + 1);

  const handleDay = (day) => {
    const date = new Date(viewYear, viewMonth, day);
    if (!value.start || (value.start && value.end)) {
      onChange({ start: date, end: null });
    } else if (date.toDateString() === value.start.toDateString()) {
      onChange({ start: null, end: null });
    } else if (date < value.start) {
      onChange({ start: date, end: value.start }); setOpen(false);
    } else {
      onChange({ start: value.start, end: date }); setOpen(false);
    }
  };

  const dayState = (day) => {
    const date = new Date(viewYear, viewMonth, day);
    const isStart = value.start && date.toDateString() === value.start.toDateString();
    const isEnd   = value.end   && date.toDateString() === value.end.toDateString();
    const eff     = !value.end && hoverDate ? hoverDate : value.end;
    let inRange   = false;
    if (value.start && eff) {
      const lo = value.start < eff ? value.start : eff;
      const hi = value.start < eff ? eff : value.start;
      inRange = date > lo && date < hi;
    }
    return { isStart, isEnd, inRange };
  };

  return (
    <div className="relative min-w-0 flex-1">
      <button
        type="button"
        onClick={() => setOpen((p) => !p)}
        className={`flex h-14 w-full items-center justify-between gap-1 rounded-lg border bg-white px-3 py-4 transition-colors ${
          open ? 'border-[#027EAC]' : 'border-[#D3E1ED] hover:border-[#027EAC]'
        }`}
      >
        <span className="truncate font-nunito text-[14px] font-normal leading-6 text-[#677883]">
          {display || 'Select period'}
        </span>
        <span className={`shrink-0 transition-transform duration-150 ${open ? 'rotate-180' : ''}`}>
          <ChevronDown />
        </span>
      </button>
      {open && (
        <>
          <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} />
          <div className="absolute left-0 top-[calc(100%+4px)] z-20 w-[280px] select-none rounded-lg border border-[#D3E1ED] bg-white p-4 shadow-[0px_2px_10px_6px_rgba(2,126,172,0.12)]">
            <div className="mb-3 flex items-center justify-between">
              <button type="button" onClick={prevMonth} className="flex h-6 w-6 items-center justify-center rounded hover:bg-[#E7F9FF]"><ChevronLeft /></button>
              <span className="font-nunito text-sm font-bold text-[#0B3857]">{monthLabel}</span>
              <button type="button" onClick={nextMonth} className="flex h-6 w-6 items-center justify-center rounded hover:bg-[#E7F9FF]"><ChevronRight /></button>
            </div>
            <div className="mb-1 grid grid-cols-7">
              {WEEK_DAYS.map((d, i) => (
                <div key={i} className="flex h-8 items-center justify-center font-nunito text-xs font-bold text-[#677883]">{d}</div>
              ))}
            </div>
            <div className="grid grid-cols-7">
              {Array.from({ length: firstDay }).map((_, i) => <div key={`pad-${i}`} className="h-8" />)}
              {Array.from({ length: daysInMonth }).map((_, i) => {
                const day = i + 1;
                const { isStart, isEnd, inRange } = dayState(day);
                const isEndpoint = isStart || isEnd;
                return (
                  <div key={day} className={`flex h-8 items-center justify-center ${inRange ? 'bg-[#E7F9FF]' : ''}`}>
                    <button
                      type="button"
                      onClick={() => handleDay(day)}
                      onMouseEnter={() => value.start && !value.end && setHoverDate(new Date(viewYear, viewMonth, day))}
                      onMouseLeave={() => setHoverDate(null)}
                      className={`flex h-8 w-8 items-center justify-center rounded-full font-nunito text-sm transition-colors ${
                        isEndpoint ? 'bg-[#027EAC] font-bold text-white' : 'text-[#0B3857] hover:bg-[#027EAC] hover:text-white'
                      }`}
                    >
                      {day}
                    </button>
                  </div>
                );
              })}
            </div>
          </div>
        </>
      )}
    </div>
  );
};

/* ─── column header with tooltip on hover ─── */
const ColHeader = ({ label, tooltip, className = '' }) => {
  const [show, setShow] = useState(false);
  return (
    <th
      className={`relative border border-[#D3E1ED] px-3 py-3 text-center font-nunito text-[13px] font-bold leading-5 text-[#0B3857] ${className}`}
      onMouseEnter={() => tooltip && setShow(true)}
      onMouseLeave={() => setShow(false)}
    >
      <span className="block truncate">{label}</span>
      {show && tooltip && (
        <div className="absolute left-1/2 top-full z-30 mt-1 -translate-x-1/2 whitespace-nowrap rounded-md border border-[#D3E1ED] bg-white px-3 py-2 text-left font-nunito text-[12px] font-normal leading-5 text-[#0B3857] shadow-md">
          {tooltip}
        </div>
      )}
    </th>
  );
};

/* ─── delta cell ─── */
const Delta = ({ val }) => {
  if (!val) return <span className="text-[#677883]">—</span>;
  return (
    <span className={val.startsWith('+') ? 'text-[#118819]' : 'text-[#B70B0B]'}>{val}</span>
  );
};

/* ─── download button ─── */
const DownloadButton = ({ reportType, dateRange, location }) => {
  const [open, setOpen] = useState(false);
  const [downloading, setDownloading] = useState(false);

  const handleDownload = async (format) => {
    setOpen(false);
    setDownloading(true);
    try {
      const apiType  = toApiType(reportType);
      const fromDate = fmtIso(dateRange.start);
      const toDate   = fmtIso(dateRange.end);
      const loc      = location && location !== 'All locations' ? location : undefined;

      const blob = await downloadReport({ type: apiType, fromDate, toDate, format, location: loc });
      const ext  = format === 'excel' ? 'xlsx' : format === 'pdf' ? 'pdf' : 'csv';
      const name = `${apiType}_report_${fromDate}_${toDate}.${ext}`;

      const url = URL.createObjectURL(blob);
      const a   = document.createElement('a');
      a.href     = url;
      a.download = name;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      alert('Download failed. Please try again.');
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="relative inline-block">
      <button
        type="button"
        disabled={downloading}
        onClick={() => setOpen((p) => !p)}
        className={`flex h-10 items-center gap-2 rounded-lg border-2 px-4 font-nunito text-[14px] font-bold leading-6 transition-colors ${
          open ? 'border-[#027EAC] bg-white text-[#027EAC]' : 'border-[#027EAC] bg-white text-[#027EAC] hover:bg-[#E7F9FF]'
        } disabled:opacity-50`}
      >
        {downloading ? 'Downloading…' : 'Download'}
        {!downloading && (
          <span className={`transition-transform duration-150 ${open ? 'rotate-180' : ''}`}>
            <ChevronDown color="#027EAC" />
          </span>
        )}
      </button>
      {open && (
        <>
          <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} />
          <ul className="absolute right-0 top-[calc(100%+4px)] z-20 w-[168px] overflow-hidden rounded-lg border border-[#D3E1ED] bg-white shadow-[0px_2px_10px_6px_rgba(2,126,172,0.12)]">
            <li>
              <button type="button" onClick={() => handleDownload('excel')}
                className="w-full px-4 py-3 text-left font-nunito text-[14px] font-normal leading-6 text-[#0B3857] hover:bg-[#E7F9FF]">
                Download Excel
              </button>
            </li>
            <li>
              <button type="button" onClick={() => handleDownload('csv')}
                className="w-full px-4 py-3 text-left font-nunito text-[14px] font-normal leading-6 text-[#0B3857] hover:bg-[#E7F9FF]">
                Download CSV
              </button>
            </li>
            <li>
              <button type="button" onClick={() => handleDownload('pdf')}
                className="w-full px-4 py-3 text-left font-nunito text-[14px] font-normal leading-6 text-[#0B3857] hover:bg-[#E7F9FF]">
                Download PDF
              </button>
            </li>
          </ul>
        </>
      )}
    </div>
  );
};

/* ─── staff performance table ─── */
const StaffTable = ({ rows, dateRange }) => (
  <div className="w-full overflow-x-auto rounded-xl border border-[#D3E1ED] bg-white">
    <table className="min-w-[1100px] w-full border-collapse">
      <thead>
        <tr className="bg-white">
          <ColHeader label="Travel Agent"             className="min-w-[130px]" />
          <ColHeader label="Travel Agent e-mail"      className="min-w-[180px]" />
          <ColHeader label="Reporting period (from)"  tooltip="Reporting period start date"                     className="min-w-[130px]" />
          <ColHeader label="Reporting period (to)"    tooltip="Reporting period end date"                       className="min-w-[130px]" />
          <ColHeader label="Tours sold"               className="min-w-[100px]" />
          <ColHeader label="Delta of tours sold"      tooltip="Change in tours sold vs previous period"         className="min-w-[130px]" />
          <ColHeader label="Avg. Feedback (1–5)"      tooltip="Average Feedback for Travel experience (1 to 5)" className="min-w-[140px]" />
          <ColHeader label="Min. Feedback (1–5)"      tooltip="Minimum Feedback for Travel experience (1 to 5)" className="min-w-[140px]" />
          <ColHeader label="Delta of Avg. Feedback"   tooltip="Change in average feedback vs previous period"   className="min-w-[150px]" />
          <ColHeader label="Revenue (USD)"            className="min-w-[120px]" />
          <ColHeader label="Delta Revenue"            tooltip="Change in revenue vs previous period"            className="min-w-[110px]" />
        </tr>
      </thead>
      <tbody>
        {rows.map((row, idx) => (
          <tr key={idx} className="border-t border-[#D3E1ED] hover:bg-[#F5F8FA]">
            <td className="border border-[#D3E1ED] px-3 py-3 font-nunito text-[13px] text-[#0B3857]">{row.agentName}</td>
            <td className="border border-[#D3E1ED] px-3 py-3 font-nunito text-[13px] text-[#027EAC] underline">{row.agentEmail}</td>
            <td className="border border-[#D3E1ED] px-3 py-3 text-center font-nunito text-[13px] text-[#0B3857]">{row.reportPeriodStart}</td>
            <td className="border border-[#D3E1ED] px-3 py-3 text-center font-nunito text-[13px] text-[#0B3857]">{row.reportPeriodEnd}</td>
            <td className="border border-[#D3E1ED] px-3 py-3 text-center font-nunito text-[13px] text-[#0B3857]">{row.toursSold}</td>
            <td className="border border-[#D3E1ED] px-3 py-3 text-center font-nunito text-[13px]"><Delta val={row.deltaToursSoldPct} /></td>
            <td className="border border-[#D3E1ED] px-3 py-3 text-center font-nunito text-[13px] text-[#0B3857]">{row.avgFeedbackRate?.toFixed(1) ?? '—'}</td>
            <td className="border border-[#D3E1ED] px-3 py-3 text-center font-nunito text-[13px] text-[#0B3857]">{row.minFeedbackRate?.toFixed(1) ?? '—'}</td>
            <td className="border border-[#D3E1ED] px-3 py-3 text-center font-nunito text-[13px]"><Delta val={row.deltaAvgFeedbackPct} /></td>
            <td className="border border-[#D3E1ED] px-3 py-3 text-center font-nunito text-[13px] text-[#0B3857]">${row.revenueUsd?.toLocaleString() ?? '0'}</td>
            <td className="border border-[#D3E1ED] px-3 py-3 text-center font-nunito text-[13px]"><Delta val={row.deltaRevenuePct} /></td>
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);

/* ─── sales table ─── */
const SalesTable = ({ rows }) => (
  <div className="w-full overflow-x-auto rounded-xl border border-[#D3E1ED] bg-white">
    <table className="min-w-[1100px] w-full border-collapse">
      <thead>
        <tr className="bg-white">
          <ColHeader label="Tour Name"               className="min-w-[150px]" />
          <ColHeader label="Country"                 className="min-w-[100px]" />
          <ColHeader label="City"                    className="min-w-[100px]" />
          <ColHeader label="Reporting period (from)" tooltip="Reporting period start date"                     className="min-w-[130px]" />
          <ColHeader label="Reporting period (to)"   tooltip="Reporting period end date"                       className="min-w-[130px]" />
          <ColHeader label="Tours sold"              className="min-w-[100px]" />
          <ColHeader label="Delta of tours sold"     tooltip="Change in tours sold vs previous period"         className="min-w-[130px]" />
          <ColHeader label="Avg. Feedback (1–5)"     tooltip="Average Feedback for Travel experience (1 to 5)" className="min-w-[140px]" />
          <ColHeader label="Min. Feedback (1–5)"     tooltip="Minimum Feedback for Travel experience (1 to 5)" className="min-w-[140px]" />
          <ColHeader label="Delta of Avg. Feedback"  tooltip="Change in average feedback vs previous period"   className="min-w-[150px]" />
          <ColHeader label="Revenue (USD)"           className="min-w-[120px]" />
          <ColHeader label="Delta Revenue"           tooltip="Change in revenue vs previous period"            className="min-w-[110px]" />
        </tr>
      </thead>
      <tbody>
        {rows.map((row, idx) => (
          <tr key={idx} className="border-t border-[#D3E1ED] hover:bg-[#F5F8FA]">
            <td className="border border-[#D3E1ED] px-3 py-3 font-nunito text-[13px] text-[#0B3857]">{row.tourName}</td>
            <td className="border border-[#D3E1ED] px-3 py-3 font-nunito text-[13px] text-[#0B3857]">{row.country}</td>
            <td className="border border-[#D3E1ED] px-3 py-3 font-nunito text-[13px] text-[#0B3857]">{row.city}</td>
            <td className="border border-[#D3E1ED] px-3 py-3 text-center font-nunito text-[13px] text-[#0B3857]">{row.reportPeriodStart}</td>
            <td className="border border-[#D3E1ED] px-3 py-3 text-center font-nunito text-[13px] text-[#0B3857]">{row.reportPeriodEnd}</td>
            <td className="border border-[#D3E1ED] px-3 py-3 text-center font-nunito text-[13px] text-[#0B3857]">{row.toursSold}</td>
            <td className="border border-[#D3E1ED] px-3 py-3 text-center font-nunito text-[13px]"><Delta val={row.deltaToursSoldPct} /></td>
            <td className="border border-[#D3E1ED] px-3 py-3 text-center font-nunito text-[13px] text-[#0B3857]">{row.avgFeedbackRate?.toFixed(1) ?? '—'}</td>
            <td className="border border-[#D3E1ED] px-3 py-3 text-center font-nunito text-[13px] text-[#0B3857]">{row.minFeedbackRate?.toFixed(1) ?? '—'}</td>
            <td className="border border-[#D3E1ED] px-3 py-3 text-center font-nunito text-[13px]"><Delta val={row.deltaAvgFeedbackPct} /></td>
            <td className="border border-[#D3E1ED] px-3 py-3 text-center font-nunito text-[13px] text-[#0B3857]">${row.revenueUsd?.toLocaleString() ?? '0'}</td>
            <td className="border border-[#D3E1ED] px-3 py-3 text-center font-nunito text-[13px]"><Delta val={row.deltaRevenuePct} /></td>
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);

/* ─── report section ─── */
const ReportSection = ({ reportType, rows, dateRange, location }) => (
  <div className="mt-8">
    <h2 className="mb-4 font-nunito text-xl font-bold text-[#0B3857]">Report</h2>

    {rows.length === 0 ? (
      <div className="rounded-xl border border-[#D3E1ED] bg-white px-6 py-10 text-center font-nunito text-[14px] text-[#677883]">
        No data found for the selected period.
      </div>
    ) : reportType === 'Staff performance' ? (
      <StaffTable rows={rows} dateRange={dateRange} />
    ) : (
      <SalesTable rows={rows} />
    )}

    <div className="mt-4 flex justify-end">
      <DownloadButton reportType={reportType} dateRange={dateRange} location={location} />
    </div>
  </div>
);

/* ─── page ─── */
const AdminReports = () => {
  const [reportType, setReportType] = useState('');
  const [dateRange, setDateRange]   = useState({ start: null, end: null });
  const [location, setLocation]     = useState('');
  const [reportData, setReportData] = useState(null);
  const [loading, setLoading]       = useState(false);
  const [error, setError]           = useState(null);

  const canGenerate = reportType && dateRange.start && dateRange.end;

  const handleGenerate = async () => {
    if (!canGenerate) return;
    setLoading(true);
    setError(null);
    setReportData(null);
    try {
      const apiType  = toApiType(reportType);
      const fromDate = fmtIso(dateRange.start);
      const toDate   = fmtIso(dateRange.end);
      const loc      = location && location !== 'All locations' ? location : undefined;

      const rows = await fetchReport({ type: apiType, fromDate, toDate, location: loc });
      setReportData({ rows, dateRange });
    } catch (err) {
      setError('Failed to generate report. Please check your connection and try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full min-h-[calc(100vh-72px)] px-4 pt-10 pb-16 sm:px-6 lg:px-10">
      <div className="mx-auto flex w-full max-w-[1360px] flex-col gap-6">

        <h1 className="self-center text-center font-nunito text-2xl font-bold leading-10 text-[#0B3857]">
          Generate a report
        </h1>

        {/* Filter bar */}
        <div className="flex w-full items-center gap-4 rounded-xl bg-white px-4 py-4 sm:px-6">
          <Dropdown
            placeholder="Select report type"
            options={reportTypes}
            value={reportType}
            onChange={(v) => { setReportType(v); setReportData(null); setError(null); }}
          />
          <DateRangePicker
            value={dateRange}
            onChange={(v) => { setDateRange(v); setReportData(null); setError(null); }}
          />
          <Dropdown
            placeholder="Select location (optional)"
            options={locations}
            value={location}
            onChange={(v) => { setLocation(v); setReportData(null); setError(null); }}
          />
          <button
            type="button"
            disabled={!canGenerate || loading}
            onClick={handleGenerate}
            className="h-14 min-w-0 flex-1 rounded-lg bg-[#027EAC] px-3 font-nunito text-sm font-bold leading-6 text-white transition-colors hover:bg-[#025f84] disabled:cursor-not-allowed disabled:opacity-50 sm:text-base"
          >
            {loading ? 'Generating…' : 'Generate report'}
          </button>
        </div>

        {/* Error */}
        {error && (
          <div className="rounded-xl border border-red-200 bg-red-50 px-6 py-4 font-nunito text-[14px] text-red-700">
            {error}
          </div>
        )}

        {/* Report section */}
        {reportData && (
          <ReportSection
            reportType={reportType}
            rows={reportData.rows}
            dateRange={reportData.dateRange}
            location={location}
          />
        )}

      </div>
    </div>
  );
};

export default AdminReports;
