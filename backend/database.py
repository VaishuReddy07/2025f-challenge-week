import os
import mysql.connector

DB_CONFIG = {
    "host": os.environ.get("DB_HOST", "localhost"),
    "port": int(os.environ.get("DB_PORT", "3306")),
    "user": os.environ.get("DB_USER", "root"),
    "password": os.environ.get("DB_PASSWORD", "1234"),
    "database": os.environ.get("DB_NAME", "fitness_tracker"),
}


def get_db(db_name=None):
    """Return a connection to the MySQL database."""
    config = dict(DB_CONFIG)
    if db_name:
        config["database"] = db_name
    return mysql.connector.connect(**config)


def init_db(db_name=None):
    """Create all tables if they do not exist."""
    config_no_db = {k: v for k, v in DB_CONFIG.items() if k != "database"}
    target_db = db_name or DB_CONFIG["database"]
    conn = mysql.connector.connect(**config_no_db)
    cursor = conn.cursor()
    cursor.execute(f"CREATE DATABASE IF NOT EXISTS {target_db}")
    cursor.close()
    conn.close()

    config = dict(DB_CONFIG)
    if db_name:
        config["database"] = db_name
    conn = mysql.connector.connect(**config)
    cursor = conn.cursor()

    cursor.execute("""
        CREATE TABLE IF NOT EXISTS exercises (
            id INT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            category VARCHAR(100) NOT NULL,
            description TEXT
        )
    """)

    cursor.execute("""
        CREATE TABLE IF NOT EXISTS workouts (
            id INT AUTO_INCREMENT PRIMARY KEY,
            date VARCHAR(30) NOT NULL,
            duration_min INT,
            notes TEXT
        )
    """)

    cursor.execute("""
        CREATE TABLE IF NOT EXISTS workout_exercises (
            id INT AUTO_INCREMENT PRIMARY KEY,
            workout_id INT NOT NULL,
            exercise_id INT NOT NULL,
            sets INT,
            reps INT,
            weight_kg DOUBLE,
            FOREIGN KEY (workout_id) REFERENCES workouts(id),
            FOREIGN KEY (exercise_id) REFERENCES exercises(id)
        )
    """)

    conn.commit()
    cursor.close()
    conn.close()
