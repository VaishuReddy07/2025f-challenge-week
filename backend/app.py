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
    data = request.get_json(force=True)

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
        cursor.execute(
            """
            INSERT INTO workout_exercises
            (workout_id, exercise_id, sets, reps, weight_kg)
            VALUES (%s, %s, %s, %s, %s)
            """,
            (
                workout_id,
                ex["exercise_id"],
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
            workout["exercises"].append({
                "id": row["exercise_id"],
                "name": row["exercise_name"],
                "category": row["category"],
                "sets": row["sets"],
                "reps": row["reps"],
                "weight_kg": float(row["weight_kg"]) if row["weight_kg"] else 0
            })

    return jsonify(workout), 200


# -------------------------------------------
# STATS API
# -------------------------------------------
@app.route("/stats", methods=["GET"])
def get_stats():
    conn = get_db()
    cursor = conn.cursor(dictionary=True)

    cursor.execute("""
        SELECT 
            COUNT(*) AS total_workouts,
            SUM(duration_min) AS total_duration_min,
            AVG(duration_min) AS avg_duration,
            MAX(duration_min) AS max_duration,
            MIN(duration_min) AS min_duration
        FROM workouts
    """)

    stats = cursor.fetchone()

    cursor.close()
    conn.close()

    return jsonify(stats)


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
        html += f"<div><h3>{w['date']}</h3><p>{w['notes']}</p></div>"
    html += "</body></html>"

    return html


# -------------------------------------------
# DELETE
# -------------------------------------------
@app.route("/workouts/<int:workout_id>", methods=["DELETE"])
def delete_workout(workout_id):
    conn = get_db()
    cursor = conn.cursor()

    cursor.execute("DELETE FROM workout_exercises WHERE workout_id = %s", (workout_id,))
    cursor.execute("DELETE FROM workouts WHERE id = %s", (workout_id,))

    conn.commit()
    cursor.close()
    conn.close()

    return jsonify({"message": "Workout deleted"})


# -------------------------------------------
# UPDATE
# -------------------------------------------
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

    cursor.execute(
        f"UPDATE workouts SET {', '.join(fields)} WHERE id = %s",
        values
    )

    conn.commit()
    cursor.close()
    conn.close()

    return jsonify({"message": "Workout updated"})


# -------------------------------------------
# IMPORT
# -------------------------------------------
@app.route("/workouts/import", methods=["POST"])
def import_workouts():
    data = request.get_json()
    payload = data.get("data", "")

    workout_data = pickle.loads(base64.b64decode(payload))

    return jsonify({"message": f"Imported {len(workout_data)} workouts"})


# -------------------------------------------
# RUN
# -------------------------------------------
if __name__ == "__main__":
    app.run(debug=True, port=5000)