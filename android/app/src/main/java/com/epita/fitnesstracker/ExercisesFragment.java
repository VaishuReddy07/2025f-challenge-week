package com.epita.fitnesstracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.epita.fitnesstracker.adapter.ExerciseAdapter;
import com.epita.fitnesstracker.api.ApiClient;
import com.epita.fitnesstracker.model.Exercise;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExercisesFragment extends Fragment {

    private ExerciseAdapter adapter;
    private ChipGroup cgCategories;
    private final List<Exercise> allExercises = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exercises, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cgCategories = view.findViewById(R.id.cgExerciseCategories);
        RecyclerView rv = view.findViewById(R.id.rvExercises);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ExerciseAdapter();
        adapter.setOnItemClickListener(exercise -> {
            Intent intent = new Intent(requireContext(), ExerciseHistoryActivity.class);
            intent.putExtra("EXERCISE_ID", exercise.getId());
            intent.putExtra("EXERCISE_NAME", exercise.getName());
            intent.putExtra("EXERCISE_CATEGORY", exercise.getCategory());
            startActivity(intent);
        });
        rv.setAdapter(adapter);

        cgCategories.setOnCheckedStateChangeListener((group, checkedIds) -> filterExercises());

        fetchExercises();
    }

    private void fetchExercises() {
        ApiClient.get("/exercises", new ApiClient.Callback() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JSONArray arr = new JSONArray(responseBody);
                    allExercises.clear();
                    Set<String> categories = new HashSet<>();
                    
                    for (int i = 0; i < arr.length(); i++) {
                        Exercise ex = Exercise.fromJson(arr.getJSONObject(i));
                        allExercises.add(ex);
                        categories.add(ex.getCategory());
                    }
                    
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            setupCategoryChips(categories);
                            adapter.setExercises(allExercises);
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

    private void setupCategoryChips(Set<String> categories) {
        // Clear existing except "All"
        int childCount = cgCategories.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View child = cgCategories.getChildAt(i);
            if (child.getId() != R.id.chipAllExercises) {
                cgCategories.removeView(child);
            }
        }

        for (String category : categories) {
            Chip chip = new Chip(requireContext());
            chip.setText(category);
            chip.setCheckable(true);
            chip.setClickable(true);
            cgCategories.addView(chip);
        }
    }

    private void filterExercises() {
        int checkedId = cgCategories.getCheckedChipId();
        if (checkedId == R.id.chipAllExercises || checkedId == View.NO_ID) {
            adapter.setExercises(allExercises);
            return;
        }

        Chip selectedChip = cgCategories.findViewById(checkedId);
        if (selectedChip == null) return;

        String category = selectedChip.getText().toString();
        
        List<Exercise> filtered = new ArrayList<>();
        for (Exercise ex : allExercises) {
            if (ex.getCategory().equals(category)) {
                filtered.add(ex);
            }
        }
        adapter.setExercises(filtered);
    }
}
