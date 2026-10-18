# Kry engineering challenge — rough POC

> This is a fictional exercise, **not a clinical application**. The symptom scoring is not medically validated and must not be used for patient care.

## Your mission

You've inherited a proof-of-concept application for patient assessment and appointment booking. It works at a basic level, but it was built quickly: some behavior is incorrect, some implementations are shallow, and some decisions are questionable.

**Aim for 90 minutes; stop after 2 hours maximum.** We do not expect a complete or production-ready result. **Use AI tools freely—we expect AI-assisted development.** You are responsible for understanding and verifying the changes you submit.

Do three things, in whatever order you think delivers the most value:

1. **Improve the POC:** investigate the existing application and fix the issues you consider most important.
2. **Build one feature:** choose **exactly one** scenario below and implement a useful, coherent part of it. You do **not** need to complete the entire scenario.
3. **Explain your choices:** tell us what you prioritized, why you designed the feature that way, what you left out, and how you checked your work.

We care more about sound decisions and demonstrated behavior than the number of fixes or features.

## Choose one feature scenario

### A. Cancellation and rescheduling

Patients want to cancel an existing appointment or move it to another available time without contacting support.

Choose the most valuable part to deliver. Consider how a booking is identified, how cancelled capacity becomes available, and what should happen if a requested replacement slot is no longer free. Cancellation alone, a carefully scoped rescheduling flow, or another justified slice may all be reasonable.

### B. Different appointment durations

The service now needs **15-minute Chat**, **30-minute Nurse**, and **45-minute Doctor** appointments. The available appointments should reflect the recommendation.

Choose a useful slice of this change. Consider how bookings occupy clinician time, what happens near breaks and shift ends, and how to avoid overlapping appointments. A simple time-block model and a more general interval model can both be defensible.

### C. Multiple clinics

The service should offer appointments at multiple clinics. Each clinic has its own opening hours, clinicians, and availability; patients should be able to choose a clinic.

Choose a useful slice of this change. Consider where clinic information lives, how availability and bookings are isolated, and how much configuration a POC needs. Hardcoded sample clinics are acceptable if you explain the trade-off.

**For any scenario:** make a decision rather than trying to implement everything. There is no single expected design. Be ready to explain an alternative you considered and why you didn't choose it.

## Submission

Submit your code changes and a short `HANDOVER.md` (roughly **300 words or fewer**) answering:

1. What did you prioritize, and why?
2. What was your most important design decision for your chosen scenario? What alternative did you reject?
3. What did you intentionally leave unfinished, and why?
4. How did you verify your changes? Include commands and **actual** results; distinguish tests you ran from tests you would run.

Do not spend time polishing documentation or redesigning the whole UI. You may use AI to write code and tests; be ready to explain the result in a follow-up interview, including a small new requirement.

## Running the POC

Prerequisites: Java 21+, Node.js 20+, npm.

```sh
cd backend && ./gradlew test && ./gradlew bootRun
# in another terminal
cd frontend && npm install && npm run dev
```

Open http://localhost:5173. Vite proxies `/api` to http://localhost:8080.

## Existing product behavior (intended)

- Five questions, with back navigation and restart; each answer scores 1–3.
- Scores 5–7 recommend Chat, 8–11 Nurse, and 12–15 Doctor.
- The API accepts individual answers rather than a client-supplied score.
- The clinic uses the `Europe/Stockholm` timezone. Appointments start on 15-minute boundaries between 08:00 and 18:00, today and the next three calendar days.
- Four clinicians start at 08:00, 08:30, 09:00, and 09:30. Each works four active hours, takes a one-hour break, then works four more active hours.
- Availability depends on clinician capacity and existing bookings. Booking an appointment should consume capacity.
- The POC uses in-memory booking state. You do not need to add a database unless you believe it is the best use of your limited time.

### Existing API

`POST /api/assessment` accepts `{"answers":[1,2,3,1,2]}` and returns a score, recommendation, and available slots.

`POST /api/booking` accepts `{"slot":"<ISO-8601 appointment time>","recommendation":"Nurse"}` and returns a confirmation ID, slot, and recommendation. Invalid requests should produce useful 4xx responses.

These describe intended behavior, not a guarantee that the POC implements everything correctly.
