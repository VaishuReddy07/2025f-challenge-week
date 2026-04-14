package com.epita.fitnesstracker.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.epita.fitnesstracker.R;
import com.epita.fitnesstracker.model.Workout;

import java.util.ArrayList;
import java.util.List;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Workout workout);
    }

    private List<Workout> workouts = new ArrayList<>();
    private OnItemClickListener listener;

    public void setWorkouts(List<Workout> workouts) {
        this.workouts = workouts;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_workout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Workout workout = workouts.get(position);
        holder.tvDate.setText(workout.getDate());
        holder.tvDuration.setText(workout.getDurationMin() + " min");
        holder.tvNotes.setText(workout.getNotes());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(workout);
            }
        });
    }

    @Override
    public int getItemCount() {
        return workouts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvDuration, tvNotes;

        ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvWorkoutDate);
            tvDuration = itemView.findViewById(R.id.tvWorkoutDuration);
            tvNotes = itemView.findViewById(R.id.tvWorkoutNotes);
        }
    }
}
