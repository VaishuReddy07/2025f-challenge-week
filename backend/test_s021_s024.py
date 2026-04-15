"""Test FTRC-S021 and FTRC-S024 endpoints"""

from app import app

with app.test_client() as client:
    # Test FTRC-S021: GET /workouts with from/to date params
    print("=== FTRC-S021: GET /workouts with date filtering ===")
    
    # Test with date range
    resp = client.get("/workouts?from=2026-03-20&to=2026-03-25")
    print(f"GET /workouts?from=2026-03-20&to=2026-03-25: {resp.status_code}")
    data = resp.get_json()
    print(f"Results: {len(data)} workouts found")
    for w in data:
        print(f"  - ID: {w['id']}, Date: {w['date']}, Duration: {w['duration_min']} min")
    
    # Verify all results are within date range
    all_in_range = all(w['date'] >= "2026-03-20" and w['date'] <= "2026-03-25" for w in data)
    print(f"All results within date range: {all_in_range}")
    
    # Test without parameters
    print("\nGET /workouts (no filtering)")
    resp_all = client.get("/workouts")
    data_all = resp_all.get_json()
    print(f"Total workouts: {len(data_all)}")
    
    # Test FTRC-S024: DELETE /workouts/:id with transaction
    print("\n=== FTRC-S024: DELETE /workouts/:id (transaction) ===")
    
    # Get initial count
    resp_before = client.get("/workouts")
    data_before = resp_before.get_json()
    initial_count = len(data_before)
    print(f"Workouts before delete: {initial_count}")
    
    # Delete workout
    resp_delete = client.delete("/workouts/1")
    print(f"DELETE /workouts/1: {resp_delete.status_code}")
    print(f"Response: {resp_delete.get_json()}")
    
    # Verify deletion
    resp_after = client.get("/workouts")
    data_after = resp_after.get_json()
    final_count = len(data_after)
    print(f"Workouts after delete: {final_count}")
    print(f"Deleted successfully: {final_count == initial_count - 1}")
    
    # Verify workout 1 is gone
    ids_after = [w["id"] for w in data_after]
    print(f"Workout ID 1 still exists: {1 in ids_after}")
