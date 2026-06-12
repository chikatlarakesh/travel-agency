/**
 * StatusStepper — rounded rectangle progress bar
 *
 * Step container spec:
 *   width: 170px; height: 32px; border-radius: 4px;
 *
 * Step states:
 *   completed → blue 1px border | white fill
 *   pending   → gray 1px border | white fill
 *   canceled  → red fill | white text
 */

// ─── Icons ────────────────────────────────────────────────────────────────────

const CheckIcon = () => (
  <div
    style={{
      width: '24px',
      height: '24px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      position: 'relative',
      flexShrink: 0,
    }}
  >
    <svg
      width="16" height="11" viewBox="0 0 16 11"
      fill="none" xmlns="http://www.w3.org/2000/svg"
      style={{
        position: 'absolute',
        top: '6px',
        left: '4px',
      }}
      aria-hidden="true"
    >
      <path
        d="M14.5 1.5L6 10L1.5 5.5"
        stroke="#0B3857" strokeWidth="1.75"
        strokeLinecap="round" strokeLinejoin="round"
      />
    </svg>
  </div>
);

const XIcon = ({ color = '#FFFFFF' }) => (
  <svg
    width="14" height="14" viewBox="0 0 14 14"
    fill="none" xmlns="http://www.w3.org/2000/svg"
    style={{ flexShrink: 0 }}
    aria-hidden="true"
  >
    <path
      d="M2 2L12 12M12 2L2 12"
      stroke={color} strokeWidth="1.75"
      strokeLinecap="round"
    />
  </svg>
);

const ChevronStep = ({ label, state, index, total }) => {
  const isCancStep = state === 'canceled';

  /* ── Colour tokens ── */
  const BLUE = '#027EAC';
  const RED = '#C8001A';
  const GRAY = '#D3E1ED';
  const WHITE = '#FFFFFF';

  const borderCol = state === 'completed' ? BLUE : state === 'canceled' ? RED : GRAY;
  const fillCol = WHITE;
  const textCol = isCancStep ? RED : state === 'completed' ? '#0B3857' : '#677883';

  const STEP_HEIGHT = 32;
  const STEP_WIDTH = 170;
  const POINT = 14;
  const CORNER_RADIUS = 4;
  const TIP_HEAD_CURVE = 2;

  const buildStepPath = (inset = 0) => {
    const x = inset;
    const y = inset;
    const w = STEP_WIDTH - inset * 2;
    const h = STEP_HEIGHT - inset * 2;
    const tip = POINT - inset;
    const r = Math.max(1, CORNER_RADIUS - inset);
    const tipCurve = Math.max(0.8, TIP_HEAD_CURVE - inset * 0.5);

    const shoulderX = x + w - tip;
    const tipX = x + w;
    const midY = y + h / 2;
    const tipJoinX = tipX - tipCurve;

    return [
      `M ${x + r} ${y}`,
      `L ${shoulderX} ${y}`,
      `L ${tipJoinX} ${midY - tipCurve}`,
      `Q ${tipX} ${midY} ${tipJoinX} ${midY + tipCurve}`,
      `L ${shoulderX} ${y + h}`,
      `L ${x + r} ${y + h}`,
      `Q ${x} ${y + h} ${x} ${y + h - r}`,
      `L ${x} ${y + r}`,
      `Q ${x} ${y} ${x + r} ${y}`,
      'Z',
    ].join(' ');
  };

  const outerPath = buildStepPath(0.5);

  return (
    <div
      style={{
        position: 'relative',
        flex: 1,
        minWidth: '120px',
        height: `${STEP_HEIGHT}px`,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        marginLeft: index === 0 ? '0px' : `-${POINT}px`,
        zIndex: total - index,
      }}
    >
      <svg
        width="100%"
        height="100%"
        viewBox={`0 0 ${STEP_WIDTH} ${STEP_HEIGHT}`}
        preserveAspectRatio="none"
        style={{
          position: 'absolute',
          inset: 0,
          overflow: 'visible',
        }}
        aria-hidden="true"
      >
        <path
          d={outerPath}
          fill={fillCol}
          stroke={borderCol}
          strokeWidth="1"
          strokeLinejoin="round"
          vectorEffect="non-scaling-stroke"
        />
      </svg>

      <div
        style={{
          position: 'relative',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '8px',
          zIndex: 2,
          paddingLeft: '12px',
          paddingRight: '18px',
          height: '100%',
        }}
      >
        {state === 'completed' && <CheckIcon />}
        {isCancStep && <XIcon color={RED} />}
        <span
          style={{
            fontFamily: 'Nunito, sans-serif',
            fontSize: '14px',
            fontWeight: '800',
            lineHeight: '24px',
            letterSpacing: '0%',
            color: textCol,
            textAlign: 'center',
            whiteSpace: 'nowrap',
            height: '24px',
          }}
        >
          {label}
        </span>
      </div>
    </div>
  );
};

// ─── Data ─────────────────────────────────────────────────────────────────────

const STEPS = ['Booked', 'Confirmed', 'Started', 'Finished'];
const STATUS_TO_INDEX = { booked: 0, confirmed: 1, started: 2, finished: 3 };

// ─── Component ────────────────────────────────────────────────────────────────

const StatusStepper = ({ status, canceledAfterStep = 1 }) => {
  const isCanceled = status === 'canceled';

  const steps = isCanceled
    ? [
        ...STEPS.slice(0, canceledAfterStep + 1).map((label) => ({ label, state: 'completed' })),
        { label: 'Canceled', state: 'canceled' },
      ]
    : STEPS.map((label, i) => ({
        label,
        state: i <= (STATUS_TO_INDEX[status] ?? 0) ? 'completed' : 'pending',
      }));

  const widthPercent = `${(steps.length / STEPS.length) * 100}%`;

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'row',
        alignItems: 'center',
        height: '32px',
        gap: '0px',
        width: widthPercent,
        maxWidth: '100%',
        paddingLeft: '0px',
        paddingRight: '0px',
        boxSizing: 'border-box',
      }}
    >
      {steps.map(({ label, state }, index) => (
        <ChevronStep
          key={`${label}-${index}`}
          label={label}
          state={state}
          index={index}
          total={steps.length}
        />
      ))}
    </div>
  );
};

export default StatusStepper;
