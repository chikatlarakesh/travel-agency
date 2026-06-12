import { useEffect, useState, useCallback } from 'react';
import { fetchAdminReviews, updateReviewVisibility } from '../../services/feedbackModerationService';

/* ─── icons ─── */
const ChevronDown = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
    <path d="M4 6l4 4 4-4" stroke="#677883" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);
const FlagIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z" stroke="#B70B0B" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    <line x1="4" y1="22" x2="4" y2="15" stroke="#B70B0B" strokeWidth="2" strokeLinecap="round" />
  </svg>
);

/* ─── star rating display ─── */
const Stars = ({ rate }) => (
  <div className="flex gap-0.5">
    {[1, 2, 3, 4, 5].map((s) => (
      <svg key={s} width="14" height="14" viewBox="0 0 20 20" fill={s <= rate ? '#F59E0B' : '#D1D5DB'}>
        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
      </svg>
    ))}
  </div>
);

/* ─── simple select ─── */
const Select = ({ value, onChange, options, placeholder }) => {
  const [open, setOpen] = useState(false);
  const label = options.find((o) => o.value === value)?.label || placeholder;
  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((p) => !p)}
        className="flex h-10 items-center gap-2 rounded-lg border border-[#D3E1ED] bg-white px-3 font-nunito text-[13px] text-[#677883] hover:border-[#027EAC]"
      >
        <span>{label}</span>
        <ChevronDown />
      </button>
      {open && (
        <>
          <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} />
          <ul className="absolute left-0 top-[calc(100%+4px)] z-20 min-w-[140px] overflow-hidden rounded-lg border border-[#D3E1ED] bg-white shadow-md">
            {options.map((opt) => (
              <li key={opt.value}>
                <button
                  type="button"
                  onClick={() => { onChange(opt.value); setOpen(false); }}
                  className={`w-full px-3 py-2.5 text-left font-nunito text-[13px] hover:bg-[#E7F9FF] ${opt.value === value ? 'font-bold text-[#0B3857]' : 'text-[#0B3857]'}`}
                >
                  {opt.label}
                </button>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
};

/* ─── visibility badge ─── */
const VisibilityBadge = ({ status }) => (
  <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 font-nunito text-[12px] font-medium ${
    status === 'PUBLISHED'
      ? 'bg-[#DCFCE7] text-[#118819]'
      : 'bg-[#FEE2E2] text-[#B70B0B]'
  }`}>
    {status === 'PUBLISHED' ? 'Published' : 'Hidden'}
  </span>
);

/* ─── tabs ─── */
const TABS = [
  { key: 'all',     label: 'All reviews' },
];

const RATING_OPTIONS = [
  { value: '', label: 'All ratings' },
  { value: '1', label: '1 star' },
  { value: '2', label: '2 stars' },
  { value: '3', label: '3 stars' },
  { value: '4', label: '4 stars' },
  { value: '5', label: '5 stars' },
];

const CATEGORY_OPTIONS = [
  { value: '', label: 'All categories' },
  { value: 'RESORT', label: 'Resort' },
  { value: 'CRUISE', label: 'Cruise' },
  { value: 'HIKE',   label: 'Hike' },
];

const VISIBILITY_OPTIONS = [
  { value: '',          label: 'All statuses' },
  { value: 'PUBLISHED', label: 'Published' },
  { value: 'HIDDEN',    label: 'Hidden' },
];

const PAGE_SIZE = 20;

const AdminFeedbackModeration = () => {
  const [activeTab, setActiveTab]     = useState('all');
  const [reviews, setReviews]         = useState([]);
  const [totalItems, setTotalItems]   = useState(0);
  const [totalPages, setTotalPages]   = useState(1);
  const [page, setPage]               = useState(1);
  const [loading, setLoading]         = useState(false);
  const [error, setError]             = useState(null);
  const [updatingId, setUpdatingId]   = useState(null);

  // Filters
  const [rating, setRating]           = useState('');
  const [tourType, setTourType]       = useState('');
  const [visibility, setVisibility]   = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = {
        page,
        pageSize: PAGE_SIZE,
        rating:     rating   || undefined,
        tourType:   tourType || undefined,
        visibility: visibility || undefined,
      };
      const data = await fetchAdminReviews(params);
      setReviews(data.reviews);
      setTotalItems(data.totalItems);
      setTotalPages(data.totalPages);
    } catch {
      setError('Failed to load reviews. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [page, rating, tourType, visibility]);

  useEffect(() => { load(); }, [load]);

  // Reset page when filters/tab change
  useEffect(() => { setPage(1); }, [rating, tourType, visibility, activeTab]);

  const handleToggleVisibility = async (review) => {
    const next = review.visibility === 'PUBLISHED' ? 'HIDDEN' : 'PUBLISHED';
    setUpdatingId(review.id);
    try {
      const updated = await updateReviewVisibility(review.id, next);
      setReviews((prev) => prev.map((r) => r.id === review.id ? updated : r));
    } catch {
      alert('Failed to update visibility. Please try again.');
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <div className="w-full min-h-[calc(100vh-72px)] px-4 pt-10 pb-16 sm:px-6 lg:px-10">
      <div className="mx-auto flex w-full max-w-[1360px] flex-col gap-6">

        <h1 className="self-center text-center font-nunito text-2xl font-bold leading-10 text-[#0B3857]">
          Feedback Moderation
        </h1>

        {/* Tabs */}
        <div className="flex gap-0 border-b border-[#D3E1ED]">
          {TABS.map((tab) => (
            <button
              key={tab.key}
              type="button"
              onClick={() => setActiveTab(tab.key)}
              className={`relative h-11 px-6 font-nunito text-[14px] font-medium transition-colors ${
                activeTab === tab.key
                  ? 'text-[#0B3857]'
                  : 'text-[#677883] hover:text-[#0B3857]'
              }`}
            >
              {tab.label}
              {activeTab === tab.key && (
                <span className="absolute bottom-0 left-0 h-[3px] w-full rounded-t-full bg-[#027EAC]" />
              )}
            </button>
          ))}
        </div>

        {/* Filter bar */}
        <div className="flex flex-wrap items-center gap-3 rounded-xl bg-white px-4 py-4">
          <Select value={rating}     onChange={(v) => setRating(v)}     options={RATING_OPTIONS}     placeholder="All ratings" />
          <Select value={tourType}   onChange={(v) => setTourType(v)}   options={CATEGORY_OPTIONS}   placeholder="All categories" />
          <Select value={visibility} onChange={(v) => setVisibility(v)} options={VISIBILITY_OPTIONS} placeholder="All statuses" />
          <span className="ml-auto font-nunito text-[13px] text-[#677883]">
            {loading ? 'Loading…' : `${totalItems} review${totalItems !== 1 ? 's' : ''}`}
          </span>
        </div>

        {/* Error */}
        {error && (
          <div className="rounded-xl border border-red-200 bg-red-50 px-6 py-4 font-nunito text-[14px] text-red-700">
            {error}
          </div>
        )}

        {/* Table */}
        {!loading && !error && (
          reviews.length === 0 ? (
            <div className="rounded-xl border border-[#D3E1ED] bg-white px-6 py-12 text-center font-nunito text-[14px] text-[#677883]">
              No reviews found for the selected filters.
            </div>
          ) : (
            <div className="overflow-x-auto rounded-xl border border-[#D3E1ED] bg-white">
              <table className="min-w-[1100px] w-full border-collapse">
                <thead>
                  <tr className="border-b border-[#D3E1ED]">
                    {['Customer', 'Tour', 'Category', 'Rating', 'Comment', 'Status', 'Action'].map((h) => (
                      <th key={h} className="px-4 py-3 text-left font-nunito text-[13px] font-bold text-[#0B3857]">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {reviews.map((review, idx) => (
                    <tr
                      key={review.id}
                      className={`border-b border-[#D3E1ED] transition-colors hover:bg-[#F5F8FA] ${review.flagged ? 'bg-[#FFF7F7]' : idx % 2 === 1 ? 'bg-[#FAFCFE]' : ''}`}
                    >
                      {/* Customer */}
                      <td className="px-4 py-3 font-nunito text-[13px] text-[#0B3857]">
                        <div className="flex items-center gap-2">
                          {review.flagged && (
                            <span title={review.flagReason || 'Flagged for review'}>
                              <FlagIcon />
                            </span>
                          )}
                          <span>{review.authorName || 'Traveler'}</span>
                        </div>
                      </td>
                      {/* Tour */}
                      <td className="px-4 py-3 font-nunito text-[13px] text-[#0B3857]">
                        <div>{review.tourName || '—'}</div>
                      </td>
                      {/* Category */}
                      <td className="px-4 py-3 font-nunito text-[13px] text-[#677883]">
                        {review.tourType ? review.tourType.charAt(0) + review.tourType.slice(1).toLowerCase() : '—'}
                      </td>
                      {/* Rating */}
                      <td className="px-4 py-3">
                        <Stars rate={review.rate} />
                      </td>
                      {/* Comment */}
                      <td className="max-w-[320px] px-4 py-3 font-nunito text-[13px] text-[#677883]">
                        <p className="line-clamp-2">{review.reviewContent || '—'}</p>
                        {review.flagReason && (
                          <p className="mt-1 text-[11px] text-[#B70B0B]">{review.flagReason}</p>
                        )}
                      </td>
                      {/* Visibility badge */}
                      <td className="px-4 py-3">
                        <VisibilityBadge status={review.visibility} />
                      </td>
                      {/* Action */}
                      <td className="px-4 py-3">
                        <button
                          type="button"
                          disabled={updatingId === review.id}
                          onClick={() => handleToggleVisibility(review)}
                          className={`rounded-lg border-2 px-3 py-1.5 font-nunito text-[12px] font-bold transition-colors disabled:opacity-50 ${
                            review.visibility === 'PUBLISHED'
                              ? 'border-[#B70B0B] text-[#B70B0B] hover:bg-[#FEE2E2]'
                              : 'border-[#118819] text-[#118819] hover:bg-[#DCFCE7]'
                          }`}
                        >
                          {updatingId === review.id
                            ? '…'
                            : review.visibility === 'PUBLISHED' ? 'Hide' : 'Publish'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-2">
            <button
              type="button"
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              disabled={page === 1}
              className="rounded-lg border border-[#D3E1ED] bg-white px-3 py-1.5 font-nunito text-[13px] text-[#0B3857] hover:bg-[#E7F9FF] disabled:opacity-40"
            >
              Previous
            </button>
            <span className="font-nunito text-[13px] text-[#677883]">
              Page {page} of {totalPages}
            </span>
            <button
              type="button"
              onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              disabled={page === totalPages}
              className="rounded-lg border border-[#D3E1ED] bg-white px-3 py-1.5 font-nunito text-[13px] text-[#0B3857] hover:bg-[#E7F9FF] disabled:opacity-40"
            >
              Next
            </button>
          </div>
        )}

      </div>
    </div>
  );
};

export default AdminFeedbackModeration;
