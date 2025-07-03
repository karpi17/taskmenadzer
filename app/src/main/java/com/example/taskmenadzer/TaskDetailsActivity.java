package com.example.taskmenadzer;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.taskmenadzer.model.Task;
import com.example.taskmenadzer.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TaskDetailsActivity extends AppCompatActivity {
    private EditText editTaskTitle, editTaskDescription, editTaskDeadline;
    private Task taskToEdit;
    private CheckBox checkboxDone;
    private AutoCompleteTextView editTaskGroup; // Poprawny typ
    private boolean notifiedIndividuallyNearDeadline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task_details);

        editTaskTitle = findViewById(R.id.editTaskTitle);
        editTaskDescription = findViewById(R.id.editTaskDescription);
        editTaskDeadline = findViewById(R.id.editTaskDeadline);
        editTaskGroup = findViewById(R.id.editTaskGroup); // Inicjalizacja AutoCompleteTextView
        checkboxDone = findViewById(R.id.checkbox_done);
        Button saveTaskButton = findViewById(R.id.saveTaskButton);

        saveTaskButton.setOnClickListener(v -> saveTask());
        ArrayAdapter<String> groupAdapter = getStringArrayAdapter();
        editTaskGroup.setAdapter(groupAdapter);
        editTaskDeadline.setOnClickListener(v -> showDateTimePicker());

        checkboxDone.setOnCheckedChangeListener((buttonView, isChecked) -> Log.d("TaskDetailsActivity", "CheckBox 'zrobione' ZMIENIONO na: " + isChecked));

        taskToEdit = (Task) getIntent().getSerializableExtra("taskToEdit");
        if (taskToEdit != null) {
            editTaskTitle.setText(taskToEdit.getTitle());
            editTaskDescription.setText(taskToEdit.getDescription());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            if (taskToEdit.getDeadline() != null) { // Dodatkowe sprawdzenie nulla dla daty
                editTaskDeadline.setText(sdf.format(taskToEdit.getDeadline()));
            }
            checkboxDone.setChecked(taskToEdit.isDone());

            String groupName = "";
            if (taskToEdit.getGroup() != null) { // Dodatkowe sprawdzenie nulla dla grupy
                switch (taskToEdit.getGroup()) {
                    case TODO:
                        groupName = "Do zrobienia";
                        break;
                    case IMPORTANT:
                        groupName = "Ważne";
                        break;
                    case LESS_IMPORTANT:
                        groupName = "Mniej ważne";
                        break;
                    case IN_FREE_TIME:
                        groupName = "Na wolny czas";
                        break;
                    case FINISHED:
                        groupName = "Zakończone";
                        break;
                    case ARCHIVED:
                        groupName = "Archiwum";
                        break;
                    case NEAR_ENDING_DEADLINE:
                        groupName = "Bliski termin";
                        break;
                }
            }
            // --- POCZĄTEK ZMIANY: Ustawianie tekstu dla AutoCompleteTextView ---
            editTaskGroup.setText(groupName, false); // Ustawia tekst i nie filtruje
            // --- KONIEC ZMIANY ---

            // Usunięto starą logikę dla Spinnera (pętla i setSelection)
        }
    }

    @NonNull
    private ArrayAdapter<String> getStringArrayAdapter() {
        String[] groups = {"Do zrobienia", "Ważne", "Mniej ważne", "Na wolny czas", "Zakończone", "Archiwum", "Bliski termin"};
        // Użyj innego layoutu dla elementów w rozwijanej liście AutoCompleteTextView,
        // aby były bardziej czytelne. simple_dropdown_item_1line jest standardem.
        // ZMIANA LAYOUTU ELEMENTU
        // groupAdapter.setDropDownViewResource(...); // Ta linia nie jest potrzebna/używana
        // w ten sam sposób dla AutoCompleteTextView
        // jak dla Spinnera. Layout jest ustawiany w konstruktorze.
        return new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, // ZMIANA LAYOUTU ELEMENTU
                groups);
    }

    private void showDateTimePicker() {
        // ... (bez zmian) ...
        final Calendar calendar = Calendar.getInstance();
        if (taskToEdit != null && taskToEdit.getDeadline() != null) {
            calendar.setTime(taskToEdit.getDeadline());
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                editTaskDeadline.setText(sdf.format(calendar.getTime()));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);

            timePickerDialog.show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private String mapGroupNameToEnum(String groupName) {
        // ... (bez zmian) ...
        switch (groupName) {
            case "Ważne":
                return "IMPORTANT";
            case "Mniej ważne":
                return "LESS_IMPORTANT";
            case "Na wolny czas":
                return "IN_FREE_TIME";
            case "Zakończone":
                return "FINISHED";
            case "Archiwum":
                return "ARCHIVED";
            case "Bliski termin":
                return "NEAR_ENDING_DEADLINE";
            default:
                return "TODO";
        }
    }

    @SuppressLint("SetTextI18n")
    private void saveTask() {
        String title = editTaskTitle.getText().toString();
        String description = editTaskDescription.getText().toString();
        String deadlineString = editTaskDeadline.getText().toString();
        // --- POCZĄTEK ZMIANY: Pobieranie wartości z AutoCompleteTextView ---
        String group = editTaskGroup.getText().toString();
        // --- KONIEC ZMIANY ---
        boolean isDone = checkboxDone.isChecked();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Date deadline = null;
        try {
            if (!deadlineString.isEmpty()) {
                deadline = sdf.parse(deadlineString);
            }
        } catch (ParseException e) {
            Log.e("TaskDetailsActivity", "Błąd parsowania daty: '" + deadlineString + "'", e);
            Toast.makeText(this, "Niepoprawny format daty.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty()) {
            Toast.makeText(this, "Tytuł zadania nie może być pusty.", Toast.LENGTH_SHORT).show();
            Log.e("TaskDetailsActivity", "Tytuł zadania jest pusty.");
            return;
        }
        if (deadline == null) {
            Toast.makeText(this, "Termin zadania nie może być pusty.", Toast.LENGTH_SHORT).show();
            Log.e("TaskDetailsActivity", "Termin zadania jest pusty/niepoprawny.");
            return;
        }
        if (group.isEmpty()) { // Dodatkowa walidacja dla grupy
            Toast.makeText(this, "Grupa zadania nie może być pusta.", Toast.LENGTH_SHORT).show();
            Log.e("TaskDetailsActivity", "Grupa zadania jest pusta.");
            return;
        }


        Date now = new Date();
        if (taskToEdit == null && deadline.before(now)) {
            Toast.makeText(this, "Nie można ustawić przeszłej daty dla nowego zadania!", Toast.LENGTH_SHORT).show();
            return;
        }

        String groupEnumName = mapGroupNameToEnum(group);

        boolean isArchived = false;
        int id = 0;

        if (taskToEdit != null) {
            id = taskToEdit.getId();
            isArchived = taskToEdit.isArchived();
        }

        Task.Group taskGroupEnum; // Użyj innej nazwy zmiennej, aby uniknąć konfliktu
        try {
            taskGroupEnum = Task.Group.valueOf(groupEnumName);
        } catch (IllegalArgumentException e) {
            Log.e("TaskDetailsActivity", "Nieznana nazwa grupy: " + groupEnumName, e);
            Toast.makeText(this, "Wybrana grupa jest nieprawidłowa.", Toast.LENGTH_SHORT).show();
            return; // Zakończ, jeśli grupa jest nieprawidłowa
        }


        if (isDone && taskGroupEnum != Task.Group.FINISHED && taskGroupEnum != Task.Group.ARCHIVED) {
            taskGroupEnum = Task.Group.FINISHED;
            editTaskGroup.setText("Zakończone", false); // Opcjonalnie: zaktualizuj tekst w polu
        }

        Task newTask = new Task(title, description, deadline, taskGroupEnum, isArchived, isDone, notifiedIndividuallyNearDeadline, BIND_EXTERNAL_SERVICE_LONG);
        if (id != 0) {
            newTask.setId(id);
        }

        Log.d("TaskDetailsActivity", "Przygotowano zadanie do zapisu - Title: " + newTask.getTitle() + ", isDone: " + newTask.isDone() + ", Group: " + newTask.getGroup() + ", ID: " + newTask.getId());

        Intent resultIntent = new Intent();
        resultIntent.putExtra("newTask", newTask);
        int position = getIntent().getIntExtra("taskPosition", -1);
        resultIntent.putExtra("taskPosition", position);

        setResult(RESULT_OK, resultIntent);
        finish();
    }
}