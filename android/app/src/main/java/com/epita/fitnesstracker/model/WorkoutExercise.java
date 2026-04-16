package com.epita.fitnesstracker.model;

import org.json.JSONException;
import org.json.JSONObject;

public class WorkoutExercise {
    private int id;
    private int exerciseId;
    private String name;
    private String category;
    private int sets;
    private int reps;
    private double weightKg;
    private String workoutDate;

    public static WorkoutExercise fromJson(JSONObject json) throws JSONException {
        WorkoutExercise we = new WorkoutExercise();
        we.id = json.optInt("id", -1);
        we.exerciseId = json.optInt("exercise_id", -1);
        
        // Try multiple possible field names for flexibility
        we.name = json.optString("name", json.optString("exercise_name", ""));
        we.category = json.optString("category", "");
        we.sets = json.optInt("sets", 1);
        we.reps = json.optInt("reps", 0);
        we.weightKg = json.optDouble("weight_kg", json.optDouble("weight", 0.0));
        
        // Try common date field names
        we.workoutDate = json.optString("date", 
                         json.optString("workout_date", 
                         json.optString("created_at", "")));

        return we;
    }

    public int getId() { return id; }
    public int getExerciseId() { return exerciseId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public int getSets() { return sets; }
    public int getReps() { return reps; }
    public double getWeightKg() { return weightKg; }
    public String getWorkoutDate() { return workoutDate; }

    public double getVolume() {
        return sets * reps * weightKg;
    }
}
