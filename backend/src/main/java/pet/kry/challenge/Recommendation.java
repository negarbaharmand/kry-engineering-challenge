package pet.kry.challenge;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.Duration;
import java.util.Arrays;

/**
 * Care type recommended by the assessment.
 * Java name is CHAT; the API label (JSON) stays "Chat" so the frontend keeps working.
 */
public enum Recommendation {
    CHAT("Chat", 15),
    NURSE("Nurse", 30),
    DOCTOR("Doctor", 45);

    private final String label;
    private final int durationMinutes;

    Recommendation(String label, int durationMinutes) {
        this.label = label;
        this.durationMinutes = durationMinutes;
    }

    /** Minutes this visit occupies on a clinician's calendar. */
    public Duration duration() {
        return Duration.ofMinutes(durationMinutes);
    }

    /** Value written to / read from JSON (e.g. "Chat"). */
    @JsonValue
    public String label() {
        return label;
    }

    @JsonCreator
    public static Recommendation fromLabel(String label) {
        return Arrays.stream(values())
            .filter(r -> r.label.equals(label))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown recommendation: " + label));
    }

    /** Score bands from the README: 5-7 Chat, 8-11 Nurse, 12-15 Doctor. */
    public static Recommendation fromScore(int score) {
        if (score <= 7) return CHAT;
        if (score <= 11) return NURSE;
        return DOCTOR;
    }
}