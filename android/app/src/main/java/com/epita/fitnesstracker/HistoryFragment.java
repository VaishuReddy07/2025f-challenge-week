package com.epita.fitnesstracker;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private WorkoutAdapter adapter;
    private RecyclerView rvWorkouts;
    private LinearLayout llEmptyState;
    private ChipGroup chipGroupFilter;

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

        rvWorkouts = view.findViewById(R.id.rvWorkouts);
        llEmptyState = view.findViewById(R.id.llEmptyState);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);

        rvWorkouts.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new WorkoutAdapter();
        adapter.setOnItemClickListener(workout -> {
            Intent intent = new Intent(requireContext(), WorkoutDetailActivity.class);
            intent.putExtra("WORKOUT_ID", workout.getId());
            startActivity(intent);
        });
        rvWorkouts.setAdapter(adapter);

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            fetchWorkouts();
        });

        view.findViewById(R.id.fabAddWorkout).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateWorkoutActivity.class);
            createWorkoutLauncher.launch(intent);
        });

        fetchWorkouts();
    }

    private void fetchWorkouts() {
        int checkedId = chipGroupFilter.getCheckedChipId();
        String query = "";

        if (checkedId != R.id.chipAll) {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String to = sdf.format(new Date());
            
            if (checkedId == R.id.chipWeek) {
                cal.add(Calendar.DAY_OF_YEAR, -7);
            } else if (checkedId == R.id.chipMonth) {
                cal.add(Calendar.MONTH, -1);
            } else if (checkedId == R.id.chip3Months) {
                cal.add(Calendar.MONTH, -3);
            }
            
            String from = sdf.format(cal.getTime());
            query = "?from=" + from + "&to=" + to;
        }

        ApiClient.get("/workouts" + query, new ApiClient.Callback() {
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
                        requireActivity().runOnUiThread(() -> {
                            adapter.setWorkouts(list);
                            if (list.isEmpty()) {
                                rvWorkouts.setVisibility(View.GONE);
                                llEmptyState.setVisibility(View.VISIBLE);
                            } else {
                                rvWorkouts.setVisibility(View.VISIBLE);
                                llEmptyState.setVisibility(View.GONE);
                            }
                        });
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
