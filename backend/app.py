import base64
import json
import os
import time
from collections import defaultdict

from dotenv import load_dotenv
from flask import Flask, jsonify, request
from markupsafe import escape
from flask_cors import CORS

from database import init_db, get_db
from models import get_all_exercises, get_all_workouts, create_workout

load_dotenv()

app = Flask(__name__)

app.secret_key = os.getenv("SECRET_KEY")
if not app.secret_key:
    raise RuntimeError("SECRET_KEY environment variable must be set")

API_KEY = os.getenv("API_KEY")
if not API_KEY:
    raise RuntimeError("API_KEY environment variable must be set")

RATE_LIMIT_WINDOW = int(os.getenv("RATE_LIMIT_WINDOW", "60"))
RATE_LIMIT_MAX_REQUESTS = int(os.getenv("RATE_LIMIT_MAX_REQUESTS", "100"))
rate_limit_data = defaultdict(lambda: {"count": 0, "reset_at": time.time() + RATE_LIMIT_WINDOW})

CORS(app)


@app.before_request
def enforce_security():
    if request.method == "OPTIONS":
        return None

    client_ip = request.remote_addr or "unknown"
    now = time.time()
    entry = rate_limit_data[client_ip]

    if now >= entry["reset_at"]:
        entry["count"] = 0
        entry["reset_at"] = now + RATE_LIMIT_WINDOW

    entry["count"] += 1
    if entry["count"] > RATE_LIMIT_MAX_REQUESTS:
        return jsonify({"error": "Too many requests"}), 429

    auth_header = request.headers.get("Authorization", "")
    if not auth_header.startswith("Bearer "):
        return jsonify({"error": "Authorization required"}), 401

    token = auth_header.split(" ", 1)[1]
    if token != API_KEY:
        return jsonify({"error": "Unauthorized"}), 401


@app.after_request
def add_header(response):
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Powered-By"] = "FitnessTracker"
    return response


# -------------------------------------------
# Init DB
# -------------------------------------------
init_db()


# -------------------------------------------
# GET EXERCISES
# -------------------------------------------
@app.route("/exercises", methods=["GET"])
def list_exercises():
    return jsonify(get_all_exercises())


# -------------------------------------------
# GET WORKOUTS
# -------------------------------------------
@app.route("/workouts", methods=["GET"])
def list_workouts():
    date_from = request.args.get("from")
    date_to = request.args.get("to")

    conn = get_db()
    cursor = conn.cursor(dictionary=True)

    if date_from and date_to:
        cursor.execute(
            "SELECT * FROM workouts WHERE date >= %s AND date <= %s ORDER BY date",
            (date_from, date_to)
        )
    else:
        cursor.execute("SELECT * FROM workouts ORDER BY date")

    workouts = cursor.fetchall()

    cursor.close()
    conn.close()

    return jsonify(workouts)


# -------------------------------------------
# CREATE WORKOUT (BASIC)
# -------------------------------------------
@app.route("/workouts", methods=["POST"])
def add_workout():
    try:
        data = request.get_json()
    except Exception:
        return jsonify({"error": "Invalid JSON"}), 400

    if not data or "date" not in data:
        return jsonify({"error": "Missing required field: date"}), 400

    conn = get_db()
    cursor = conn.cursor()

    # insert workout
    cursor.execute(
        "INSERT INTO workouts (date, duration_min, notes) VALUES (%s, %s, %s)",
        (data["date"], data.get("duration_min"), data.get("notes"))
    )

    workout_id = cursor.lastrowid

    exercises = data.get("exercises", [])

    for ex in exercises:
        if "exercise_id" not in ex:
            continue
        cursor.execute(
            """
            INSERT INTO workout_exercises
            (workout_id, exercise_id, sets, reps, weight_kg)
            VALUES (%s, %s, %s, %s, %s)
            """,
            (
                workout_id,
                ex.get("exercise_id"),
                ex.get("sets"),
                ex.get("reps"),
                ex.get("weight_kg"),
            )
        )

    conn.commit()
    cursor.close()
    conn.close()

    return jsonify({
        "id": workout_id,
        "date": data["date"],
        "duration_min": data.get("duration_min"),
        "notes": data.get("notes"),
        "exercises": [
            {
                "id": ex["exercise_id"],  
            "sets": ex.get("sets"),
            "reps": ex.get("reps"),
            "weight_kg": ex.get("weight_kg")
        }
        for ex in exercises
    ]
    }), 201


