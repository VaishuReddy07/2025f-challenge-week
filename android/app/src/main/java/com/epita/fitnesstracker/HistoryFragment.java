package com.epita.fitnesstracker;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.epita.fitnesstracker.adapter.WorkoutAdapter;
import com.epita.fitnesstracker.api.ApiClient;
import com.epita.fitnesstracker.model.Workout;
import com.google.android.material.chip.Chip;
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
    private TextView tvWorkoutCount, tvEmptyMessage;
    private SwipeRefreshLayout swipeRefresh;

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

        swipeRefresh = view.findViewById(R.id.swipeRefreshHistory);
        rvWorkouts = view.findViewById(R.id.rvWorkouts);
        llEmptyState = view.findViewById(R.id.llEmptyState);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        tvWorkoutCount = view.findViewById(R.id.tvWorkoutCount);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);

        rvWorkouts.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new WorkoutAdapter();
        adapter.setOnItemClickListener(workout -> {
            Intent intent = new Intent(requireContext(), WorkoutDetailActivity.class);
            intent.putExtra("WORKOUT_ID", workout.getId());
            startActivity(intent);
        });
        rvWorkouts.setAdapter(adapter);

        setupSwipeToDelete();

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> fetchWorkouts());
        
        swipeRefresh.setOnRefreshListener(this::fetchWorkouts);

        view.findViewById(R.id.fabAddWorkout).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateWorkoutActivity.class);
            createWorkoutLauncher.launch(intent);
        });

        fetchWorkouts();
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int position = viewHolder.getAdapterPosition();
                Workout workout = adapter.getWorkout(position);
                
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Workout")
                        .setMessage("Are you sure you want to delete this workout?")
                        .setPositiveButton("Delete", (dialog, which) -> deleteWorkout(workout.getId(), position))
                        .setNegativeButton("Cancel", (dialog, which) -> adapter.notifyItemChanged(position))
                        .setOnCancelListener(dialog -> adapter.notifyItemChanged(position))
                        .show();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(rvWorkouts);
    }

    private void deleteWorkout(int workoutId, int position) {
        ApiClient.delete("/workouts/" + workoutId, new ApiClient.Callback() {
            @Override
            public void onSuccess(String responseBody) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        adapter.removeWorkout(position);
                        Toast.makeText(requireContext(), "Workout deleted", Toast.LENGTH_SHORT).show();
                        updateCountLabel();
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        adapter.notifyItemChanged(position);
                        Toast.makeText(requireContext(), "Error deleting workout", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void updateCountLabel() {
        int count = adapter.getItemCount();
        tvWorkoutCount.setText(count + " workouts");
        if (count == 0) {
            rvWorkouts.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvWorkouts.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
        }
    }

    private void fetchWorkouts() {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        
        int checkedId = chipGroupFilter.getCheckedChipId();
        String query = "";

        if (checkedId != View.NO_ID && checkedId != R.id.chipAll) {
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
                            swipeRefresh.setRefreshing(false);
                            adapter.setWorkouts(list);
                            updateCountLabel();
                            
                            if (list.isEmpty()) {
                                if (checkedId == R.id.chipAll || checkedId == View.NO_ID) {
                                    tvEmptyMessage.setText("No workouts yet");
                                } else {
                                    Chip chip = chipGroupFilter.findViewById(checkedId);
                                    if (chip != null) tvEmptyMessage.setText("No workouts for " + chip.getText());
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            swipeRefresh.setRefreshing(false);
                            Toast.makeText(requireContext(), "Parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(requireContext(), "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
}
