package pet.kry.challenge;

import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class SchedulingService {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Europe/Stockholm");
    private static final List<LocalTime> STARTS = List.of(LocalTime.of(8, 0), LocalTime.of(8, 30), LocalTime.of(9, 0), LocalTime.of(9, 30));
    private final Clock clock;
    // Key by Instant (= the moment itself), not OffsetDateTime (= how the time is written).
    // So 08:00+02:00 and 06:00Z share one entry.
    private final Map<Instant, Integer> booked = new HashMap<>();

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
                OffsetDateTime slot = start.toOffsetDateTime(); // still Stockholm form for the API / UI
                Instant when = slot.toInstant();               // map key = the moment
                if (activeClinicians(time) > booked.getOrDefault(when, 0)) result.add(slot);
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

    /**
     * synchronized = only one thread runs this method at a time on this service instance.
     * That way two requests cannot both pass the "is there a free seat?" check and overbook.
     */
    public synchronized String book(OffsetDateTime slot) {
        Instant when = slot.toInstant();

        // 1) Refuse times already in the past (compared to our clock).
        if (when.isBefore(clock.instant())) {
            throw new BookingRejected(BookingRejected.Reason.PAST);
        }

        // 2) Interpret the moment in the clinic timezone to check the local wall clock.
        LocalTime time = when.atZone(CLINIC_ZONE).toLocalTime();
        boolean onGrid = time.getMinute() % 15 == 0
            && time.getSecond() == 0
            && time.getNano() == 0
            && !time.isBefore(LocalTime.of(8, 0))
            && time.isBefore(LocalTime.of(18, 0));
        int capacity = activeClinicians(time);
        if (!onGrid || capacity == 0) {
            throw new BookingRejected(BookingRejected.Reason.INVALID);
        }

        // 3) Refuse if every seat at this moment is already taken.
        int alreadyBooked = booked.getOrDefault(when, 0);
        if (alreadyBooked >= capacity) {
            throw new BookingRejected(BookingRejected.Reason.FULL);
        }

        // 4) Reserve one seat. Instant key = UTC and Stockholm writings share capacity.
        booked.merge(when, 1, Integer::sum);
        return UUID.randomUUID().toString();
    }
}