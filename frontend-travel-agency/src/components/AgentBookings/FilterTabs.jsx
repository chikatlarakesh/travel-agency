import './FilterTabs.css';

const FILTER_TABS = [
  { key: 'all', label: 'All tours' },
  { key: 'booked', label: 'Booked' },
  { key: 'confirmed', label: 'Confirmed' },
  { key: 'started', label: 'Started' },
  { key: 'finished', label: 'Finished' },
  { key: 'canceled', label: 'Canceled' },
];

const FilterTabs = ({ activeTab, onChange }) => (
  <div className="filter-tabs" role="tablist" aria-label="Booking status filters">
    {FILTER_TABS.map(({ key, label }) => {
      const isActive = activeTab === key;

      return (
        <button
          key={key}
          type="button"
          role="tab"
          aria-selected={isActive}
          onClick={() => onChange(key)}
          className={`filter-tabs__tab ${isActive ? 'filter-tabs__tab--active' : ''}`}
        >
          <span className="filter-tabs__label">{label}</span>
          <span className="filter-tabs__indicator" aria-hidden="true" />
        </button>
      );
    })}
  </div>
);

export default FilterTabs;
