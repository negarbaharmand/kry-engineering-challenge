# HANDOVER

## What I prioritized and why

Booking correctness came first. The same slot could be booked twice, past and full slots returned 200, and a clinician was still counted at their exact shift-end minute. Any new feature on top of that would be misleading.

Fixes in order:

- Bookings are half-open intervals on one clinician, compared as `Instant`, so `06:00Z` and `08:00+02:00` are the same moment
- `book()` made `synchronized` with explicit validation: rejects past, off-grid, and full slots with 400/409
- Shift-end intervals made half-open `[start, finish)`; no phantom capacity at 17:00/17:30

Then I built **Scenario B**.

## Most important design decision

Each booking assigns **one clinician** a half-open interval `[start, start+duration)` that must fit entirely inside their morning or afternoon block; no crossing breaks or shift ends.

**Rejected:** mark the next N 15-minute buckets busy. Simpler, but one 45-minute Doctor visit could be covered by three different clinicians. That's not a real booking model.

## What I left unfinished

- Cancellation/rescheduling (A) and multiple clinics (C), because the brief says to ship one scenario
- Persistence; in-memory is fine for a POC
- Frontend left as-is, because the brief says not to redesign the UI. The slot dropdown still caps at 60, and booking errors stay generic
- Concurrency stress tests

## How I verified

**Actually ran:**

```sh
cd backend && ./gradlew test --rerun-tasks
# BUILD SUCCESSFUL — 10 tests, 0 failures
```

Tests cover: timezone round-trip, past and full rejection, shift-end boundary, Nurse overlap, Doctor break-fit, back-to-back Nurses. In the UI I booked 11:15 four times (four clinicians) and a 17:00 Doctor slot twice, then the next attempt failed.

**Would run with more time:** an off-grid booking (expect 400), a parallel double-book race, and a DST boundary check (2026-10-25).
