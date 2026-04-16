## FTRC-C002: Endpoint Inventory

| Endpoint | Method | Inputs | Auth | Sensitivity |
|----------|--------|--------|------|------------|
| /exercises | GET | None | Yes | Low |
| /workouts | GET | from, to | Yes | Medium |
| /workouts | POST | JSON | Yes | High |
| /workouts/<id> | GET | Path | Yes | High |
| /workouts/<id> | PATCH | JSON | Yes | High |
| /workouts/<id> | DELETE | Path | Yes | High |
| /workouts/search | GET | Query | Yes | Medium |
| /workouts/import | POST | Base64 | Yes | High |
| /stats | GET | None | Yes | Medium |
| /admin | GET | None | Yes | High |
