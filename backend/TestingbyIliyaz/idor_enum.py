import requests

for user_id in range(1, 20):
    url = f"http://localhost:5000/activity?user_id={user_id}"
    r = requests.get(url)
    print(f"User {user_id}: {r.status_code}")