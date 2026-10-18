package pet.kry.challenge;

import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class SchedulingService {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Europe/Stockholm");
    private static final List<LocalTime> STARTS = List.of(
        LocalTime.of(8, 0), LocalTime.of(8, 30), LocalTime.of(9, 0), LocalTime.of(9, 30));

    private final Clock clock;
    // One booking list per clinician (same order as STARTS). Each booking is [start, end).
    private final List<List<Interval>> bookingsByClinician = List.of(
        new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

    public SchedulingService() { this(Clock.system(CLINIC_ZONE)); }
    SchedulingService(Clock clock) { this.clock = clock; }

    /** Half-open time range on the timeline (Instant = timezone-safe). */
    private record Interval(Instant start, Instant end) {
        boolean overlaps(Interval other) {
            return start.isBefore(other.end) && other.start.isBefore(end);
        }
    }

    public List<OffsetDateTime> availableSlots(Recommendation recommendation) {
        Objects.requireNonNull(recommendation, "recommendation");
        Duration duration = recommendation.duration();
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(CLINIC_ZONE);
        List<OffsetDateTime> result = new ArrayList<>();
        for (int day = 0; day <= 3; day++) {
            LocalDate date = now.toLocalDate().plusDays(day);
            for (LocalTime time = LocalTime.of(8, 0); time.isBefore(LocalTime.of(18, 0)); time = time.plusMinutes(15)) {
                ZonedDateTime start = ZonedDateTime.of(date, time, CLINIC_ZONE);
                if (start.isBefore(now)) continue;
                if (findFreeClinician(date, time, duration).isPresent()) {
                    result.add(start.toOffsetDateTime());
                }
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
            boolean morning = !time.isBefore(start) && time.isBefore(breakStart);
            boolean afternoon = !time.isBefore(breakEnd) && time.isBefore(finish);
            if (morning || afternoon) {
                available++;
            }
        }
        return available;
    }

    /**
     * True if the whole visit [visitStart, visitStart+duration) sits inside one work block
     * for this clinician (morning or afternoon) - cannot cross break or shift end.
     */
    boolean fitsWorkBlock(LocalTime shiftStart, LocalTime visitStart, Duration duration) {
        LocalTime visitEnd = visitStart.plus(duration);
        LocalTime breakStart = shiftStart.plusHours(4);
        LocalTime breakEnd = breakStart.plusHours(1);
        LocalTime finish = shiftStart.plusHours(9);
        boolean morning = !visitStart.isBefore(shiftStart) && !visitEnd.isAfter(breakStart);
        boolean afternoon = !visitStart.isBefore(breakEnd) && !visitEnd.isAfter(finish);
        return morning || afternoon;
    }

    private OptionalInt findFreeClinician(LocalDate date, LocalTime visitStart, Duration duration) {
        Interval visit = intervalOn(date, visitStart, duration);
        for (int i = 0; i < STARTS.size(); i++) {
            if (fitsWorkBlock(STARTS.get(i), visitStart, duration) && isFree(i, visit)) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    private boolean anyClinicianCanFit(LocalTime visitStart, Duration duration) {
        return STARTS.stream().anyMatch(s -> fitsWorkBlock(s, visitStart, duration));
    }

    private boolean isFree(int clinicianIndex, Interval visit) {
        return bookingsByClinician.get(clinicianIndex).stream().noneMatch(visit::overlaps);
    }

    private Interval intervalOn(LocalDate date, LocalTime visitStart, Duration duration) {
        Instant start = ZonedDateTime.of(date, visitStart, CLINIC_ZONE).toInstant();
        return new Interval(start, start.plus(duration));
    }

    /**
     * synchronized = only one thread runs this method at a time on this service instance.
     * Check-and-reserve stays atomic so two requests cannot take the same clinician slot.
     */
    public synchronized String book(OffsetDateTime slot, Recommendation recommendation) {
        Objects.requireNonNull(recommendation, "recommendation");
        Duration duration = recommendation.duration();
        Instant when = slot.toInstant();

        if (when.isBefore(clock.instant())) {
            throw new BookingRejected(BookingRejected.Reason.PAST);
        }

        ZonedDateTime zoned = when.atZone(CLINIC_ZONE);
        LocalDate date = zoned.toLocalDate();
        LocalTime time = zoned.toLocalTime();
        boolean onGrid = time.getMinute() % 15 == 0
            && time.getSecond() == 0
            && time.getNano() == 0
            && !time.isBefore(LocalTime.of(8, 0))
            && time.isBefore(LocalTime.of(18, 0));
        if (!onGrid) {
            throw new BookingRejected(BookingRejected.Reason.INVALID);
        }

        // Nobody's shift can hold this length at this start -> bad request.
        if (!anyClinicianCanFit(time, duration)) {
            throw new BookingRejected(BookingRejected.Reason.INVALID);
        }

        OptionalInt clinician = findFreeClinician(date, time, duration);
        if (clinician.isEmpty()) {
            throw new BookingRejected(BookingRejected.Reason.FULL);
        }

        bookingsByClinician.get(clinician.getAsInt()).add(intervalOn(date, time, duration));
        return UUID.randomUUID().toString();
    }
}