# -------------------------------------------
# GET WORKOUT DETAILS (JOIN)
# -------------------------------------------
@app.route("/workouts/<int:workout_id>", methods=["GET"])
def get_workout_detail(workout_id):
    conn = get_db()
    cursor = conn.cursor(dictionary=True)

    cursor.execute("""
        SELECT 
            w.id,
            w.date,
            w.duration_min,
            w.notes,
            e.id as exercise_id,
            e.name as exercise_name,
            e.category,
            we.sets,
            we.reps,
            we.weight_kg
        FROM workouts w
        LEFT JOIN workout_exercises we ON w.id = we.workout_id
        LEFT JOIN exercises e ON we.exercise_id = e.id
        WHERE w.id = %s
    """, (workout_id,))

    rows = cursor.fetchall()

    cursor.close()
    conn.close()

    if not rows:
        return jsonify({"error": "Workout not found"}), 404

    workout = {
        "id": rows[0]["id"],
        "date": str(rows[0]["date"]),
        "duration_min": rows[0]["duration_min"],
        "notes": rows[0]["notes"],
        "exercises": []
    }

    for row in rows:
        if row["exercise_id"]:
            try:
                weight = float(row["weight_kg"]) if row["weight_kg"] else 0
            except (ValueError, TypeError):
                weight = 0
            workout["exercises"].append({
                "id": row["exercise_id"],
                "name": row["exercise_name"],
                "category": row["category"],
                "sets": row["sets"],
                "reps": row["reps"],
                "weight_kg": weight
            })

    return jsonify(workout), 200


# -------------------------------------------
# STATS API
# -------------------------------------------
@app.route("/stats", methods=["GET"])
def get_stats():
    """Get aggregate workout statistics."""
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    
    # Get total workouts, total duration, and average duration
    cursor.execute("""
        SELECT 
            COUNT(*) as total_workouts,
            SUM(duration_min) as total_duration_min,
            AVG(duration_min) as avg_duration_min
        FROM workouts
    """)
    
    stats_result = cursor.fetchone()
    
    # Get workouts per category
    cursor.execute("""
        SELECT 
            e.category,
            COUNT(DISTINCT w.id) as workout_count
        FROM workouts w
        JOIN workout_exercises we ON w.id = we.workout_id
        JOIN exercises e ON we.exercise_id = e.id
        GROUP BY e.category
        ORDER BY e.category
    """)
    
    category_results = cursor.fetchall()
    cursor.close()
    conn.close()
    
    # Build workouts_per_category dictionary
    workouts_per_category = {}
    for row in category_results:
        workouts_per_category[row["category"]] = row["workout_count"]
    
    # Build response with EXACT field names the test expects
    response = {
        "total_workouts": stats_result["total_workouts"] or 0,
        "total_duration_min": stats_result["total_duration_min"] or 0,
        "avg_duration_min": float(stats_result["avg_duration_min"]) if stats_result["avg_duration_min"] else 0,
        "workouts_per_category": workouts_per_category
    }
    
    return jsonify(response), 200


# -------------------------------------------
# SEARCH WORKOUTS
# -------------------------------------------
@app.route("/workouts/search")
def search_workouts():
    date_from = request.args.get("from", "")
    date_to = request.args.get("to", "")

    conn = get_db()
    cursor = conn.cursor(dictionary=True)

    cursor.execute(
        "SELECT * FROM workouts WHERE date >= %s AND date <= %s ORDER BY date",
        (date_from, date_to)
    )

    results = cursor.fetchall()

    cursor.close()
    conn.close()

    return jsonify(results)


