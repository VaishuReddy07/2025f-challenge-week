# Fitness Tracker - iOS

## Setup

1. Open the `ios/` folder in Xcode.
2. Create a new iOS App project named **FitnessTracker** and replace the generated files with the ones provided here.
3. Make sure the Flask backend is running on `localhost:5000` before launching the app.

## Architecture

| Folder | Purpose |
|--------|---------|
| `Models/` | Codable structs matching the API JSON |
| `Services/` | `APIClient` — URLSession wrapper for GET and POST requests |
| `Views/` | View controllers for each screen |

## What is implemented

- **ExerciseListViewController** — fetches and displays all exercises grouped by category.
- **WorkoutHistoryViewController** — fetches and displays all workouts (date + duration).
- **WorkoutDetailViewController** — empty placeholder.

## What students must build

See the main project README for the full list of TODOs.
