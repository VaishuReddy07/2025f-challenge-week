package com.epita.fitnesstracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.epita.fitnesstracker.adapter.SelectedExerciseAdapter;
import com.epita.fitnesstracker.api.ApiClient;
import com.epita.fitnesstracker.model.Exercise;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CreateWorkoutActivity extends AppCompatActivity {

    private EditText etDate, etTitle, etNotes;
    private Chronometer workoutTimer;
    private SelectedExerciseAdapter adapter;
    private List<Exercise> allExercises = new ArrayList<>();
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_workout);

        etDate = findViewById(R.id.etWorkoutDate);
        etTitle = findViewById(R.id.etWorkoutTitle);
        etNotes = findViewById(R.id.etWorkoutNotes);
        workoutTimer = findViewById(R.id.workoutTimer);
        RecyclerView rv = findViewById(R.id.rvSelectedExercises);

        adapter = new SelectedExerciseAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        etDate.setOnClickListener(v -> showDatePicker());
        findViewById(R.id.btnAddExercise).setOnClickListener(v -> showExercisePicker());
        findViewById(R.id.btnSaveWorkout).setOnClickListener(v -> saveWorkout());

        fetchAllExercises();
        
        // Setup Timer
        startTime = SystemClock.elapsedRealtime();
        workoutTimer.setBase(startTime);
        workoutTimer.start();

        // Default to today's date
        Calendar c = Calendar.getInstance();
        updateDateLabel(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            updateDateLabel(year, month, dayOfMonth);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateLabel(int year, int month, int day) {
        etDate.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day));
    }

    private void fetchAllExercises() {
        ApiClient.get("/exercises", new ApiClient.Callback() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JSONArray arr = new JSONArray(responseBody);
                    allExercises.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        allExercises.add(Exercise.fromJson(arr.getJSONObject(i)));
                    }
                } catch (Exception ignored) {}
            }

            @Override
            public void onError(Exception e) {}
        });
    }

    private void showExercisePicker() {
        if (allExercises.isEmpty()) {
            Toast.makeText(this, "Loading exercises, please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[allExercises.size()];
        for (int i = 0; i < allExercises.size(); i++) {
            names[i] = allExercises.get(i).getName() + " (" + allExercises.get(i).getCategory() + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Exercise")
                .setItems(names, (dialog, which) -> {
                    adapter.addExercise(allExercises.get(which));
                })
                .show();
    }

    private void saveWorkout() {
        String date = etDate.getText().toString();
        String title = etTitle.getText().toString();
        String notes = etNotes.getText().toString();
        
        // Calculate duration in minutes
        long elapsedMillis = SystemClock.elapsedRealtime() - workoutTimer.getBase();
        int durationMin = (int) (elapsedMillis / 60000);
        if (durationMin == 0) durationMin = 1; // Minimum 1 minute

        if (date.isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }

        List<SelectedExerciseAdapter.SelectedExercise> selected = adapter.getSelectedExercises();
        if (selected.isEmpty()) {
            Toast.makeText(this, "Add at least one exercise", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("date", date);
            body.put("duration_min", durationMin);
            // We'll append title to notes if the backend doesn't support title yet
            body.put("notes", title.isEmpty() ? notes : "[" + title + "] " + notes);

            JSONArray exercisesArr = new JSONArray();
            for (SelectedExerciseAdapter.SelectedExercise se : selected) {
                for (SelectedExerciseAdapter.ExerciseSet set : se.sets) {
                    if (set.isDone) {
                        JSONObject exObj = new JSONObject();
                        exObj.put("exercise_id", se.exercise.getId());
                        exObj.put("sets", 1); // We send each set individually or aggregate them
                        exObj.put("reps", set.reps);
                        exObj.put("weight_kg", set.weight);
                        // If backend supports exercise notes, add it here
                        exercisesArr.put(exObj);
                    }
                }
            }
            
            if (exercisesArr.length() == 0) {
                Toast.makeText(this, "Mark at least one set as done", Toast.LENGTH_SHORT).show();
                return;
            }

            body.put("exercises", exercisesArr);

            ApiClient.post("/workouts", body.toString(), new ApiClient.Callback() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() -> {
                        Toast.makeText(CreateWorkoutActivity.this, "Workout saved!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> Toast.makeText(CreateWorkoutActivity.this, "Error saving workout", Toast.LENGTH_SHORT).show());
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
