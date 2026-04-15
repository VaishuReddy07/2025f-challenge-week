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

    public static WorkoutExercise fromJson(JSONObject json) throws JSONException {
        WorkoutExercise we = new WorkoutExercise();
        we.id = json.optInt("id");
        we.exerciseId = json.optInt("exercise_id");
        we.name = json.getString("name");
        we.category = json.getString("category");
        we.sets = json.getInt("sets");
        we.reps = json.getInt("reps");
        we.weightKg = json.getDouble("weight_kg");
        return we;
    }

    public int getId() { return id; }
    public int getExerciseId() { return exerciseId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public int getSets() { return sets; }
    public int getReps() { return reps; }
    public double getWeightKg() { return weightKg; }

    public double getVolume() {
        return sets * reps * weightKg;
    }
}
