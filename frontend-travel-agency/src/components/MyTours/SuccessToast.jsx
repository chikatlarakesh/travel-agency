import { useCallback, useEffect, useState } from 'react';
import { ReactComponent as CheckCircleIcon } from '../../assets/icons/Check Circle.svg';
import { ReactComponent as CloseIcon } from '../../assets/icons/Close.svg';

const SuccessToast = ({ message = 'All changes has been saved successfully.', onClose }) => {
  const [isVisible, setIsVisible] = useState(true);

  const handleClose = useCallback(() => {
    setIsVisible(false);
    setTimeout(onClose, 300);
  }, [onClose]);

  // Auto-dismiss after 4 seconds.
  useEffect(() => {
    const t = setTimeout(handleClose, 4000);
    return () => clearTimeout(t);
  }, [handleClose]);

  return (
    <div
      className="fixed z-[60] flex items-start"
      style={{
        width: '406px',
        maxWidth: 'calc(100vw - 32px)',
        height: '76px',
        top: '88px',
        right: '16px',
        borderRadius: '4px',
        padding: '12px',
        gap: '8px',
        background: '#EDFFEE',
        border: '1px solid #118819',
        transition: 'opacity 300ms ease-out, transform 300ms ease-out',
        opacity: isVisible ? 1 : 0,
        transform: isVisible ? 'translateY(0)' : 'translateY(-6px)',
      }}
    >
      <div className="flex" style={{ width: '350px', height: '52px', gap: '8px' }}>
        <div className="shrink-0" style={{ width: '24px', height: '24px' }}>
          <CheckCircleIcon width={24} height={24} />
        </div>

        <div className="flex flex-col" style={{ width: '318px', height: '52px', gap: '4px' }}>
          <p
            className="font-nunito"
            style={{
              width: '52px',
              height: '24px',
              color: '#0B3857',
              fontSize: '14px',
              fontWeight: 800,
              lineHeight: '24px',
              letterSpacing: '0px',
            }}
          >
            Success
          </p>

          <p
            className="font-nunito"
            style={{
              width: '318px',
              height: '24px',
              color: '#0B3857',
              fontSize: '14px',
              fontWeight: 400,
              lineHeight: '24px',
              letterSpacing: '0px',
              whiteSpace: 'nowrap',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
            }}
          >
            {message}
          </p>
        </div>
      </div>

      <button
        type="button"
        onClick={handleClose}
        className="flex shrink-0 items-center justify-center"
        style={{ width: '24px', height: '24px', transition: 'all 300ms ease-out' }}
        aria-label="Dismiss"
      >
        <CloseIcon width={24} height={24} />
      </button>
    </div>
  );
};

export default SuccessToast;
