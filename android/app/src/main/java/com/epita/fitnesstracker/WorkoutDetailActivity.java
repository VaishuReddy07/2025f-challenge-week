package com.epita.fitnesstracker;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.epita.fitnesstracker.adapter.WorkoutExerciseAdapter;
import com.epita.fitnesstracker.api.ApiClient;
import com.epita.fitnesstracker.model.WorkoutExercise;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WorkoutDetailActivity extends AppCompatActivity {

    private TextView tvDate, tvDuration, tvVolume, tvNotes;
    private WorkoutExerciseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        tvDate = findViewById(R.id.tvDetailDate);
        tvDuration = findViewById(R.id.tvDetailDuration);
        tvVolume = findViewById(R.id.tvDetailVolume);
        tvNotes = findViewById(R.id.tvDetailNotes);

        RecyclerView rv = findViewById(R.id.rvWorkoutExercises);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WorkoutExerciseAdapter();
        rv.setAdapter(adapter);

        int workoutId = getIntent().getIntExtra("WORKOUT_ID", -1);
        if (workoutId != -1) {
            fetchWorkoutDetail(workoutId);
        } else {
            Toast.makeText(this, "Invalid workout ID", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchWorkoutDetail(int id) {
        ApiClient.get("/workouts/" + id, new ApiClient.Callback() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JSONObject obj = new JSONObject(responseBody);
                    String date = obj.getString("date");
                    int duration = obj.getInt("duration_min");
                    String notes = obj.optString("notes", "");

                    JSONArray exercisesArr = obj.getJSONArray("exercises");
                    List<WorkoutExercise> exercises = new ArrayList<>();
                    double totalVolume = 0;

                    for (int i = 0; i < exercisesArr.length(); i++) {
                        WorkoutExercise we = WorkoutExercise.fromJson(exercisesArr.getJSONObject(i));
                        exercises.add(we);
                        totalVolume += we.getVolume();
                    }

                    final double finalVolume = totalVolume;
                    runOnUiThread(() -> {
                        tvDate.setText(date);
                        tvDuration.setText(String.format(Locale.getDefault(), "%d min", duration));
                        tvNotes.setText(notes);
                        tvVolume.setText(String.format(Locale.getDefault(), "%.1f kg", finalVolume));
                        adapter.setExercises(exercises);
                    });

                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(WorkoutDetailActivity.this,
                                    "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(WorkoutDetailActivity.this,
                                "Network error", Toast.LENGTH_SHORT).show());
            }
        });
    }
}
