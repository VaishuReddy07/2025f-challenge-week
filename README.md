# Fitness Tracker

A workout tracking application with a Python/Flask + MySQL backend and native mobile frontends (Android or iOS).

## Project structure

```
backend/          Python/Flask API + MySQL database
android/          Android app (Java)
ios/              iOS app (Swift)
```

## Prerequisites

- Python 3.10+
- MySQL 8.0+ (must be installed and running)

## Getting started

### Backend

Make sure MySQL is running locally before starting the backend. By default the app connects as `root` with no password to a database called `fitness_tracker`. You can override this with environment variables: `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`.

```bash
cd backend
python -m venv venv
source venv/bin/activate   # Windows: venv\Scripts\activate
pip install -r requirements.txt
python seed.py             # populate the database with sample data
python app.py              # starts on http://localhost:5000
```

### Android

1. Open the `android/` folder in Android Studio.
2. Make sure the backend is running.
3. Run the app on an emulator (the API client uses `10.0.2.2:5000` to reach the host).

### iOS

1. See `ios/README-iOS.md` for Xcode setup instructions.
2. Make sure the backend is running on `localhost:5000`.
3. Run the app on the iOS Simulator.

## API endpoints (implemented)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/exercises` | List all exercises |
| GET | `/workouts` | List all workouts (date + duration) |
| POST | `/workouts` | Create a basic workout (`{ "date", "duration_min", "notes" }`) |

## Running the tests

```bash
cd backend
pytest -v
```

Three tests pass out of the box. Five tests are expected to fail — they cover features you will build during the week.

---

## TODO — Build Week Tasks

### TODO 1: Workout detail

**Backend:** Add `GET /workouts/<id>` that returns the workout together with its exercises. Use a JOIN query across `workouts` and `workout_exercises` and `exercises` to build the response.

**Mobile:** When the user taps a workout in the history list, navigate to the detail screen (`WorkoutDetailActivity` / `WorkoutDetailViewController`) and display the list of exercises with sets, reps, and weight.

**Test:** `test_workout_detail`

---

### TODO 2: Create a full workout

**Backend:** Extend `POST /workouts` so that when the request body contains an `exercises` array, each entry is inserted into `workout_exercises` inside a single database transaction. Roll back if anything fails.

Example request body:
```json
{
  "date": "2026-03-28",
  "duration_min": 60,
  "notes": "Push day",
  "exercises": [
    { "exercise_id": 1, "sets": 4, "reps": 8, "weight_kg": 70.0 },
    { "exercise_id": 5, "sets": 3, "reps": 10, "weight_kg": 50.0 }
  ]
}
```

**Mobile:** Build a "New Workout" screen where the user picks exercises from the list, enters sets/reps/weight for each, and submits the whole workout.

**Test:** `test_create_workout_with_exercises`

---

### TODO 3: Statistics

**Backend:** Add `GET /stats` that returns aggregate data using SQL `GROUP BY`, `COUNT`, and `AVG`:
- `total_workouts` — total number of workouts
- `total_duration_min` — sum of all workout durations
- `avg_duration_min` — average workout duration
- `workouts_per_category` — number of workouts that include at least one exercise from each category

**Mobile:** Add a Statistics screen (new tab or button) that displays the stats in a readable layout (cards, simple charts, or formatted text).

**Test:** `test_stats`

---

### TODO 4: Filter workouts by date range

**Backend:** Update `GET /workouts` to accept optional `from` and `to` query parameters. When provided, filter workouts using `WHERE date >= %s AND date <= %s`.

**Mobile:** Add a date-range picker (two date pickers or preset buttons like "This week" / "This month" / "All") above the workout history list. Re-fetch the list with the selected range.

**Test:** `test_filter_workouts_by_date`

---

### TODO 5: Delete a workout

**Backend:** Add `DELETE /workouts/<id>`. Delete the workout and all its linked `workout_exercises` rows. Use `ON DELETE CASCADE` or explicit deletes inside a transaction.

**Mobile:** Add a delete action to the workout detail or history screen (swipe-to-delete, long press, or a button). Show a confirmation dialog before deleting.

**Test:** `test_delete_workout`


---

## SE TODOs (40)


### Core (01-15)

| Code | TODO |
|------|------|
| FTRC-S001 | [API] GET /workouts/:id with JOIN (nested exercises) |
| FTRC-S002 | [API] Each exercise: name, category, sets, reps, weight |
| FTRC-S003 | [Mobile] Workout detail screen, tap from history |
| FTRC-S004 | [Mobile] Display date, duration, notes, exercise list |
| FTRC-S005 | [Mobile] Total volume: SUM(sets × reps × weight) |
| FTRC-S006 | [Mobile] Group exercises by category + section headers |
| FTRC-S007 | [API] POST /workouts with exercises array |
| FTRC-S008 | [API] SQL transaction: INSERT workout → exercises |
| FTRC-S009 | [API] Validate: date, duration>0, ≥1 exercise |
| FTRC-S010 | [Mobile] Creation: Step 1 select exercises |
| FTRC-S011 | [Mobile] Step 2: sets, reps, weight per exercise |
| FTRC-S012 | [Mobile] Date picker + duration + notes |
| FTRC-S013 | [Mobile] Save button, assemble JSON, POST |
| FTRC-S014 | [Mobile] On success, navigate to history |
| FTRC-S015 | [API] GET /stats: total, sum duration, avg, frequent |

