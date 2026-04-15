"""Quick test of FTRC-S017 endpoints."""

import json
from app import app

with app.test_client() as client:
    # Test /workouts/this-week
    resp_week = client.get('/workouts/this-week')
    print(f'GET /workouts/this-week: {resp_week.status_code}')
    data_week = resp_week.get_json()
    print(f'Response: {data_week}')
    print(f'Is list: {isinstance(data_week, list)}')
    
    # Test /workouts/this-month
    print('\n---')
    resp_month = client.get('/workouts/this-month')
    print(f'GET /workouts/this-month: {resp_month.status_code}')
    data_month = resp_month.get_json()
    print(f'Response: {data_month}')
    print(f'Is list: {isinstance(data_month, list)}')
    
    # Test /most-frequent-exercise
    print('\n---')
    resp_freq = client.get('/most-frequent-exercise')
    print(f'GET /most-frequent-exercise: {resp_freq.status_code}')
    data_freq = resp_freq.get_json()
    print(f'Response: {data_freq}')
    if resp_freq.status_code == 200:
        print(f'Has frequency field: {"frequency" in data_freq}')
