"""Quick test of FTRC-S018 endpoint."""

from app import app

with app.test_client() as client:
    # Test /total-volume
    resp_vol = client.get('/total-volume')
    print(f'GET /total-volume: {resp_vol.status_code}')
    data_vol = resp_vol.get_json()
    print(f'Response: {data_vol}')
    print(f'Has total_volume field: {"total_volume" in data_vol}')
    
    if resp_vol.status_code == 200 and "total_volume" in data_vol:
        total = data_vol["total_volume"]
        print(f'Total volume value: {total}')
        print(f'Is number: {isinstance(total, (int, float))}')
