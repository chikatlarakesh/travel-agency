import DateDurationFilter from './DateDurationFilter';
import DestinationDropdown from './DestinationDropdown';
import MealFilter from './MealFilter';
import TouristSelector from './TouristSelector';
import TourTypeFilter from './TourTypeFilter';

const FilterBar = ({
  filters,
  destinationOptions,
  destinationQuery,
  loadingDestinations,
  onDestinationQuery,
  onDestinationSelect,
  onFilterChange,
  onSearch,
}) => (
  <div className="flex min-h-[88px] w-full items-center rounded-xl bg-white py-4 px-6 shadow-filter">
    <div className="flex w-full flex-wrap items-center gap-4 xl:flex-nowrap">

      <DestinationDropdown
        options={destinationOptions}
        query={destinationQuery}
        value={filters.destination}
        loading={loadingDestinations}
        onQueryChange={onDestinationQuery}
        onSelect={onDestinationSelect}
      />

      <DateDurationFilter
        startDates={filters.startDates}
        durations={filters.durations}
        onDatesChange={(v) => onFilterChange('startDates', v)}
        onDurationsChange={(v) => onFilterChange('durations', v)}
      />

      <TouristSelector
        value={filters.tourists}
        onChange={(v) => onFilterChange('tourists', v)}
      />

      <MealFilter
        selected={filters.mealPlans}
        onChange={(v) => onFilterChange('mealPlans', v)}
      />

      <TourTypeFilter
        selected={filters.tourTypes}
        onChange={(v) => onFilterChange('tourTypes', v)}
      />

      <button
        type="button"
        onClick={onSearch}
        className="h-[56px] w-[77px] shrink-0 rounded-lg bg-[#027EAC] py-2 px-4 font-nunito text-sm font-bold leading-6 text-white transition hover:bg-primary-dark active:scale-95"
      >
        Search
      </button>
    </div>
  </div>
);

export default FilterBar;
