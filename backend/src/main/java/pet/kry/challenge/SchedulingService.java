package pet.kry.challenge;

import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class SchedulingService {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Europe/Stockholm");
    private static final List<LocalTime> STARTS = List.of(LocalTime.of(8, 0), LocalTime.of(8, 30), LocalTime.of(9, 0), LocalTime.of(9, 30));
    private final Clock clock;
    private final Map<OffsetDateTime, Integer> booked = new HashMap<>();

    public SchedulingService() { this(Clock.system(CLINIC_ZONE)); }
    SchedulingService(Clock clock) { this.clock = clock; }

    public List<OffsetDateTime> availableSlots() {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(CLINIC_ZONE);
        List<OffsetDateTime> result = new ArrayList<>();
        for (int day = 0; day <= 3; day++) {
            LocalDate date = now.toLocalDate().plusDays(day);
            for (LocalTime time = LocalTime.of(8, 0); time.isBefore(LocalTime.of(18, 0)); time = time.plusMinutes(15)) {
                ZonedDateTime start = ZonedDateTime.of(date, time, CLINIC_ZONE);
                if (start.isBefore(now)) continue;
                OffsetDateTime slot = start.toOffsetDateTime();
                if (activeClinicians(time) > booked.getOrDefault(slot, 0)) result.add(slot);
            }
        }
        return result;
    }

    int activeClinicians(LocalTime time) {
        int available = 0;
        for (LocalTime start : STARTS) {
            LocalTime breakStart = start.plusHours(4);
            LocalTime breakEnd = breakStart.plusHours(1);
            LocalTime finish = start.plusHours(9);
            if (!time.isBefore(start) && !time.isAfter(finish) &&
                (time.isBefore(breakStart) || !time.isBefore(breakEnd))) {
                available++;
            }
        }
        return available;
    }

    public String book(OffsetDateTime slot) {
        // The client is expected to choose a slot from the assessment response.
        booked.merge(slot, 1, Integer::sum);
        return UUID.randomUUID().toString();
    }
}
