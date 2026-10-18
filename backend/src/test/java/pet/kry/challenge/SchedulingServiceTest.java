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
        assertFalse(scheduling.availableSlots().isEmpty());
    }

    @Test void bookingUtcFormRemovesStockholmSlot() {
        OffsetDateTime utcForm = OffsetDateTime.parse("2026-10-12T06:00:00Z");
        OffsetDateTime stockholmForm = OffsetDateTime.parse("2026-10-12T08:00:00+02:00");
        assertEquals(utcForm.toInstant(), stockholmForm.toInstant());
        scheduling.book(utcForm);
        assertFalse(scheduling.availableSlots().contains(stockholmForm),
            "Booking the UTC form should consume the Stockholm slot (same Instant)");
    }
}