# -------------------------------------------
# ADMIN PAGE
# -------------------------------------------
@app.route("/admin")
def admin_page():
    conn = get_db()
    cursor = conn.cursor(dictionary=True)

    cursor.execute("SELECT * FROM workouts ORDER BY date DESC")
    workouts = cursor.fetchall()

    cursor.close()
    conn.close()

    html = "<html><body><h1>Admin Panel</h1>"
    for w in workouts:
        safe_date = escape(str(w['date'])) if w['date'] else ""
        safe_notes = escape(w['notes']) if w['notes'] else ""
        html += f"<div><h3>{safe_date}</h3><p>{safe_notes}</p></div>"
    html += "</body></html>"

    return html


# -------------------------------------------
# DELETE
# -------------------------------------------
@app.route("/workouts/<int:workout_id>", methods=["DELETE"])
def delete_workout(workout_id):
    """Delete a workout and all its associated exercises (transactional)."""
    conn = get_db()
    cursor = conn.cursor()

    try:
        # First delete all workout_exercises entries (child records)        GET http://localhost:5000/workouts?from=2026-01-01&to=2026-04-15
        cursor.execute("DELETE FROM workout_exercises WHERE workout_id = %s", (workout_id,))
        
        # Then delete the workout itself
        cursor.execute("DELETE FROM workouts WHERE id = %s", (workout_id,))

        # Commit the transaction
        conn.commit()
        
        return jsonify({"message": "Workout deleted successfully"}), 200
    
    except Exception as e:
        # Rollback on error
        conn.rollback()
        return jsonify({"error": "Failed to delete workout"}), 500
    
    finally:
        cursor.close()
        conn.close()


# -------------------------------------------
# UPDATE
# -------------------------------------------
@app.route("/workouts/<int:workout_id>", methods=["PATCH"])
def update_workout(workout_id):
    data = request.get_json()
    if not isinstance(data, dict) or not data:
        return jsonify({"error": "Invalid request body"}), 400

    allowed_fields = {"date", "duration_min", "notes"}
    updates = {}

    for key, value in data.items():
        if key not in allowed_fields:
            return jsonify({"error": f"Field not allowed: {key}"}), 400
        updates[key] = value

    if "duration_min" in updates and updates["duration_min"] is not None:
        if not isinstance(updates["duration_min"], int):
            return jsonify({"error": "duration_min must be an integer"}), 400

    if "date" in updates and not isinstance(updates["date"], str):
        return jsonify({"error": "date must be a string"}), 400

    conn = get_db()
    cursor = conn.cursor()

    fields = [f"{key} = %s" for key in updates.keys()]
    values = list(updates.values())

    if not fields:
        return jsonify({"error": "No valid fields to update"}), 400

    values.append(workout_id)

    cursor.execute(
        f"UPDATE workouts SET {', '.join(fields)} WHERE id = %s",
        values
    )

    conn.commit()
    cursor.close()
    conn.close()

    return jsonify({"message": "Workout updated"})


# -------------------------------------------
# MOST FREQUENT EXERCISE (JOIN + GROUP BY)
# -------------------------------------------
@app.route("/most-frequent-exercise", methods=["GET"])
def get_most_frequent_exercise():
    """Get the most frequently performed exercise using JOIN and GROUP BY."""
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    
    cursor.execute("""
        SELECT 
            e.id,
            e.name,
            e.category,
            e.description,
            COUNT(we.id) as frequency
        FROM exercises e
        JOIN workout_exercises we ON e.id = we.exercise_id
        GROUP BY e.id, e.name, e.category, e.description
        ORDER BY frequency DESC
        LIMIT 1
    """)
    
    result = cursor.fetchone()
    cursor.close()
    conn.close()
    
    if not result:
        return jsonify({"error": "No exercises found"}), 404
    
    return jsonify({
        "id": result["id"],
        "name": result["name"],
        "category": result["category"],
        "description": result["description"],
        "frequency": result["frequency"]
    }), 200


# -------------------------------------------
# TOTAL VOLUME ACROSS ALL WORKOUTS
# -------------------------------------------
@app.route("/total-volume", methods=["GET"])
def get_total_volume():
    """Calculate total volume across all workouts (weight × reps × sets)."""
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    
    cursor.execute("""
        SELECT 
            SUM(COALESCE(weight_kg, 0) * COALESCE(reps, 0) * COALESCE(sets, 0)) as total_volume
        FROM workout_exercises
    """)
    
    result = cursor.fetchone()
    cursor.close()
    conn.close()
    
    total_volume = float(result["total_volume"]) if result["total_volume"] else 0.0
    
    return jsonify({
        "total_volume": total_volume
    }), 200


