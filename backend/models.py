from database import get_db


# ---------------------------------------------------------------------------
# Exercises
# ---------------------------------------------------------------------------

def get_all_exercises(db_name=None):
    """Return every exercise as a list of dicts."""
    conn = get_db(db_name)
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT * FROM exercises ORDER BY category, name")
    exercises = cursor.fetchall()
    cursor.close()
    conn.close()
    return exercises


def get_exercise_by_id(exercise_id, db_name=None):
    """Return a single exercise by id, or None."""
    conn = get_db(db_name)
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT * FROM exercises WHERE id = %s", (exercise_id,))
    row = cursor.fetchone()
    cursor.close()
    conn.close()
    return row


# ---------------------------------------------------------------------------
# Workouts
# ---------------------------------------------------------------------------

def get_all_workouts(db_name=None):
    """Return every workout as a list of dicts (no exercises attached)."""
    conn = get_db(db_name)
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT * FROM workouts ORDER BY date DESC")
    workouts = cursor.fetchall()
    cursor.close()
    conn.close()
    return workouts


def create_workout(date, duration_min=None, notes=None, db_name=None):
    """Insert a basic workout (no exercises). Return the new workout dict."""
    conn = get_db(db_name)
    cursor = conn.cursor(dictionary=True)
    cursor.execute(
        "INSERT INTO workouts (date, duration_min, notes) VALUES (%s, %s, %s)",
        (date, duration_min, notes),
    )
    conn.commit()
    workout_id = cursor.lastrowid
    cursor.execute("SELECT * FROM workouts WHERE id = %s", (workout_id,))
    workout = cursor.fetchone()
    cursor.close()
    conn.close()
    return workout


# ---------------------------------------------------------------------------
# Workout-Exercises (linking table helpers — students will extend these)
# ---------------------------------------------------------------------------

def get_exercises_for_workout(workout_id, db_name=None):
    """Return exercises attached to a workout via workout_exercises.

    TODO: Students will implement this with a JOIN query.
    """
    return []
