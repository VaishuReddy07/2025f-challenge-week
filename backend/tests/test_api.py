"""API tests for the Fitness Tracker backend.

Tests marked with '# SHOULD FAIL' exercise endpoints that students must implement.
"""

import json


# -----------------------------------------------------------------------
# Passing tests (endpoints already implemented)
# -----------------------------------------------------------------------

def test_get_exercises(client):
    """GET /exercises returns a list of exercises."""
    resp = client.get("/exercises")
    assert resp.status_code == 200
    data = resp.get_json()
    assert isinstance(data, list)
    assert len(data) == 20
    # Check structure
    first = data[0]
    assert "id" in first
    assert "name" in first
    assert "category" in first


def test_get_workouts(client):
    """GET /workouts returns a list of workouts."""
    resp = client.get("/workouts")
    assert resp.status_code == 200
    data = resp.get_json()
    assert isinstance(data, list)
    assert len(data) == 10
    first = data[0]
    assert "date" in first
    assert "duration_min" in first


def test_create_workout(client):
    """POST /workouts creates a basic workout and returns 201."""
    payload = {"date": "2026-03-28", "duration_min": 45, "notes": "Test workout"}
    resp = client.post("/workouts", data=json.dumps(payload),
                       content_type="application/json")
    assert resp.status_code == 201
    data = resp.get_json()
    assert data["date"] == "2026-03-28"
    assert data["duration_min"] == 45
    assert "id" in data


# -----------------------------------------------------------------------
# Failing tests — students must implement these features
# -----------------------------------------------------------------------

def test_workout_detail(client):
    """GET /workouts/:id should return the workout with its exercises.

    SHOULD FAIL until students implement the detail endpoint with JOINs.
    """
    resp = client.get("/workouts/1")
    assert resp.status_code == 200
    data = resp.get_json()
    assert "exercises" in data
    assert isinstance(data["exercises"], list)
    assert len(data["exercises"]) > 0
    ex = data["exercises"][0]
    assert "name" in ex
    assert "sets" in ex
    assert "reps" in ex


def test_create_workout_with_exercises(client):
    """POST /workouts with an exercises array should create everything in one request.

    SHOULD FAIL until students implement transactional creation.
    """
    payload = {
        "date": "2026-03-28",
        "duration_min": 60,
        "notes": "Full workout creation",
        "exercises": [
            {"exercise_id": 1, "sets": 4, "reps": 8, "weight_kg": 70.0},
            {"exercise_id": 8, "sets": 3, "reps": 10, "weight_kg": 80.0},
        ],
    }
    resp = client.post("/workouts", data=json.dumps(payload),
                       content_type="application/json")
    assert resp.status_code == 201
    data = resp.get_json()
    assert "exercises" in data
    assert len(data["exercises"]) == 2


def test_stats(client):
    """GET /stats should return aggregate statistics.

    SHOULD FAIL until students implement the stats endpoint.
    """
    resp = client.get("/stats")
    assert resp.status_code == 200
    data = resp.get_json()
    assert "total_workouts" in data
    assert "total_duration_min" in data
    assert "avg_duration_min" in data
    assert "workouts_per_category" in data


def test_filter_workouts_by_date(client):
    """GET /workouts?from=...&to=... should filter by date range.

    SHOULD FAIL until students implement date filtering.
    """
    resp = client.get("/workouts?from=2026-03-20&to=2026-03-25")
    assert resp.status_code == 200
    data = resp.get_json()
    assert isinstance(data, list)
    for w in data:
        assert w["date"] >= "2026-03-20"
        assert w["date"] <= "2026-03-25"
    # The seed data has specific workouts in this range
    assert len(data) >= 1


def test_delete_workout(client):
    """DELETE /workouts/:id should remove the workout and its exercises.

    SHOULD FAIL until students implement the delete endpoint with CASCADE.
    """
    resp = client.delete("/workouts/1")
    assert resp.status_code == 200
    # Verify it's gone
    resp2 = client.get("/workouts")
    data = resp2.get_json()
    ids = [w["id"] for w in data]
    assert 1 not in ids
