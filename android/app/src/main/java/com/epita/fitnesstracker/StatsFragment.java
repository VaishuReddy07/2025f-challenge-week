package com.epita.fitnesstracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.epita.fitnesstracker.api.ApiClient;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.Locale;

public class StatsFragment extends Fragment {

    private TextView tvTotalWorkouts, tvTotalDuration, tvAvgDuration;
    private LinearLayout llCategoryStats;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTotalWorkouts = view.findViewById(R.id.tvTotalWorkouts);
        tvTotalDuration = view.findViewById(R.id.tvTotalDuration);
        tvAvgDuration = view.findViewById(R.id.tvAvgDuration);
        llCategoryStats = view.findViewById(R.id.llCategoryStats);

        fetchStats();
    }

    private void fetchStats() {
        ApiClient.get("/stats", new ApiClient.Callback() {
            @Override
            public void onSuccess(String responseBody) {
                try {
                    JSONObject json = new JSONObject(responseBody);
                    int totalWorkouts = json.getInt("total_workouts");
                    int totalDuration = json.getInt("total_duration_min");
                    double avgDuration = json.getDouble("avg_duration_min");
                    JSONObject perCategory = json.getJSONObject("workouts_per_category");

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            tvTotalWorkouts.setText(String.valueOf(totalWorkouts));
                            tvTotalDuration.setText(String.format(Locale.getDefault(), "%d min", totalDuration));
                            tvAvgDuration.setText(String.format(Locale.getDefault(), "%.1f min", avgDuration));

                            llCategoryStats.removeAllViews();
                            Iterator<String> keys = perCategory.keys();
                            while (keys.hasNext()) {
                                String category = keys.next();
                                try {
                                    int count = perCategory.getInt(category);
                                    addCategoryRow(category, count);
                                } catch (Exception ignored) {}
                            }
                        });
                    }
                } catch (Exception e) {
                    showError("Parse error: " + e.getMessage());
                }
            }

            @Override
            public void onError(Exception e) {
                showError("Network error: " + e.getMessage());
            }
        });
    }

    private void addCategoryRow(String category, int count) {
        TextView tv = new TextView(requireContext());
        tv.setText(String.format(Locale.getDefault(), "%s: %d workouts", category, count));
        tv.setTextSize(16f);
        tv.setPadding(0, 8, 0, 8);
        llCategoryStats.addView(tv);
    }

    private void showError(String message) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show());
        }
    }
}
