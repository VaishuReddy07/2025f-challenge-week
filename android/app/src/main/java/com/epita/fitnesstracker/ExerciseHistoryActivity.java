package com.epita.fitnesstracker;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
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

public class ExerciseHistoryActivity extends AppCompatActivity {

    private static final String TAG = "ExerciseHistory";
    private TextView tvName, tvCategory;
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
        RecyclerView rv = findViewById(R.id.rvExerciseHistory);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WorkoutExerciseAdapter();
        adapter.setUseGrouping(false); // Disable grouping for exercise history
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
                        JSONObject obj = historyArr.getJSONObject(i);
                        historyList.add(WorkoutExercise.fromJson(obj));
                    }
                    
                    runOnUiThread(() -> adapter.setExercises(historyList));
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
}
