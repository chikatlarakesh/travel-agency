package com.epam.edp.demo.mapper;

import com.epam.edp.demo.dto.booking.BookedTourDTO;
import com.epam.edp.demo.entity.Booking;
import com.epam.edp.demo.entity.Destination;
import com.epam.edp.demo.entity.GuestCount;
import com.epam.edp.demo.entity.PersonDetail;
import com.epam.edp.demo.entity.PricingOption;
import com.epam.edp.demo.entity.Tour;
import com.epam.edp.demo.entity.TourInstance;
import com.epam.edp.demo.entity.TravelAgent;
import com.epam.edp.demo.enums.BookingStatus;
import com.epam.edp.demo.util.MealPlanFormatter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(JUnit4.class)
public class BookingMapperTest {

    private BookingMapper mapper;

    @Before
    public void setUp() {
        mapper = new BookingMapper(new MealPlanFormatter());
    }

    // ── toBookedTourDTO ───────────────────────────────────────────────────────

    @Test
    public void toBookedTourDTO_mapsAllFieldsCorrectly() {
        Booking booking = booking("b-1", "u-1", "t-1", "i-1", BookingStatus.BOOKED,
                "2027-06-15", "7 days", "BB");

        Tour tour = Tour.builder()
                .id("t-1")
                .name("Garden Resort")
                .destination(new Destination("Punta Cana", "Dominican Republic"))
                .imageUrls(List.of("http://img1.jpg", "http://img2.jpg"))
                .build();

        PricingOption pricing = new PricingOption();
        pricing.setDuration(7);
        pricing.setPricePerPerson(new BigDecimal("1400"));

        TourInstance instance = TourInstance.builder()
                .id("i-1")
                .tourId("t-1")
                .startDate(LocalDate.of(2027, 6, 15))
                .pricing(List.of(pricing))
                .build();

        TravelAgent agent = new TravelAgent();
        agent.setName("Alice");
        agent.setEmail("alice@agency.com");
        agent.setPhone("+1234567890");
        agent.setMessenger("Skype");

        BookedTourDTO dto = mapper.toBookedTourDTO(booking, tour, List.of(instance), agent);

        assertNotNull(dto);
        assertEquals("b-1", dto.getId());
        assertEquals("BOOKED", dto.getState());
        assertEquals("http://img1.jpg", dto.getTourImageUrl());
        assertEquals("Garden Resort", dto.getName());
        assertEquals("Punta Cana, Dominican Republic", dto.getDestination());
        assertNotNull(dto.getTourDetails());
        assertNotNull(dto.getTravelAgent());
        assertEquals("Alice", dto.getTravelAgent().getName());
    }

    @Test
    public void toBookedTourDTO_nullDestination_emptyString() {
        Booking booking = booking("b-1", "u-1", "t-1", "i-1", BookingStatus.BOOKED,
                "2027-06-15", "7 days", "BB");

        Tour tour = Tour.builder()
                .id("t-1")
                .name("Mystery Tour")
                .destination(null)
                .imageUrls(Collections.emptyList())
                .build();

        TravelAgent agent = new TravelAgent();
        agent.setName("Bob");

        BookedTourDTO dto = mapper.toBookedTourDTO(booking, tour, Collections.emptyList(), agent);

        assertNotNull(dto);
        assertEquals("", dto.getDestination());
        assertNull(dto.getTourImageUrl());
        assertEquals("$0", dto.getTourDetails().getTotalPrice());
    }

    @Test
    public void toBookedTourDTO_noMatchingInstancePricing_returnsZeroPrice() {
        Booking booking = booking("b-1", "u-1", "t-1", "i-1", BookingStatus.CONFIRMED,
                "2027-07-01", "10 days", "HB");

        Tour tour = Tour.builder().id("t-1").name("Beach Tour")
                .destination(new Destination("Cancun", "Mexico"))
                .imageUrls(List.of("img.jpg"))
                .build();

        PricingOption pricing = new PricingOption();
        pricing.setDuration(7); // booking wants 10 days but only 7 available
        pricing.setPricePerPerson(new BigDecimal("1200"));

        TourInstance instance = TourInstance.builder()
                .id("i-1")
                .startDate(LocalDate.of(2027, 7, 1))
                .pricing(List.of(pricing))
                .build();

        TravelAgent agent = new TravelAgent();
        agent.setName("Carol");

        BookedTourDTO dto = mapper.toBookedTourDTO(booking, tour, List.of(instance), agent);

        assertEquals("$0", dto.getTourDetails().getTotalPrice());
    }

    @Test
    public void toBookedTourDTO_nullPricing_returnsZeroPrice() {
        Booking booking = booking("b-1", "u-1", "t-1", "i-1", BookingStatus.BOOKED,
                "2027-06-15", "7 days", "BB");

        Tour tour = Tour.builder().id("t-1").name("Tour")
                .destination(new Destination("City", "Country"))
                .imageUrls(List.of("img.jpg"))
                .build();

        TourInstance instance = TourInstance.builder()
                .id("i-1")
                .startDate(LocalDate.of(2027, 6, 15))
                .pricing(null)
                .build();

        TravelAgent agent = new TravelAgent();
        agent.setName("Dave");

        BookedTourDTO dto = mapper.toBookedTourDTO(booking, tour, List.of(instance), agent);

        assertEquals("$0", dto.getTourDetails().getTotalPrice());
    }

    @Test
    public void toBookedTourDTO_multipleGuests_formatsProperly() {
        Booking booking = booking("b-1", "u-1", "t-1", "i-1", BookingStatus.BOOKED,
                "2027-06-15", "7 days", "AI");
        booking.setGuests(new GuestCount(2, 1));
        booking.setPersonalDetails(List.of(
                new PersonDetail("Jane", "Doe"),
                new PersonDetail("Tom", "Doe")));

        Tour tour = Tour.builder().id("t-1").name("Tour")
                .destination(new Destination("Rome", "Italy"))
                .imageUrls(List.of("img.jpg"))
                .build();

        TravelAgent agent = new TravelAgent();
        agent.setName("Eva");

        BookedTourDTO dto = mapper.toBookedTourDTO(booking, tour, Collections.emptyList(), agent);

        assertNotNull(dto.getTourDetails().getGuests());
        // should contain "2 adults" and "1 child" in guests text
        String guests = dto.getTourDetails().getGuests();
        assertEquals(true, guests.contains("adults"));
        assertEquals(true, guests.contains("child"));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private Booking booking(String id, String userId, String tourId, String instanceId,
                             BookingStatus status, String date, String duration, String mealPlan) {
        Booking b = new Booking();
        b.setId(id);
        b.setUserId(userId);
        b.setTourId(tourId);
        b.setTourInstanceId(instanceId);
        b.setState(status);
        b.setDate(date);
        b.setDuration(duration);
        b.setMealPlan(mealPlan);
        b.setGuests(new GuestCount(1, 0));
        b.setPersonalDetails(List.of(new PersonDetail("John", "Doe")));
        return b;
    }
}
