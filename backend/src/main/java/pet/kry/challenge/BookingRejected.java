package pet.kry.challenge;

/**
 * Thrown when a booking cannot be accepted.
 * The controller will map FULL -> HTTP 409 and the others -> HTTP 400.
 */
public class BookingRejected extends RuntimeException {
  public enum Reason { PAST, FULL, INVALID }

  private final Reason reason;

  public BookingRejected(Reason reason) {
      super(reason.name());
      this.reason = reason;
  }

  public Reason reason() {
      return reason;
  }
}