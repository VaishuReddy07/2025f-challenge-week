package com.epita.fitnesstracker.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Workout {
    private int id;
    private String date;
    private int durationMin;
    private String notes;
    private int exerciseCount;
    private double totalVolume;

    public Workout() {}

    public Workout(int id, String date, int durationMin, String notes) {
        this.id = id;
        this.date = date;
        this.durationMin = durationMin;
        this.notes = notes;
    }

    public static Workout fromJson(JSONObject json) throws JSONException {
        Workout w = new Workout();
        w.id = json.getInt("id");
        w.date = json.getString("date");
        w.durationMin = json.optInt("duration_min", 0);
        w.notes = json.optString("notes", "");
        
        // Based on backend snippet: "total_exercises" and "total_volume"
        w.exerciseCount = json.optInt("total_exercises", 
                         json.optInt("exercise_count", 
                         json.optInt("count", 0)));
                         
        w.totalVolume = json.optDouble("total_volume", 
                        json.optDouble("volume", 0.0));

        return w;
    }

    public int getId() { return id; }
    public String getDate() { return date; }
    public int getDurationMin() { return durationMin; }
    public String getNotes() { return notes; }
    public int getExerciseCount() { return exerciseCount; }
    public double getTotalVolume() { return totalVolume; }
}
