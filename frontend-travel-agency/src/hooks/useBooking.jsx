import { useState } from 'react';

const useBooking = () => {
  const [booking, setBooking] = useState(null);

  // Add booking logic here

  return { booking, setBooking };
};

export default useBooking;

