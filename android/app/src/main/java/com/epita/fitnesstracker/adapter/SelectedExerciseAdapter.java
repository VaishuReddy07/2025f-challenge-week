package com.epita.fitnesstracker.adapter;

import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.epita.fitnesstracker.R;
import com.epita.fitnesstracker.model.Exercise;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SelectedExerciseAdapter extends RecyclerView.Adapter<SelectedExerciseAdapter.ViewHolder> {

    public static class ExerciseSet {
        public int reps = 0;
        public double weight = 0.0;
        public boolean isDone = false;
    }

    public static class SelectedExercise {
        public Exercise exercise;
        public String notes = "";
        public List<ExerciseSet> sets = new ArrayList<>();
        public int restDurationSeconds = 60; // Default rest duration

        public SelectedExercise(Exercise exercise) {
            this.exercise = exercise;
            this.sets.add(new ExerciseSet());
        }
    }

    private final List<SelectedExercise> selectedExercises = new ArrayList<>();
    private CountDownTimer currentTimer;

    public void addExercise(Exercise exercise) {
        selectedExercises.add(new SelectedExercise(exercise));
        notifyItemInserted(selectedExercises.size() - 1);
    }

    public List<SelectedExercise> getSelectedExercises() {
        return selectedExercises;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_create_workout_exercise, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SelectedExercise item = selectedExercises.get(position);
        holder.tvName.setText(item.exercise.getName());
        holder.etNotes.setText(item.notes);

        holder.etNotes.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(String s) {
                item.notes = s;
            }
        });

        holder.btnRemoveExercise.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                selectedExercises.remove(pos);
                notifyItemRemoved(pos);
            }
        });

        holder.btnAddSet.setOnClickListener(v -> {
            item.sets.add(new ExerciseSet());
            refreshSets(holder, item);
        });

        holder.cgRestDurations.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip60s)) item.restDurationSeconds = 60;
            else if (checkedIds.contains(R.id.chip90s)) item.restDurationSeconds = 90;
            else if (checkedIds.contains(R.id.chip120s)) item.restDurationSeconds = 120;
            holder.tvRestTimer.setText("Rest Timer: " + item.restDurationSeconds + "s");
        });

        refreshSets(holder, item);

        holder.btnStartRest.setOnClickListener(v -> {
            if (currentTimer != null) currentTimer.cancel();
            
            holder.btnStartRest.setEnabled(false);
            holder.cgRestDurations.setEnabled(false);
            
            currentTimer = new CountDownTimer(item.restDurationSeconds * 1000L, 1000) {
                public void onTick(long millisUntilFinished) {
                    holder.tvRestTimer.setText("Rest: " + (millisUntilFinished / 1000) + "s");
                }
                public void onFinish() {
                    holder.tvRestTimer.setText("Rest Over!");
                    holder.btnStartRest.setEnabled(true);
                    holder.cgRestDurations.setEnabled(true);
                    holder.tvRestTimer.setText("Rest Timer: " + item.restDurationSeconds + "s");
                }
            }.start();
        });
    }

    private void refreshSets(ViewHolder holder, SelectedExercise item) {
        holder.llSetsContainer.removeAllViews();
        for (int i = 0; i < item.sets.size(); i++) {
            final int index = i;
            ExerciseSet set = item.sets.get(i);
            View setView = LayoutInflater.from(holder.itemView.getContext())
                    .inflate(R.layout.item_set_row, holder.llSetsContainer, false);

            TextView tvNum = setView.findViewById(R.id.tvSetNumber);
            EditText etReps = setView.findViewById(R.id.etReps);
            EditText etWeight = setView.findViewById(R.id.etWeight);
            CheckBox cbDone = setView.findViewById(R.id.cbDone);
            ImageButton btnDelete = setView.findViewById(R.id.btnDeleteSet);

            tvNum.setText(String.valueOf(i + 1));
            etReps.setText(set.reps > 0 ? String.valueOf(set.reps) : "");
            etWeight.setText(set.weight > 0 ? String.valueOf(set.weight) : "");
            cbDone.setChecked(set.isDone);

            etReps.addTextChangedListener(new SimpleTextWatcher() {
                @Override public void onTextChanged(String s) {
                    try { set.reps = Integer.parseInt(s); } catch (Exception ignored) { set.reps = 0; }
                }
            });

            etWeight.addTextChangedListener(new SimpleTextWatcher() {
                @Override public void onTextChanged(String s) {
                    try { set.weight = Double.parseDouble(s); } catch (Exception ignored) { set.weight = 0.0; }
                }
            });

            cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
                set.isDone = isChecked;
                if (isChecked) {
                    holder.llRestTimer.setVisibility(View.VISIBLE);
                }
            });

            btnDelete.setOnClickListener(v -> {
                item.sets.remove(index);
                refreshSets(holder, item);
            });

            holder.llSetsContainer.addView(setView);
        }
    }

    @Override
    public int getItemCount() {
        return selectedExercises.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRestTimer;
        EditText etNotes;
        LinearLayout llSetsContainer, llRestTimer;
        Button btnAddSet, btnStartRest;
        ImageButton btnRemoveExercise;
        ChipGroup cgRestDurations;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvExName);
            etNotes = itemView.findViewById(R.id.etExerciseNotes);
            llSetsContainer = itemView.findViewById(R.id.llSetsContainer);
            btnAddSet = itemView.findViewById(R.id.btnAddSet);
            btnRemoveExercise = itemView.findViewById(R.id.btnRemoveExercise);
            llRestTimer = itemView.findViewById(R.id.llRestTimer);
            tvRestTimer = itemView.findViewById(R.id.tvRestTimer);
            btnStartRest = itemView.findViewById(R.id.btnStartRest);
            cgRestDurations = itemView.findViewById(R.id.cgRestDurations);
        }
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            onTextChanged(s.toString());
        }
        @Override public void afterTextChanged(Editable s) {}
        public abstract void onTextChanged(String s);
    }
}
