import requests

url = "http://127.0.0.1:5000/workouts/this-week"

payload = {
    "notes": "<script>alert('XSS')</script>"
}

headers = { 
    "Authorization": "Bearer test"
}

response = requests.post(url, json=payload, headers=headers)

print("Status:", response.status_code)
print("Response:", response.text)