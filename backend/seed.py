"""Seed the database with realistic fitness data."""

from database import init_db, get_db

EXERCISES = [
    # Chest
    ("Bench Press", "Chest", "Flat barbell bench press targeting the pectorals"),
    ("Incline Dumbbell Press", "Chest", "Incline press with dumbbells for upper chest"),
    ("Cable Fly", "Chest", "Standing cable crossover for chest isolation"),
    ("Push-Up", "Chest", "Bodyweight push-up with various grip widths"),
    # Back
    ("Deadlift", "Back", "Conventional barbell deadlift for posterior chain"),
    ("Pull-Up", "Back", "Bodyweight pull-up with overhand grip"),
    ("Barbell Row", "Back", "Bent-over barbell row for mid-back thickness"),
    ("Lat Pulldown", "Back", "Cable lat pulldown to the chest"),
    # Legs
    ("Squat", "Legs", "Barbell back squat for quadriceps and glutes"),
    ("Leg Press", "Legs", "Machine leg press at 45 degrees"),
    ("Romanian Deadlift", "Legs", "Stiff-leg deadlift targeting hamstrings"),
    ("Calf Raise", "Legs", "Standing calf raise on a step or machine"),
    # Arms
    ("Barbell Curl", "Arms", "Standing barbell curl for biceps"),
    ("Tricep Dip", "Arms", "Parallel bar dip focusing on triceps"),
    ("Hammer Curl", "Arms", "Dumbbell hammer curl for brachialis"),
    # Cardio
    ("Running", "Cardio", "Outdoor or treadmill running at steady pace"),
    ("Cycling", "Cardio", "Stationary or outdoor cycling"),
    ("Rowing Machine", "Cardio", "Indoor rowing for full-body cardio"),
    # Core
    ("Plank", "Core", "Isometric front plank hold"),
    ("Hanging Leg Raise", "Core", "Hanging from bar, raising legs to parallel"),
]

WORKOUTS = [
    ("2026-03-16", 55, "Upper body push day"),
    ("2026-03-17", 40, "Light cardio and core"),
    ("2026-03-18", 60, "Leg day — heavy squats"),
    ("2026-03-19", 35, "Quick arms session"),
    ("2026-03-20", 50, "Pull day — back focus"),
    ("2026-03-22", 45, "Full body circuit"),
    ("2026-03-23", 30, "Cardio only — running"),
    ("2026-03-24", 65, "Chest and triceps"),
    ("2026-03-25", 50, "Back and biceps"),
    ("2026-03-27", 40, "Active recovery — light cycling and core"),
]

# (workout_index, exercise_index, sets, reps, weight_kg)
WORKOUT_EXERCISES = [
    # Workout 0 — Upper body push
    (0, 0, 4, 8, 70.0),   # Bench Press
    (0, 1, 3, 10, 22.0),  # Incline Dumbbell Press
    (0, 2, 3, 12, 15.0),  # Cable Fly
    (0, 3, 3, 15, None),  # Push-Up (bodyweight)
    # Workout 1 — Light cardio + core
    (1, 15, 1, 1, None),  # Running
    (1, 18, 3, 1, None),  # Plank (hold)
    (1, 19, 3, 12, None), # Hanging Leg Raise
    # Workout 2 — Leg day
    (2, 8, 5, 5, 100.0),  # Squat
    (2, 9, 4, 10, 140.0), # Leg Press
    (2, 10, 3, 10, 60.0), # Romanian Deadlift
    (2, 11, 4, 15, 40.0), # Calf Raise
    # Workout 3 — Arms
    (3, 12, 4, 10, 30.0), # Barbell Curl
    (3, 13, 3, 12, None), # Tricep Dip
    (3, 14, 3, 12, 14.0), # Hammer Curl
    # Workout 4 — Pull day
    (4, 4, 4, 5, 120.0),  # Deadlift
    (4, 5, 4, 8, None),   # Pull-Up
    (4, 6, 4, 8, 60.0),   # Barbell Row
    (4, 7, 3, 10, 50.0),  # Lat Pulldown
    # Workout 5 — Full body circuit
    (5, 0, 3, 10, 60.0),  # Bench Press
    (5, 8, 3, 10, 80.0),  # Squat
    (5, 5, 3, 8, None),   # Pull-Up
    (5, 18, 3, 1, None),  # Plank
    # Workout 6 — Cardio
    (6, 15, 1, 1, None),  # Running
    # Workout 7 — Chest + triceps
    (7, 0, 5, 5, 80.0),   # Bench Press
    (7, 1, 4, 8, 26.0),   # Incline Dumbbell Press
    (7, 2, 3, 12, 17.5),  # Cable Fly
    (7, 13, 4, 10, None),  # Tricep Dip
    # Workout 8 — Back + biceps
    (8, 6, 4, 8, 65.0),   # Barbell Row
    (8, 7, 4, 10, 55.0),  # Lat Pulldown
    (8, 12, 3, 10, 32.0), # Barbell Curl
    (8, 14, 3, 12, 16.0), # Hammer Curl
    # Workout 9 — Active recovery
    (9, 16, 1, 1, None),  # Cycling
    (9, 18, 3, 1, None),  # Plank
    (9, 19, 3, 10, None), # Hanging Leg Raise
]


