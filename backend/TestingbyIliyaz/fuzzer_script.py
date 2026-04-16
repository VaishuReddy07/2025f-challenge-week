import requests
import random
import stringpython 

def random_string():
    return ''.join(random.choice(string.ascii_letters) for _ in range(10))

url = "http://localhost:5000/login"

for _ in range(100):
    payload = {
        "email": random_string(),
        "password": random_string()
    }
    r = requests.post(url, json=payload)
    print(r.status_code)