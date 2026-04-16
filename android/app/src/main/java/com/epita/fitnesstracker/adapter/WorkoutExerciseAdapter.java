package com.epita.fitnesstracker.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.epita.fitnesstracker.R;
import com.epita.fitnesstracker.model.WorkoutExercise;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class WorkoutExerciseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<Object> items = new ArrayList<>();
    private boolean useGrouping = true;

    public void setUseGrouping(boolean useGrouping) {
        this.useGrouping = useGrouping;
    }

    public void setExercises(List<WorkoutExercise> exercises) {
        items.clear();
        
        if (!useGrouping) {
            items.addAll(exercises);
        } else {
            // Group by category
            Map<String, List<WorkoutExercise>> grouped = new TreeMap<>();
            for (WorkoutExercise we : exercises) {
                String category = we.getCategory() != null && !we.getCategory().isEmpty() ? we.getCategory() : "Other";
                if (!grouped.containsKey(category)) {
                    grouped.put(category, new ArrayList<>());
                }
                grouped.get(category).add(we);
            }

            for (Map.Entry<String, List<WorkoutExercise>> entry : grouped.entrySet()) {
                items.add(entry.getKey()); // Header
                items.addAll(entry.getValue()); // Items
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_workout_exercise, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.tvHeader.setText((String) items.get(position));
            headerHolder.tvHeader.setTextSize(14f);
            headerHolder.tvHeader.setPadding(32, 16, 16, 8);
            headerHolder.tvHeader.setAllCaps(true);
            headerHolder.tvHeader.setTextColor(0xFF888888);
        } else {
            WorkoutExercise we = (WorkoutExercise) items.get(position);
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            itemHolder.tvName.setText(we.getName());
            
            // If it's from history, we might want to show the date
            if (we.getWorkoutDate() != null && !we.getWorkoutDate().isEmpty()) {
                itemHolder.tvName.setText(we.getName() + " (" + we.getWorkoutDate() + ")");
            }

            itemHolder.tvSets.setText(String.valueOf(we.getSets()));
            itemHolder.tvReps.setText(String.valueOf(we.getReps()));
            itemHolder.tvWeight.setText(String.format(Locale.getDefault(), "%.1f kg", we.getWeightKg()));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;
        HeaderViewHolder(View itemView) {
            super(itemView);
            tvHeader = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSets, tvReps, tvWeight;
        ItemViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvExName);
            tvSets = itemView.findViewById(R.id.tvExSets);
            tvReps = itemView.findViewById(R.id.tvExReps);
            tvWeight = itemView.findViewById(R.id.tvExWeight);
        }
    }
}
