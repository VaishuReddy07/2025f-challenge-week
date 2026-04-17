package com.epita.fitnesstracker;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.epita.fitnesstracker.api.ApiClient;

import org.json.JSONObject;

public class AddExerciseActivity extends AppCompatActivity {

    private EditText etName, etCategory, etDesc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_exercise);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Exercise");
        }

        etName = findViewById(R.id.etNewExName);
        etCategory = findViewById(R.id.etNewExCategory);
        etDesc = findViewById(R.id.etNewExDesc);
        Button btnSave = findViewById(R.id.btnSaveCustomExercise);

        btnSave.setOnClickListener(v -> saveExercise());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveExercise() {
        String name = etName.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();

        if (name.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Please enter name and category", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("name", name);
            body.put("category", category);
            body.put("description", desc);

            ApiClient.post("/exercises", body.toString(), new ApiClient.Callback() {
                @Override
                public void onSuccess(String responseBody) {
                    runOnUiThread(() -> {
                        Toast.makeText(AddExerciseActivity.this, "Exercise created!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> Toast.makeText(AddExerciseActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "JSON Error", Toast.LENGTH_SHORT).show();
        }
    }
}
