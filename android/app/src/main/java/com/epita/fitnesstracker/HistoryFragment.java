package com.epita.fitnesstracker;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.epita.fitnesstracker.adapter.WorkoutAdapter;
import com.epita.fitnesstracker.api.ApiClient;
import com.epita.fitnesstracker.model.Workout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private WorkoutAdapter adapter;
    private final ActivityResultLauncher<Intent> createWorkoutLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    fetchWorkouts();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rv = view.findViewById(R.id.rvWorkouts);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new WorkoutAdapter();
        adapter.setOnItemClickListener(workout -> {
            Intent intent = new Intent(requireContext(), WorkoutDetailActivity.class);
            intent.putExtra("WORKOUT_ID", workout.getId());
            startActivity(intent);
        });
        rv.setAdapter(adapter);

        view.findViewById(R.id.fabAddWorkout).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateWorkoutActivity.class);
            createWorkoutLauncher.launch(intent);
        });

        fetchWorkouts();
    }

    private void fetchWorkouts() {
        ApiClient.get("/workouts", new ApiClient.Callback() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JSONArray arr = new JSONArray(responseBody);
                    List<Workout> list = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        list.add(Workout.fromJson(obj));
                    }
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> adapter.setWorkouts(list));
                    }
                } catch (Exception e) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(),
                                        "Parse error: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show());
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    "Network error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
