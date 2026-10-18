import React, { useState } from 'react';
import { createRoot } from 'react-dom/client';
import './style.css';

type Assessment = { score: number; recommendation: string; availableSlots: string[] };
type Confirmation = { confirmationId: string; slot: string; recommendation: string };
const questions = [
  { title: 'How would you describe your main symptom?', options: ['Mild discomfort', 'Moderate pain', 'Severe pain'] },
  { title: 'How long have you had this symptom?', options: ['Less than 24 hours', '1–3 days', 'More than 3 days'] },
  { title: 'Does it affect your daily activities?', options: ['Not at all', 'Somewhat', 'Significantly'] },
  { title: 'Have you experienced this before?', options: ['Yes, resolved on its own', 'Yes, needed treatment', 'No, this is new'] },
  { title: 'Do you have fever above 38°C, difficulty breathing, or chest pain?', options: ['None of these', 'One of these', 'Two or more'] },
];

async function post<T>(url: string, body: unknown): Promise<T> {
  const response = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
  if (!response.ok) throw new Error(`Request failed (${response.status})`);
  return response.json() as Promise<T>;
}
function App() {
  const [step, setStep] = useState(-1);
  const [answers, setAnswers] = useState<number[]>(Array(5).fill(0));
  const [assessment, setAssessment] = useState<Assessment | null>(null);
  const [slot, setSlot] = useState('');
  const [confirmation, setConfirmation] = useState<Confirmation | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  function reset() { setStep(-1); setAnswers(Array(5).fill(0)); setAssessment(null); setSlot(''); setConfirmation(null); setError(''); }
  async function submit() {
    setBusy(true); setError('');
    try { setAssessment(await post<Assessment>('/api/assessment', { answers })); }
    catch (e) { setError(e instanceof Error ? e.message : 'Assessment failed'); }
    finally { setBusy(false); }
  }
  async function book() {
    if (!slot || !assessment) return;
    setBusy(true); setError('');
    try { setConfirmation(await post<Confirmation>('/api/booking', { slot, recommendation: assessment.recommendation })); }
    catch (e) { setError(e instanceof Error ? e.message : 'Booking failed'); }
    finally { setBusy(false); }
  }
  return <main className="card">
    <header><span className="eyebrow">Engineering exercise · fictional clinic</span><h1>Appointment guidance</h1><p>This is a prototype, not medical advice. For urgent symptoms seek appropriate emergency care.</p></header>
    {step >= 0 && <button className="secondary reset" onClick={reset}>Cancel and restart</button>}
    {error && <p role="alert" className="error">{error}</p>}
    {step === -1 && <section><h2>Get started</h2><p>Answer five questions to explore the booking flow.</p><button onClick={() => setStep(0)}>Start questionnaire</button></section>}
    {step >= 0 && !assessment && <section><p className="eyebrow">Question {step + 1} of 5</p><progress max={5} value={step + 1}/><h2>{questions[step].title}</h2><fieldset><legend>Choose one answer</legend>{questions[step].options.map((option, index) => <label key={option} className="choice"><input type="radio" name={`q${step}`} checked={answers[step] === index + 1} onChange={() => setAnswers(prev => prev.map((v, i) => i === step ? index + 1 : v))}/>{option}</label>)}</fieldset><div className="actions"><button className="secondary" disabled={step === 0 || busy} onClick={() => setStep(step - 1)}>Back</button>{step < 4 ? <button disabled={!answers[step]} onClick={() => setStep(step + 1)}>Next</button> : <button disabled={busy || answers.some(x => !x)} onClick={submit}>{busy ? 'Loading…' : 'See recommendation'}</button>}</div></section>}
    {assessment && !confirmation && <section><h2>Recommended: {assessment.recommendation}</h2><p>Exercise score: {assessment.score}. This recommendation is not clinically validated.</p><label htmlFor="slot">Available appointments</label><select id="slot" value={slot} onChange={e => setSlot(e.target.value)}><option value="">Choose a slot</option>{assessment.availableSlots.slice(0, 60).map(s => <option key={s} value={s}>{new Date(s).toLocaleString('en-GB', { timeZone: 'Europe/Stockholm', dateStyle: 'medium', timeStyle: 'short' })} (Stockholm)</option>)}</select><button disabled={!slot || busy} onClick={book}>{busy ? 'Booking…' : 'Confirm booking'}</button></section>}
    {confirmation && <section><h2>Booking confirmed</h2><p>Reference: <code>{confirmation.confirmationId}</code></p><p>{new Date(confirmation.slot).toLocaleString('en-GB', { timeZone: 'Europe/Stockholm', dateStyle: 'full', timeStyle: 'short' })}</p><button onClick={reset}>Start again</button></section>}
  </main>;
}

createRoot(document.getElementById('root')!).render(<React.StrictMode><App /></React.StrictMode>);