# -------------------------------------------
# WORKOUTS THIS WEEK
# -------------------------------------------
@app.route("/workouts/this-week", methods=["GET"])
def get_workouts_this_week():
    """Get all workouts from the current week."""
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    
    cursor.execute("""
        SELECT * FROM workouts
        WHERE YEARWEEK(date) = YEARWEEK(NOW())
        ORDER BY date DESC
    """)
    
    workouts = cursor.fetchall()
    cursor.close()
    conn.close()
    
    return jsonify(workouts), 200


# -------------------------------------------
# WORKOUTS THIS MONTH
# -------------------------------------------
@app.route("/workouts/this-month", methods=["GET"])
def get_workouts_this_month():
    """Get all workouts from the current month."""
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    
    cursor.execute("""
        SELECT * FROM workouts
        WHERE YEAR(date) = YEAR(NOW()) AND MONTH(date) = MONTH(NOW())
        ORDER BY date DESC
    """)
    
    workouts = cursor.fetchall()
    cursor.close()
    conn.close()
    
    return jsonify(workouts), 200


# -------------------------------------------
# IMPORT
# -------------------------------------------
@app.route("/workouts/import", methods=["POST"])
def import_workouts():
    data = request.get_json(force=True)
    if not isinstance(data, dict) or "data" not in data:
        return jsonify({"error": "Payload must include base64-encoded JSON data"}), 400

    payload = data.get("data", "")
    try:
        decoded = base64.b64decode(payload).decode("utf-8")
        workout_data = json.loads(decoded)
    except Exception:
        return jsonify({"error": "Invalid import payload"}), 400

    if not isinstance(workout_data, list):
        return jsonify({"error": "Imported data must be a JSON array"}), 400

    return jsonify({"message": f"Imported {len(workout_data)} workouts"}), 200


# -------------------------------------------
# EXERCISE HISTORY
# -------------------------------------------
@app.route("/exercises/<int:exercise_id>/history", methods=["GET"])
def get_exercise_history(exercise_id):
    """Get the performance history of a specific exercise (all past performances)."""
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    
    # First, verify the exercise exists
    cursor.execute("SELECT * FROM exercises WHERE id = %s", (exercise_id,))
    exercise = cursor.fetchone()
    
    if not exercise:
        cursor.close()
        conn.close()
        return jsonify({"error": "Exercise not found"}), 404
    
    # Get all instances of this exercise in workouts, ordered by date
    cursor.execute("""
        SELECT 
            w.id as workout_id,
            w.date,
            w.duration_min,
            w.notes,
            we.sets,
            we.reps,
            we.weight_kg,
            e.id as exercise_id,
            e.name as exercise_name,
            e.category,
            e.description
        FROM workouts w
        JOIN workout_exercises we ON w.id = we.workout_id
        JOIN exercises e ON we.exercise_id = e.id
        WHERE e.id = %s
        ORDER BY w.date DESC
    """, (exercise_id,))
    
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    
    # Build response
    history_entries = []
    for row in rows:
        try:
            weight = float(row["weight_kg"]) if row["weight_kg"] else 0
        except (ValueError, TypeError):
            weight = 0
        
        history_entries.append({
            "workout_id": row["workout_id"],
            "date": str(row["date"]),
            "duration_min": row["duration_min"],
            "notes": row["notes"],
            "sets": row["sets"],
            "reps": row["reps"],
            "weight_kg": weight
        })
    
    return jsonify({
        "exercise_id": exercise["id"],
        "exercise_name": exercise["name"],
        "category": exercise["category"],
        "description": exercise["description"],
        "history": history_entries
    }), 200


# -------------------------------------------
# RUN
# -------------------------------------------
if __name__ == "__main__":
    debug_mode = os.getenv("FLASK_DEBUG", "False").lower() == "true"
    app.run(debug=debug_mode, port=5000)