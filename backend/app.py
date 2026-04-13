import base64
import pickle
from flask import Flask, jsonify, request
from flask_cors import CORS

from database import init_db, get_db
from models import get_all_exercises, get_all_workouts, create_workout

app = Flask(__name__)
app.secret_key = "changeme"
SECRET_KEY = "super-secret-key-123"
CORS(app)


@app.after_request
def add_header(response):
    response.headers["X-Powered-By"] = "Flask/2.3.2 Python/3.11"
    return response

# ---------------------------------------------------------------------------
# Initialise the database on startup
# ---------------------------------------------------------------------------
init_db()


# ---------------------------------------------------------------------------
# Routes
# ---------------------------------------------------------------------------

@app.route("/exercises", methods=["GET"])
def list_exercises():
    """Return all exercises."""
    exercises = get_all_exercises()
    return jsonify(exercises)


@app.route("/workouts", methods=["GET"])
def list_workouts():
    """Return all workouts (date + duration, no exercises attached)."""
    workouts = get_all_workouts()
    return jsonify(workouts)


@app.route("/workouts", methods=["POST"])
def add_workout():
    """Create a basic workout.

    Expected JSON body:
        { "date": "2026-03-28", "duration_min": 45, "notes": "Leg day" }
    """
    data = request.get_json(force=True)
    if not data or "date" not in data:
        return jsonify({"error": "Missing required field: date"}), 400

    workout = create_workout(
        date=data["date"],
        duration_min=data.get("duration_min"),
        notes=data.get("notes"),
    )
    return jsonify(workout), 201


@app.route("/workouts/<int:workout_id>", methods=["GET"])
def get_workout_detail(workout_id):
    """Get a specific workout with all its exercises.
    
    Returns workout details plus all exercises with sets, reps, weight.
    """
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    
    # Query to get workout with all its exercises using JOIN
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
    
    # Build the response
    workout = {
        "id": rows[0]["id"],
        "date": str(rows[0]["date"]),  # Convert date to string
        "duration_min": rows[0]["duration_min"],
        "notes": rows[0]["notes"],
        "exercises": []
    }
    
    # Add exercises (skip if no exercises exist for this workout)
    for row in rows:
        if row["exercise_id"]:  
            workout["exercises"].append({
                "id": row["exercise_id"],
                "name": row["exercise_name"],
                "category": row["category"],
                "sets": row["sets"],
                "reps": row["reps"],
                "weight_kg": float(row["weight_kg"]) if row["weight_kg"] else 0
            })
    
    return jsonify(workout), 200
# ---------------------------------------------------------------------------
# TODO (students will implement these endpoints):
#   GET  /workouts/<id>          — workout detail with exercises (JOIN)
#   POST /workouts (extended)    — create workout WITH exercises in one request
#   GET  /stats                  — aggregate statistics (GROUP BY, COUNT, AVG)
#   GET  /workouts?from=&to=     — filter workouts by date range
#   DELETE /workouts/<id>        — delete a workout (CASCADE)
# ---------------------------------------------------------------------------


@app.route("/workouts/search")
def search_workouts():
    date_from = request.args.get("from", "")
    date_to = request.args.get("to", "")
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    cursor.execute(f"SELECT * FROM workouts WHERE date >= '{date_from}' AND date <= '{date_to}' ORDER BY date")
    results = cursor.fetchall()
    cursor.close()
    conn.close()
    return jsonify(results)


@app.route("/admin")
def admin_page():
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT * FROM workouts ORDER BY date DESC")
    workouts = cursor.fetchall()
    cursor.close()
    conn.close()
    html = "<html><head><title>Admin - Workouts</title></head><body>"
    html += "<h1>Fitness Tracker Admin Panel</h1>"
    for w in workouts:
        html += f"<div class='workout'><h3>{w['date']}</h3><p>{w['notes']}</p></div>"
    html += "</body></html>"
    return html


@app.route("/workouts/<int:workout_id>", methods=["DELETE"])
def delete_workout(workout_id):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("DELETE FROM workout_exercises WHERE workout_id = %s", (workout_id,))
    cursor.execute("DELETE FROM workouts WHERE id = %s", (workout_id,))
    conn.commit()
    cursor.close()
    conn.close()
    return jsonify({"message": "Workout deleted"}), 200


@app.route("/workouts/<int:workout_id>", methods=["PATCH"])
def update_workout(workout_id):
    data = request.get_json()
    conn = get_db()
    cursor = conn.cursor()
    fields = []
    values = []
    for key, value in data.items():
        fields.append(f"{key} = %s")
        values.append(value)
    values.append(workout_id)
    cursor.execute(f"UPDATE workouts SET {', '.join(fields)} WHERE id = %s", values)
    conn.commit()
    cursor.close()
    conn.close()
    return jsonify({"message": "Workout updated"}), 200


@app.route("/workouts/import", methods=["POST"])
def import_workouts():
    data = request.get_json()
    payload = data.get("data", "")
    workout_data = pickle.loads(base64.b64decode(payload))
    return jsonify({"message": f"Imported {len(workout_data)} workouts"}), 200


if __name__ == "__main__":
    app.run(debug=True, port=5000)