### Intermediate (16-25)

| Code | TODO |
|------|------|
| FTRC-S016 | [API] most_frequent_exercise (JOIN + GROUP BY) |
| FTRC-S017 | [API] workouts_this_week, workouts_this_month |
| FTRC-S018 | [API] total_volume across all workouts |
| FTRC-S019 | [Mobile] Stats screen: metric cards |
| FTRC-S020 | [Mobile] 'Start tracking!' if empty |
| FTRC-S021 | [API] GET /workouts with from/to date params |
| FTRC-S022 | [Mobile] History filter: Week/Month/3 Months/All |
| FTRC-S023 | [Mobile] Count label + empty state per filter |
| FTRC-S024 | [API] DELETE /workouts/:id (transaction) |
| FTRC-S025 | [Mobile] Delete button + confirmation dialog |

### Advanced (26-35)

| Code | TODO |
|------|------|
| FTRC-S026 | [Mobile] Swipe-to-delete on history |
| FTRC-S027 | [API] GET /exercises/:id/history |
| FTRC-S028 | [Mobile] Exercise history on tap |
| FTRC-S029 | [Mobile] Progress text: 'bench went 40→60kg' |
| FTRC-S030 | [API] Category filter on GET /exercises |
| FTRC-S031 | [Mobile] Category filter chips on exercises |
| FTRC-S032 | [Mobile] Pull-to-refresh on both tabs |
| FTRC-S033 | [Mobile] Compact history cards: date, duration, count, volume |
| FTRC-S034 | [Mobile] Live timer on creation screen |
| FTRC-S035 | [Mobile] Rest timer: 60/90/120s countdown |

### Polish (36-40)

| Code | TODO |
|------|------|
| FTRC-S036 | [Mobile] 'Add Custom Exercise' form |
| FTRC-S037 | [API] POST /exercises (name, category, desc) |
| FTRC-S038 | [Mobile] Weekly summary header on history |
| FTRC-S039 | [Mobile] Personal Records section in stats |
| FTRC-S040 | [Mobile] Final polish: tab icons, nav, styling |

## CS TODOs (30)


### Threat Modeling (C001-C006)

| Code | TODO |
|------|------|
| FTRC-C001 | Architecture diagram + data flows + trust boundaries |
| FTRC-C002 | Endpoint inventory (method, inputs, auth, sensitivity) |
| FTRC-C003 | STRIDE analysis on main flow |
| FTRC-C004 | Attack surface mapping (all untrusted inputs) |
| FTRC-C005 | Attack tree (3+ paths, AND/OR nodes) |
| FTRC-C006 | 1-page threat assessment (top 5 risks) |

### Code Review / Bug Bounty (C007-C016)

| Code | TODO |
|------|------|
| FTRC-C007 | Find SQL Injection (CWE-89) |
| FTRC-C008 | Find Stored XSS (CWE-79) |
| FTRC-C009 | Find IDOR (CWE-639) |
| FTRC-C010 | Find hardcoded secrets (CWE-798) |
| FTRC-C011 | Find weak password hashing (CWE-328) |
| FTRC-C012 | Audit missing authentication (CWE-306) |
| FTRC-C013 | Analyze CORS configuration |
| FTRC-C014 | Find debug mode exposure (CWE-489) |
| FTRC-C015 | Review error handling (CWE-209) |
| FTRC-C016 | Audit SE teammates' code (2+ bug reports) |

### Security Testing (C017-C024)

| Code | TODO |
|------|------|
| FTRC-C017 | SQLi exploit script |
| FTRC-C018 | XSS exploit (3 payloads) |
| FTRC-C019 | IDOR enumeration script |
| FTRC-C020 | Rate limit / DoS test |
| FTRC-C021 | Automated scan (nmap + Python) |
| FTRC-C022 | API fuzzer |
| FTRC-C023 | Project-specific exploit (Werkzeug/pickle/CSRF/etc.) |
| FTRC-C024 | test_security.py (8+ pytest cases) |

### Deployment & Hardening (C025-C030)

| Code | TODO |
|------|------|
| FTRC-C025 | Fix SQL Injection (parameterized queries) |
| FTRC-C026 | Add authentication + fix IDOR |
| FTRC-C027 | Secure configuration (debug, secrets, passwords) |
| FTRC-C028 | Security headers + rate limiting |
| FTRC-C029 | Structured logging + alerting |
| FTRC-C030 | Final 5-page audit report |


# Collaborators
1. Janice Ngugi
2. Vaishnavi Putta
3. Iliyaz Mohammed

