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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Locale;

public class StatsFragment extends Fragment {

    private TextView tvTotalWorkouts, tvTotalDuration, tvAvgDuration;
    private LinearLayout llCategoryStats, llPersonalRecords;

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
        llPersonalRecords = view.findViewById(R.id.llPersonalRecords);

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
                    
                    // S039: Expected new field from backend
                    JSONArray prs = json.optJSONArray("personal_records");

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            tvTotalWorkouts.setText(String.valueOf(totalWorkouts));
                            tvTotalDuration.setText(String.format(Locale.getDefault(), "%dm", totalDuration));
                            tvAvgDuration.setText(String.format(Locale.getDefault(), "%.1fm", avgDuration));

                            // Category Stats
                            llCategoryStats.removeAllViews();
                            Iterator<String> keys = perCategory.keys();
                            while (keys.hasNext()) {
                                String category = keys.next();
                                try {
                                    int count = perCategory.getInt(category);
                                    addStatRow(llCategoryStats, category, count + " workouts");
                                } catch (Exception ignored) {}
                            }
                            
                            // Personal Records (S039)
                            llPersonalRecords.removeAllViews();
                            if (prs != null && prs.length() > 0) {
                                for (int i = 0; i < prs.length(); i++) {
                                    try {
                                        JSONObject pr = prs.getJSONObject(i);
                                        String name = pr.getString("exercise_name");
                                        double weight = pr.getDouble("max_weight");
                                        addStatRow(llPersonalRecords, name, weight + " kg");
                                    } catch (Exception ignored) {}
                                }
                            } else {
                                TextView tv = new TextView(requireContext());
                                tv.setText("Log more sets to see PRs!");
                                tv.setTextColor(0xFF999999);
                                llPersonalRecords.addView(tv);
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

    private void addStatRow(LinearLayout container, String label, String value) {
        View row = LayoutInflater.from(requireContext()).inflate(android.R.layout.simple_list_item_2, container, false);
        TextView tvLabel = row.findViewById(android.R.id.text1);
        TextView tvValue = row.findViewById(android.R.id.text2);
        
        tvLabel.setText(label);
        tvLabel.setTextSize(16f);
        tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        
        tvValue.setText(value);
        tvValue.setTextColor(0xFF666666);
        
        container.addView(row);
    }

    private void showError(String message) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show());
        }
    }
}
