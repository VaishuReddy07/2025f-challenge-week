import sqlite3

DB_NAME = "fitness.db"

def get_db():
    conn = sqlite3.connect(DB_NAME)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    conn = get_db()
    cursor = conn.cursor()

    # Exercises table
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS exercises (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            category TEXT NOT NULL,
            description TEXT
        )
    """)

    # Workouts table
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS workouts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            date TEXT NOT NULL,
            duration_min INTEGER,
            notes TEXT
        )
    """)

    # Workout exercises table
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS workout_exercises (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            workout_id INTEGER,
            exercise_id INTEGER,
            sets INTEGER,
            reps INTEGER,
            weight_kg REAL,
            FOREIGN KEY (workout_id) REFERENCES workouts(id),
            FOREIGN KEY (exercise_id) REFERENCES exercises(id)
        )
    """)

    conn.commit()
    conn.close()