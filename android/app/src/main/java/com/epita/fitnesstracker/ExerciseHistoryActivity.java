package com.epita.fitnesstracker;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

public class ExerciseHistoryActivity extends AppCompatActivity {

    private static final String TAG = "ExerciseHistory";
    private TextView tvName, tvCategory, tvProgress;
    private WorkoutExerciseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_history);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Exercise History");
        }

        tvName = findViewById(R.id.tvExHistoryName);
        tvCategory = findViewById(R.id.tvExHistoryCategory);
        tvProgress = findViewById(R.id.tvProgressText);
        RecyclerView rv = findViewById(R.id.rvExerciseHistory);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WorkoutExerciseAdapter();
        adapter.setUseGrouping(false);
        rv.setAdapter(adapter);

        int exerciseId = getIntent().getIntExtra("EXERCISE_ID", -1);
        String name = getIntent().getStringExtra("EXERCISE_NAME");
        String category = getIntent().getStringExtra("EXERCISE_CATEGORY");

        tvName.setText(name);
        tvCategory.setText(category);

        if (exerciseId != -1) {
            fetchExerciseHistory(exerciseId);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchExerciseHistory(int id) {
        ApiClient.get("/exercises/" + id + "/history", new ApiClient.Callback() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JSONObject root = new JSONObject(responseBody);
                    JSONArray historyArr = root.getJSONArray("history");
                    
                    List<WorkoutExercise> historyList = new ArrayList<>();
                    for (int i = 0; i < historyArr.length(); i++) {
                        historyList.add(WorkoutExercise.fromJson(historyArr.getJSONObject(i)));
                    }
                    
                    runOnUiThread(() -> {
                        adapter.setExercises(historyList);
                        calculateProgress(historyList);
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Parsing error", e);
                    runOnUiThread(() -> Toast.makeText(ExerciseHistoryActivity.this, "Error parsing history", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(ExerciseHistoryActivity.this, "Network error", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void calculateProgress(List<WorkoutExercise> history) {
        if (history.size() < 2) {
            tvProgress.setVisibility(View.GONE);
            return;
        }

        // Assuming history is returned in reverse chronological order (newest first)
        WorkoutExercise newest = history.get(0);
        WorkoutExercise oldest = history.get(history.size() - 1);

        double weightNew = newest.getWeightKg();
        double weightOld = oldest.getWeightKg();

        if (weightNew > 0 && weightOld > 0) {
            String progressText = String.format(Locale.getDefault(), 
                "Progress: %.1f kg → %.1f kg", weightOld, weightNew);
            tvProgress.setText(progressText);
            tvProgress.setVisibility(View.VISIBLE);
            
            // Add a little color based on progress
            if (weightNew > weightOld) {
                tvProgress.setTextColor(0xFF4CAF50); // Green
            } else if (weightNew < weightOld) {
                tvProgress.setTextColor(0xFFF44336); // Red
            }
        }
    }
}
