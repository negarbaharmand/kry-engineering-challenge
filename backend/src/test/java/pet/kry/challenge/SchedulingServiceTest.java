package pet.kry.challenge;

import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class SchedulingServiceTest {
    private final ZoneId stockholm = ZoneId.of("Europe/Stockholm");
    private final SchedulingService scheduling = new SchedulingService(
        Clock.fixed(Instant.parse("2026-10-12T06:00:00Z"), stockholm));

    @Test void hasCapacityAtOpening() {
        assertEquals(1, scheduling.activeClinicians(LocalTime.of(8, 0)));
    }

    @Test void hasSlotsInTheFuture() {
        assertFalse(scheduling.availableSlots(Recommendation.CHAT).isEmpty());
    }

    @Test void bookingUtcFormRemovesStockholmSlot() {
        OffsetDateTime utcForm = OffsetDateTime.parse("2026-10-12T06:00:00Z");
        OffsetDateTime stockholmForm = OffsetDateTime.parse("2026-10-12T08:00:00+02:00");
        assertEquals(utcForm.toInstant(), stockholmForm.toInstant());
        scheduling.book(utcForm, Recommendation.CHAT);
        assertFalse(scheduling.availableSlots(Recommendation.CHAT).contains(stockholmForm),
            "Booking the UTC form should consume the Stockholm slot (same Instant)");
    }

    @Test void secondBookingOfFullSlotIsRejected() {
        OffsetDateTime opening = OffsetDateTime.parse("2026-10-12T08:00:00+02:00");
        scheduling.book(opening, Recommendation.CHAT);
        BookingRejected error = assertThrows(BookingRejected.class, () -> scheduling.book(opening, Recommendation.CHAT));
        assertEquals(BookingRejected.Reason.FULL, error.reason());
    }

    @Test void bookingPastSlotIsRejected() {
        OffsetDateTime past = OffsetDateTime.parse("2026-10-12T07:45:00+02:00");
        BookingRejected error = assertThrows(BookingRejected.class, () -> scheduling.book(past, Recommendation.CHAT));
        assertEquals(BookingRejected.Reason.PAST, error.reason());
    }

    @Test void capacityDropsAtShiftEnd() {
        assertEquals(3, scheduling.activeClinicians(LocalTime.of(17, 0)));
        assertEquals(2, scheduling.activeClinicians(LocalTime.of(17, 30)));
    }

    // --- durations (per-clinician half-open intervals) ---

    @Test void nurseBookingBlocksOverlappingChatSlot() {
        // Nurse = 30 minutes. At 08:00 only one clinician works.
        // Booking Nurse 08:00-08:30 occupies that person through 08:15.
        OffsetDateTime nurseStart = OffsetDateTime.parse("2026-10-12T08:00:00+02:00");
        OffsetDateTime chatAtQuarterPast = OffsetDateTime.parse("2026-10-12T08:15:00+02:00");

        scheduling.book(nurseStart, Recommendation.NURSE);

        assertFalse(
            scheduling.availableSlots(Recommendation.CHAT).contains(chatAtQuarterPast),
            "A 30-minute Nurse visit must block overlapping Chat starts for that clinician"
        );

        BookingRejected error = assertThrows(
            BookingRejected.class,
            () -> scheduling.book(chatAtQuarterPast, Recommendation.CHAT)
        );
        assertEquals(BookingRejected.Reason.FULL, error.reason());
    }

    @Test void doctorCannotCrossAnIndividualClinicianBreak() {
        // 08:00 clinician morning ends at 12:00. Doctor 11:30->12:15 crosses that break.
        // 08:30 clinician morning ends at 12:30, so they CAN take the same visit.
        assertFalse(scheduling.fitsWorkBlock(
            LocalTime.of(8, 0), LocalTime.of(11, 30), Duration.ofMinutes(45)));
        assertTrue(scheduling.fitsWorkBlock(
            LocalTime.of(8, 30), LocalTime.of(11, 30), Duration.ofMinutes(45)));

        // Therefore 11:30 is still offered for Doctor (via a later clinician).
        OffsetDateTime elevenThirty = OffsetDateTime.parse("2026-10-12T11:30:00+02:00");
        assertTrue(scheduling.availableSlots(Recommendation.DOCTOR).contains(elevenThirty));
        assertDoesNotThrow(() -> scheduling.book(elevenThirty, Recommendation.DOCTOR));
    }

    @Test void doctorThatFitsBeforeBreakIsOffered() {
        // Doctor starting 11:15 ends at 12:00 -> fits in [11:15, 12:00) for the 08:00 clinician.
        OffsetDateTime fits = OffsetDateTime.parse("2026-10-12T11:15:00+02:00");

        assertTrue(
            scheduling.availableSlots(Recommendation.DOCTOR).contains(fits),
            "A 45-minute visit that ends exactly at break start should be allowed"
        );
    }

    @Test void backToBackNurseVisitsAreAllowed() {
        // Half-open intervals: [08:00, 08:30) then [08:30, 09:00) touch but do not overlap.
        OffsetDateTime first = OffsetDateTime.parse("2026-10-12T08:00:00+02:00");
        OffsetDateTime second = OffsetDateTime.parse("2026-10-12T08:30:00+02:00");

        scheduling.book(first, Recommendation.NURSE);
        assertDoesNotThrow(() -> scheduling.book(second, Recommendation.NURSE));
    }
}