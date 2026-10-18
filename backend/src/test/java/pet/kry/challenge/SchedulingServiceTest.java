package pet.kry.challenge;

import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class SchedulingServiceTest {
    private final SchedulingService scheduling = new SchedulingService(
        Clock.fixed(Instant.parse("2026-10-12T06:00:00Z"), ZoneId.of("Europe/Stockholm")));

    @Test void hasCapacityAtOpening() {
        assertEquals(1, scheduling.activeClinicians(LocalTime.of(8, 0)));
    }

    @Test void hasSlotsInTheFuture() {
        assertFalse(scheduling.availableSlots().isEmpty());
    }
}
