import TourCard from './TourCard';

const SkeletonCard = () => (
  <div className="flex w-full animate-pulse flex-col gap-6 rounded-xl bg-white p-6 shadow-[0px_2px_10px_6px_rgba(2,126,172,0.2)] sm:h-[384px] sm:flex-row">
    <div className="h-52 w-full shrink-0 rounded-xl bg-slate-200 sm:h-[336px] sm:w-[232px]" />
    <div className="flex flex-1 flex-col justify-between sm:h-[336px] sm:w-[360px]">
      <div className="flex flex-col gap-3">
      <div className="flex items-start justify-between gap-4">
        <div className="h-5 w-1/2 rounded-lg bg-slate-200" />
        <div className="h-5 w-20 rounded-lg bg-slate-200" />
      </div>
      <div className="h-3.5 w-1/3 rounded-lg bg-slate-100" />
      <div className="flex gap-1">
        {[1,2,3,4,5].map((i) => (
          <div key={i} className="h-3.5 w-3.5 rounded-sm bg-slate-200" />
        ))}
      </div>
      <div className="h-3.5 w-3/4 rounded-lg bg-slate-100" />
      <div className="h-3.5 w-2/3 rounded-lg bg-slate-100" />
      <div className="h-6 w-40 rounded-full bg-slate-100" />
      </div>
      <div className="flex gap-3 pt-4">
        <div className="h-10 flex-1 rounded-xl bg-slate-100" />
        <div className="h-10 flex-1 rounded-xl bg-slate-200" />
      </div>
    </div>
  </div>
);

const EmptyState = () => (
  <div className="col-span-full flex flex-col items-center justify-center gap-5 rounded-xl border-2 border-dashed border-slate-200 bg-white px-8 py-24 text-center">
    <span className="text-5xl">🗺️</span>
    <div>
      <p className="font-nunito text-lg font-bold text-[#0B3857]">No tours found</p>
      <p className="mt-1 font-nunito text-sm text-[#677883]">
        Try adjusting your filters or search for a different destination.
      </p>
    </div>
  </div>
);

const TourList = ({ tours, loading, onSeeDetails, onBookTour }) => (
  <section className="grid grid-cols-1 gap-4 lg:grid-cols-2 lg:gap-8">
    {loading ? (
      Array.from({ length: 4 }).map((_, i) => <SkeletonCard key={i} />)
    ) : !tours.length ? (
      <EmptyState />
    ) : (
      tours.map((tour) => (
        <TourCard
          key={tour.id}
          tour={tour}
          onSeeDetails={onSeeDetails}
          onBookTour={onBookTour}
        />
      ))
    )}
  </section>
);

export default TourList;
