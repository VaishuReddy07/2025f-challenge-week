package com.epita.fitnesstracker.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Exercise {
    private int id;
    private String name;
    private String category;
    private String description;

    public Exercise() {}

    public Exercise(int id, String name, String category, String description) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
    }

    public static Exercise fromJson(JSONObject json) throws JSONException {
        Exercise e = new Exercise();
        e.id = json.getInt("id");
        e.name = json.getString("name");
        e.category = json.getString("category");
        e.description = json.optString("description", "");
        return e;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
}