def seed(db_name=None):
    """Drop and re-create data."""
    init_db(db_name)
    conn = get_db(db_name)
    cur = conn.cursor(dictionary=True)

    # Clear existing data
    cur.execute("DELETE FROM workout_exercises")
    cur.execute("DELETE FROM workouts")
    cur.execute("DELETE FROM exercises")

    # Insert exercises
    for name, category, description in EXERCISES:
        cur.execute(
            "INSERT INTO exercises (name, category, description) VALUES (%s, %s, %s)",
            (name, category, description),
        )

    # Insert workouts
    for date, duration, notes in WORKOUTS:
        cur.execute(
            "INSERT INTO workouts (date, duration_min, notes) VALUES (%s, %s, %s)",
            (date, duration, notes),
        )

    # Retrieve inserted IDs
    cur.execute("SELECT id FROM exercises ORDER BY id")
    exercise_ids = [row["id"] for row in cur.fetchall()]
    cur.execute("SELECT id FROM workouts ORDER BY id")
    workout_ids = [row["id"] for row in cur.fetchall()]

    # Insert workout_exercises
    for wi, ei, sets, reps, weight in WORKOUT_EXERCISES:
        cur.execute(
            "INSERT INTO workout_exercises (workout_id, exercise_id, sets, reps, weight_kg) "
            "VALUES (%s, %s, %s, %s, %s)",
            (workout_ids[wi], exercise_ids[ei], sets, reps, weight),
        )

    conn.commit()
    cur.close()
    conn.close()
    print(f"Seeded {len(EXERCISES)} exercises, {len(WORKOUTS)} workouts, "
          f"{len(WORKOUT_EXERCISES)} workout-exercise links.")


def seed_users():
    import hashlib
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS users (
            id INT AUTO_INCREMENT PRIMARY KEY,
            username VARCHAR(255) NOT NULL,
            auth_token VARCHAR(255) NOT NULL,
            role VARCHAR(50) DEFAULT 'user',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)
    cursor.execute("DELETE FROM users")
    users = [
        ("admin", hashlib.md5(b"admin_token").hexdigest(), "admin"),
        ("coach_mike", hashlib.md5(b"coach_token").hexdigest(), "coach"),
        ("athlete_jane", hashlib.md5(b"jane_token").hexdigest(), "user"),
    ]
    for username, token, role in users:
        cursor.execute(
            "INSERT INTO users (username, auth_token, role) VALUES (%s, %s, %s)",
            (username, token, role),
        )
    conn.commit()
    cursor.close()
    conn.close()
    print(f"Seeded {len(users)} users.")


if __name__ == "__main__":
    seed()
    seed_users()
