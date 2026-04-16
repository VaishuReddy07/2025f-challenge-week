import requests

url = "http://localhost:5000/activity"

for i in range(500):
    try:
        r = requests.get(url)
        print(i, r.status_code)
    except:
        print("Error at request", i)