import requests

BASE_URL = "http://127.0.0.1:5000"

headers = {
    "Authorization": "Bearer test"
}

def test_home_requires_auth():
    r = requests.get(BASE_URL + "/")
    assert r.status_code == 401

def test_profile_requires_auth():
    r = requests.get(BASE_URL + "/profile")
    assert r.status_code == 401

def test_invalid_login():
    data = {"email": "test@test.com", "password": "wrong"}
    r = requests.post(BASE_URL + "/login", json=data)
    assert r.status_code in [400, 401]

def test_activity_requires_auth():
    r = requests.get(BASE_URL + "/activity")
    assert r.status_code == 401

def test_xss_payload_blocked():
    payload = {"notes": "<script>alert('XSS')</script>"}
    r = requests.post(BASE_URL + "/workouts/this-week", json=payload, headers=headers)
    assert r.status_code in [400, 401]

def test_sql_injection_attempt():
    payload = {"email": "' OR 1=1 --", "password": "test"}
    r = requests.post(BASE_URL + "/login", json=payload)
    assert r.status_code in [400, 401]

def test_invalid_endpoint():
    r = requests.get(BASE_URL + "/invalid")
    assert r.status_code == 404

def test_large_input():
    payload = {"notes": "A" * 10000}
    r = requests.post(BASE_URL + "/workouts/this-week", json=payload, headers=headers)
    assert r.status_code in [400, 401]