import { useState, useEffect } from 'react';

/* ── Inline star – filled (dark) or outlined ───────────────── */
const StarSvg = ({ filled }) => (
  <svg
    width="32"
    height="32"
    viewBox="0 0 24 24"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    <path
      d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"
      fill={filled ? '#0B3857' : 'none'}
      stroke="#0B3857"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);

const MAX_COMMENT = 500;

/**
 * FeedbackModal
 *
 * Props:
 *   onClose        – closes the modal
 *   onSubmit       – async fn called with { rating, comment }
 *   isLoading      – shows spinner on Submit button
 *   initialRating  – pre-fill for "Update feedback"
 *   initialComment – pre-fill for "Update feedback"
 */
const FeedbackModal = ({
  onClose,
  onSubmit,
  isLoading = false,
  initialRating = 0,
  initialComment = '',
}) => {
  const [rating, setRating]             = useState(initialRating);
  const [hoveredRating, setHoveredRating] = useState(0);
  const [comment, setComment]           = useState(initialComment);
  const [ratingError, setRatingError]   = useState('');
  const [commentError, setCommentError] = useState('');

  /* ── ESC closes modal ──────────────────────────────────────── */
  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [onClose]);

  /* ── Validation ────────────────────────────────────────────── */
  const validate = () => {
    let valid = true;

    if (!rating) {
      setRatingError('Please select a rating.');
      valid = false;
    } else {
      setRatingError('');
    }

    const trimmed = comment.trim();
    if (rating >= 1 && rating <= 3 && !trimmed) {
      setCommentError('Comment is required for ratings of 1–3 stars.');
      valid = false;
    } else if (trimmed.length > MAX_COMMENT) {
      setCommentError(`Comment must not exceed ${MAX_COMMENT} characters.`);
      valid = false;
    } else {
      setCommentError('');
    }

    return valid;
  };

  const handleSubmit = () => {
    if (!validate()) return;
    onSubmit({ rating, comment: comment.trim() });
  };

  const handleStarClick = (star) => {
    setRating(star);
    setRatingError('');
    if (star >= 4) setCommentError('');
  };

  const displayRating = hoveredRating || rating;

  return (
    /* Overlay – click outside closes */
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      style={{ background: '#42424280' }}
      onClick={onClose}
      role="presentation"
    >
      {/* Modal box */}
      <div
        className="relative flex w-full flex-col bg-white"
        style={{
          maxWidth: '600px',
          width: 'calc(100vw - 24px)',
          borderRadius: '12px',
          padding: '24px',
          gap: '24px',
          maxHeight: 'calc(100vh - 32px)',
          overflowY: 'auto',
        }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="feedback-modal-title"
      >
        {/* ── Header ── */}
        <div className="flex items-center justify-between" style={{ minHeight: '40px' }}>
          <h2
            id="feedback-modal-title"
            className="font-nunito text-[#0B3857]"
            style={{ fontSize: '24px', fontWeight: 700, lineHeight: '40px' }}
          >
            Feedback
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="flex items-center justify-center text-[#677883] transition hover:text-[#0B3857]"
            style={{ width: '24px', height: '24px', flexShrink: 0 }}
            aria-label="Close feedback modal"
          >
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </button>
        </div>

        {/* ── Body ── */}
        <div className="flex flex-col" style={{ gap: '20px' }}>

          {/* ── Star rating section ── */}
          <div className="flex flex-col" style={{ gap: '8px' }}>
            <label
              className="font-nunito"
              style={{ fontSize: '14px', fontWeight: 800, lineHeight: '24px', color: '#0B3857' }}
            >
              Please rate your experience*
            </label>

            {/* Stars row */}
            <div className="flex items-center" style={{ gap: '12px' }}>
              <div
                className="flex items-center"
                style={{ gap: '4px' }}
                role="group"
                aria-label="Star rating"
                onMouseLeave={() => setHoveredRating(0)}
              >
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => handleStarClick(star)}
                    onMouseEnter={() => setHoveredRating(star)}
                    onFocus={() => setHoveredRating(star)}
                    onBlur={() => setHoveredRating(0)}
                    className="rounded focus:outline-none focus-visible:ring-2 focus-visible:ring-[#027EAC] focus-visible:ring-offset-1"
                    style={{
                      transition: 'transform 0.15s ease',
                      transform: hoveredRating >= star || rating >= star ? 'scale(1.1)' : 'scale(1)',
                      cursor: 'pointer',
                      background: 'none',
                      border: 'none',
                      padding: '2px',
                    }}
                    aria-label={`${star} star${star !== 1 ? 's' : ''}`}
                    aria-pressed={rating === star}
                  >
                    <StarSvg filled={star <= displayRating} />
                  </button>
                ))}
              </div>

              {/* "X/5 stars" text */}
              <span
                className="font-nunito"
                style={{ fontSize: '14px', fontWeight: 400, lineHeight: '24px', color: '#677883', minWidth: '56px' }}
              >
                {rating}/5 stars
              </span>
            </div>

            {ratingError && (
              <p className="font-nunito" style={{ fontSize: '12px', color: '#B70B0B', lineHeight: '16px', marginTop: '2px' }}>
                {ratingError}
              </p>
            )}
          </div>

          {/* ── Comment section ── */}
          <div className="flex flex-col" style={{ gap: '8px' }}>
            <label
              className="font-nunito"
              style={{ fontSize: '14px', fontWeight: 700, lineHeight: '24px', color: '#0B3857' }}
              htmlFor="feedback-comment"
            >
              Comment{rating >= 1 && rating <= 3 ? '*' : ''}
            </label>

            <div
              className="flex flex-col"
              style={{
                border: `1px solid ${commentError ? '#B70B0B' : '#D3E1ED'}`,
                borderRadius: '8px',
                padding: '12px',
                transition: 'border-color 0.2s',
              }}
            >
              <textarea
                id="feedback-comment"
                value={comment}
                onChange={(e) => {
                  const val = e.target.value;
                  if (val.length <= MAX_COMMENT) {
                    setComment(val);
                    if (commentError) setCommentError('');
                  }
                }}
                placeholder="Add your comments"
                rows={4}
                className="font-nunito w-full resize-none bg-transparent focus:outline-none"
                style={{
                  fontSize: '14px',
                  lineHeight: '24px',
                  color: '#0B3857',
                  minHeight: '96px',
                  placeholderColor: '#A2AEB9',
                }}
                aria-label="Your comment"
                aria-describedby={commentError ? 'comment-error' : undefined}
                aria-required={rating >= 1 && rating <= 3}
              />
              {/* Character counter – bottom right */}
              <div className="flex justify-end" style={{ marginTop: '4px' }}>
                <span className="font-nunito" style={{ fontSize: '12px', lineHeight: '16px', color: '#677883' }}>
                  {comment.length}/{MAX_COMMENT}
                </span>
              </div>
            </div>

            {commentError && (
              <p
                id="comment-error"
                className="font-nunito"
                style={{ fontSize: '12px', color: '#B70B0B', lineHeight: '16px', marginTop: '2px' }}
              >
                {commentError}
              </p>
            )}
          </div>
        </div>

        {/* ── Footer buttons ── */}
        <div className="flex items-center justify-end" style={{ gap: '8px' }}>
          {/* Cancel */}
          <button
            type="button"
            onClick={onClose}
            disabled={isLoading}
            className="font-nunito transition"
            style={{
              height: '40px',
              padding: '8px 16px',
              borderRadius: '8px',
              border: '2px solid #027EAC',
              background: '#FFFFFF',
              color: '#027EAC',
              fontSize: '14px',
              fontWeight: 700,
              lineHeight: '24px',
              cursor: isLoading ? 'not-allowed' : 'pointer',
              opacity: isLoading ? 0.6 : 1,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              minWidth: '80px',
            }}
          >
            Cancel
          </button>

          {/* Submit */}
          <button
            type="button"
            onClick={handleSubmit}
            disabled={isLoading}
            className="font-nunito transition hover:opacity-90"
            style={{
              height: '40px',
              padding: '8px 24px',
              borderRadius: '8px',
              border: 'none',
              background: '#027EAC',
              color: '#FFFFFF',
              fontSize: '14px',
              fontWeight: 700,
              lineHeight: '24px',
              cursor: isLoading ? 'not-allowed' : 'pointer',
              opacity: isLoading ? 0.75 : 1,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '6px',
              minWidth: '90px',
            }}
          >
            {isLoading ? (
              <>
                <svg
                  width="16" height="16" viewBox="0 0 24 24" fill="none"
                  style={{ animation: 'spin 0.8s linear infinite' }}
                  aria-hidden="true"
                >
                  <circle cx="12" cy="12" r="10" stroke="rgba(255,255,255,0.3)" strokeWidth="3" />
                  <path d="M12 2a10 10 0 0 1 10 10" stroke="white" strokeWidth="3" strokeLinecap="round" />
                </svg>
                Submitting...
              </>
            ) : (
              'Submit'
            )}
          </button>
        </div>
      </div>

      {/* Spinner keyframes injected once */}
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
};

export default FeedbackModal;

