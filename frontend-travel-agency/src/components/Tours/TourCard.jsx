import { ReactComponent as LocationIcon } from '../../assets/icons/Location.svg';
import { ReactComponent as StarIcon } from '../../assets/icons/star.svg';
import { ReactComponent as TourDetailIcon } from '../../assets/icons/tourdetail.svg';
import { ReactComponent as FoodIcon } from '../../assets/icons/food.svg';
import { ReactComponent as WalletIcon } from '../../assets/icons/wallet.svg';
import { ReactComponent as GreenCheckIcon } from '../../assets/icons/greencheck.svg';
import { ReactComponent as RedInfoIcon } from '../../assets/icons/redinfo.svg';
import { parseCalendarDate, formatCalendarDate } from '../../utils/dateUtils';

const parseLocalDate = parseCalendarDate;

const fmt = (iso) => formatCalendarDate(iso, { month: 'short', day: 'numeric' });

const fmtDurations = (durations = []) => durations.map((d) => `${d} days`).join(', ');
const fmtMeals = (mealPlans = []) => mealPlans.join(', ');

const calculateCancellationStatus = (tour) => {
  // If tour has hardcoded cancellationText, use it (backward compatibility)
  if (tour.cancellationText && !tour.freeCancellationDaysBefore) {
    return {
      canCancel: tour.cancellationPolicy === 'available',
      text: tour.cancellationText
    };
  }
  
  // Calculate dynamically based on freeCancellationDate or freeCancellationDaysBefore
  const freeCancellationDate = tour.freeCancellationDate
    ? parseLocalDate(tour.freeCancellationDate)
    : tour.freeCancellationDaysBefore && tour.dates?.[0]
      ? new Date(parseLocalDate(tour.dates[0]).getTime() - tour.freeCancellationDaysBefore * 24 * 60 * 60 * 1000)
      : null;
  
  if (!freeCancellationDate) {
    return {
      canCancel: false,
      text: 'Free cancellation is no longer available'
    };
  }
  
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  freeCancellationDate.setHours(0, 0, 0, 0);
  
  const canCancel = today < freeCancellationDate;
  const text = canCancel 
    ? `Free cancellation until ${fmt(freeCancellationDate)}`
    : 'Free cancellation is no longer available';
  
  return { canCancel, text };
};

const TourCard = ({ tour, onSeeDetails, onBookTour }) => {
  const { canCancel, text } = calculateCancellationStatus(tour);

  return (
    <article className="group flex w-full flex-col gap-6 rounded-[12px] bg-white p-6 shadow-[0px_2px_10px_6px_rgba(2,126,172,0.2)] transition-shadow hover:shadow-soft sm:h-[384px] sm:flex-row">

      <div className="relative h-52 w-full shrink-0 overflow-hidden rounded-[12px] sm:h-[336px] sm:w-[232px]">
        <img
          src={tour.image}
          alt={tour.name}
          loading="lazy"
          className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
        />
      </div>

      <div className="flex min-w-0 flex-1 flex-col justify-between sm:h-[336px] sm:max-w-[360px]">
        <div className="flex w-full flex-col gap-6">
          <div className="flex min-h-[56px] w-full flex-col gap-2">
            <div className="flex min-h-[48px] w-full items-start justify-between gap-2">
              <div className="flex min-w-0 flex-1 flex-col gap-1">
                <h3 className="font-nunito text-[18px] font-bold leading-6 text-[#0B3857]">
                  {tour.name}
                </h3>

                <div className="flex w-full min-w-0 items-start gap-1">
                  <div className="h-4 w-4 shrink-0 flex items-center justify-center">
                    <LocationIcon className="h-4 w-4 text-[#677883]" />
                  </div>
                  <span className="flex-1 font-nunito text-[12px] font-normal leading-4 text-[#677883]">
                    {tour.location}
                  </span>
                </div>
              </div>

              <div className="flex max-w-[120px] shrink-0 flex-col gap-1 text-right">
                <div className="flex items-center justify-end gap-1">
                  <StarIcon className="h-4 w-4" />
                  <span className="font-nunito text-[12px] font-normal leading-4 text-[#0B3857]">
                    {tour.rating.toFixed(1)}
                  </span>
                </div>
                <span className="font-nunito text-[12px] font-normal leading-4 text-[#0B3857]">
                  {tour.reviews} reviews
                </span>
              </div>
            </div>

          </div>

          <div className="flex w-full flex-col gap-2">
            <div className="flex w-full items-center gap-2">
              <div className="h-4 w-4 shrink-0">
                <TourDetailIcon className="h-4 w-4" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="font-nunito text-[14px] font-normal leading-6 text-[#0B3857]">
                  {tour.dates?.[0] ? fmt(tour.dates[0]) : 'Jan 4'} ({fmtDurations(tour.durations)})
                </p>
              </div>
            </div>

            <div className="flex w-full items-center gap-2">
              <div className="h-4 w-4 shrink-0">
                <FoodIcon className="h-4 w-4" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="font-nunito text-[14px] font-normal leading-6 text-[#0B3857]">
                  {fmtMeals(tour.mealPlans)}
                </p>
              </div>
            </div>

            <div className="flex w-full items-center gap-2">
              <div className="h-4 w-4 shrink-0">
                <WalletIcon className="h-4 w-4" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="font-nunito text-[14px] font-normal leading-6 text-[#0B3857]">
                  From ${tour.price} for 1 person
                </p>
              </div>
            </div>

            <div className="flex w-full items-center gap-2">
              <div className="h-4 w-4 shrink-0">
                {canCancel ? <GreenCheckIcon className="h-4 w-4" /> : <RedInfoIcon className="h-4 w-4" />}
              </div>
              <div className="min-w-0 flex-1">
                <p className={`font-nunito text-[14px] font-normal leading-6 ${
                  canCancel ? 'text-[#118819]' : 'text-[#B70B0B]'
                }`}>
                  {text}
                </p>
              </div>
            </div>
          </div>
        </div>

        <div className="flex h-10 w-full max-w-[360px] justify-end gap-2 self-end">
          <button
            type="button"
            onClick={() => onSeeDetails(tour)}
            className="inline-flex h-10 w-[104px] shrink-0 items-center justify-center gap-1 whitespace-nowrap rounded-lg border-2 border-[#027EAC] bg-white px-4 py-2 font-nunito text-[14px] font-bold leading-6 text-[#027EAC] transition hover:bg-primary-light active:scale-95"
          >
            See details
          </button>
          <button
            type="button"
            onClick={() => onBookTour(tour)}
            className="inline-flex h-10 w-[122px] shrink-0 items-center justify-center gap-1 whitespace-nowrap rounded-lg bg-[#027EAC] px-4 py-2 font-nunito text-[14px] font-bold leading-6 text-white transition hover:bg-primary-dark active:scale-95"
          >
            Book the tour
          </button>
        </div>
      </div>
    </article>
  );
};

export default TourCard;
