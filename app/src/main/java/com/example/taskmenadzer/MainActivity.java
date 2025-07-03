package com.example.taskmenadzer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.taskmenadzer.model.AppDatabase;
import com.example.taskmenadzer.model.Task;
import com.example.taskmenadzer.model.TaskEntity;
import com.example.taskmenadzer.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskActionListener { // Dodaj implementację interfejsu
    private static final String TAG = "MainActivity";
    private final List<Task> currentTasksFullList = new ArrayList<>(); // Przechowuje pełną, niefiltrowaną listę z LiveData
    private final ActivityResultLauncher<Intent> settingsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            // Ponowne utworzenie Activity, aby zastosować zmiany (np. tryb ciemny)
            // Również odświeży SharedPreferences dla nearDeadlineDays w onResume
            recreate();
        }
    });
    private ActivityResultLauncher<Intent> addTaskLauncher;
    private ActivityResultLauncher<Intent> editTaskLauncher;
    private int currentFilterAdapterPosition = 0;
    private AppDatabase db;
    private TaskAdapter adapter;
    private RecyclerView tasksRecyclerView;
    private ImageView emptyStateImageView;
    private TextView emptyStateTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // --- Ustawienia początkowe (tryb ciemny, uprawnienia) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
        // Do odczytu ustawień globalnych
        SharedPreferences settingsPrefs = getSharedPreferences("settings", MODE_PRIVATE); // <--- POPRAWKA TUTAJ
        boolean darkMode = settingsPrefs.getBoolean("darkMode", false);
        AppCompatDelegate.setDefaultNightMode(darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        db = AppDatabase.getDatabase(getApplicationContext());
        // --- Konfiguracja RecyclerView i Adaptera ---
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView);
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(this); // <<<< NAJPIERW STWÓRZ OBIEKT ADAPTERA
        tasksRecyclerView.setAdapter(adapter);

        // --- ActivityResultLaunchers dla Dodawania/Edycji Zadań ---
        setupTaskLaunchers();

        // --- Przyciski i UI ---
        findViewById(R.id.settingsButton).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            settingsLauncher.launch(intent);
        });
        emptyStateImageView = findViewById(R.id.emptyStateImageView); // Użyj R z Twojego pakietu
        emptyStateTextView = findViewById(R.id.emptyStateTextView);   // Użyj R z Twojego pakietu


        // << DODAJ TO
        com.google.android.material.floatingactionbutton.FloatingActionButton fabAddTask = findViewById(R.id.fabAddTask); // << INICJALIZACJA
        fabAddTask.setOnClickListener(v -> { // << USTAW LISTENER
            Intent intent = new Intent(MainActivity.this, TaskDetailsActivity.class);
            addTaskLauncher.launch(intent);
            // Log.d(TAG, "FAB clicked - launching TaskDetailsActivity"); // Opcjonalny log
        });
        currentFilterAdapterPosition = settingsPrefs.getInt("defaultCategory", 0); // Domyślnie 0
        Log.d(TAG, "onCreate - Initial currentFilterAdapterPosition from settings: " + currentFilterAdapterPosition);


        // --- Filtr Grup (AutoCompleteTextView) ---
        // Przekazujemy `settingsPrefs` tylko do odczytu tablicy grup, nie do zapisu wyboru użytkownika
        setupGroupFilter();


        // --- Inicjalizacja Workerów ---
        setupWorkers();

        // --- Obserwacja Zmian w Bazie Danych (LiveData) ---
        observeTasks();

        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button testTaskStateUpdaterButton = findViewById(R.id.testTaskStateUpdaterWorkerButton);
        if (testTaskStateUpdaterButton != null) {
            testTaskStateUpdaterButton.setOnClickListener(v -> {
                Log.d(TAG, "Uruchamianie TaskStateUpdaterWorker na żądanie...");
                OneTimeWorkRequest oneTimeTaskStateUpdateRequest = new OneTimeWorkRequest.Builder(TaskStateUpdaterWorker.class).addTag("TestTaskStateUpdater").build();
                WorkManager.getInstance(MainActivity.this).enqueue(oneTimeTaskStateUpdateRequest);
                Toast.makeText(MainActivity.this, "Zakolejkowano TaskStateUpdaterWorker (test)", Toast.LENGTH_SHORT).show();

            });
        }
    }


    private void setupTaskLaunchers() {
        addTaskLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Task newTask = (Task) result.getData().getSerializableExtra("newTask");
                if (newTask != null) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        db.taskDao().insert(TaskEntity.fromTask(newTask));
                        runOnUiThread(() -> Log.d(TAG, "Dodano zadanie: " + newTask.getTitle()));
                    });
                    // LiveData automatycznie odświeży listę
                }
            }
        });

        editTaskLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Task updatedTask = (Task) result.getData().getSerializableExtra("newTask");
                if (updatedTask != null) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        db.taskDao().update(TaskEntity.fromTask(updatedTask));
                        // runOnUiThread(() -> testTaskUpdateInDb(updatedTask.getId())); // testTaskUpdateInDb też musi być w tle
                    });
                    Log.d(TAG, "Zaktualizowano zadanie: " + updatedTask.getTitle());
                    // LiveData automatycznie odświeży listę
                }
            }
        });
    }

    private void setupGroupFilter() {
        AutoCompleteTextView groupAutoCompleteTextView = findViewById(R.id.groupFilterAutoCompleteTextView);
        String[] groupArray = getResources().getStringArray(R.array.group_array_for_spinner);
        ArrayAdapter<String> groupAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, groupArray);
        groupAutoCompleteTextView.setAdapter(groupAdapter);

        if (currentFilterAdapterPosition >= 0 && currentFilterAdapterPosition < groupArray.length) {
            groupAutoCompleteTextView.setText(groupAdapter.getItem(currentFilterAdapterPosition), false);
            Log.d(TAG, "setupGroupFilter - Initial text set to: '" + groupArray[currentFilterAdapterPosition] + "' for position: " + currentFilterAdapterPosition);
        } else {
            Log.w(TAG, "setupGroupFilter - currentFilterAdapterPosition (" + currentFilterAdapterPosition + ") is out of bounds. Defaulting to 0.");
            currentFilterAdapterPosition = 0; // Fallback
            if (groupArray.length > 0) {
                groupAutoCompleteTextView.setText(groupAdapter.getItem(currentFilterAdapterPosition), false);
            }
        }

        View.OnClickListener showDropdownListener = v -> {
            groupAdapter.getFilter().filter(null);
            groupAutoCompleteTextView.showDropDown();
            Log.d(TAG, "OnClickListener - Dropdown shown with reset filter.");
        };

        groupAutoCompleteTextView.setOnClickListener(showDropdownListener);

        groupAutoCompleteTextView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && groupAutoCompleteTextView.isAttachedToWindow()) {
                // Można rozważyć lekkie opóźnienie, jeśli są problemy z timingiem po recreate
                // v.postDelayed(() -> {
                //    groupAdapter.getFilter().filter(null);
                //    groupAutoCompleteTextView.showDropDown();
                //    Log.d(TAG, "OnFocusChangeListener - Dropdown shown with reset filter.");
                // }, 50); // Krótkie opóźnienie
                // Bez opóźnienia:
                groupAdapter.getFilter().filter(null);
                groupAutoCompleteTextView.showDropDown();
                Log.d(TAG, "OnFocusChangeListener - Dropdown shown with reset filter.");
            }
        });


        groupAutoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            Log.d(TAG, "OnItemClickListener - User selected position: " + position);
            this.currentFilterAdapterPosition = position;
            applyCurrentFilter();


            groupAutoCompleteTextView.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && view != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });
    }


    private void observeTasks() {
        db.taskDao().getAllTasksLiveData().observe(this, taskEntities -> {
            if (taskEntities != null) {
                currentTasksFullList.clear();
                for (TaskEntity entity : taskEntities) {
                    currentTasksFullList.add(entity.toTask());
                }
                Log.d(TAG, "Tasks updated from LiveData: " + currentTasksFullList.size() + " tasks. Applying current filter.");
                applyCurrentFilter(); // Zastosuj filtr po aktualizacji pełnej listy, użyje this.currentFilterAdapterPosition
            }
        });
    }


    private void setupWorkers() {
        PeriodicWorkRequest taskStateUpdateWorkRequest = new PeriodicWorkRequest.Builder(TaskStateUpdaterWorker.class, 1, TimeUnit.DAYS) // Np. co 1 dzień
                // TODO: Możesz dodać ograniczenia
                .build();
        WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork("TaskStateUpdateWorker", // Unikalna nazwa
                ExistingPeriodicWorkPolicy.KEEP, taskStateUpdateWorkRequest);

        Log.d(TAG, "Okresowe Workery (NearDeadline, TaskStateUpdate) zakolejkowane.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // applyCurrentFilter(); // Filtr jest teraz stosowany po zmianie LiveData lub wyborze w spinnerze.
        // Jeśli ustawienia (np. nearDeadlineDays) się zmieniły i chcesz natychmiastowego odświeżenia,
        // to wywołanie tutaj może być potrzebne, ale SharedPreferences są odczytywane w applyCurrentFilter.
        // Sprawdź, czy `recreate()` po zmianie ustawień jest wystarczające.
    }

    // Metoda applyCurrentFilter jest kluczowa do wyświetlania zadań
    private void applyCurrentFilter() {
        if (adapter == null) {
            Log.w(TAG, "applyCurrentFilter: Adapter jest null.");
            if (this.tasksRecyclerView != null) this.tasksRecyclerView.setVisibility(View.GONE);
            if (emptyStateImageView != null) emptyStateImageView.setVisibility(View.VISIBLE);
            if (emptyStateTextView != null) emptyStateTextView.setVisibility(View.VISIBLE);
            return;
        }

        AutoCompleteTextView groupAutoCompleteTextView = findViewById(R.id.groupFilterAutoCompleteTextView);
        String selectedGroupText = groupAutoCompleteTextView.getText().toString();

        String[] groupArray = getResources().getStringArray(R.array.group_array_for_spinner);
        int selectedPosition = -1; // Domyślnie -1, jeśli nie znaleziono

        for (int i = 0; i < groupArray.length; i++) {
            if (groupArray[i].equals(selectedGroupText)) {
                selectedPosition = i;
                break;
            }
        }

        if (selectedPosition == -1) {
            // Jeśli z jakiegoś powodu tekst nie pasuje do żadnej opcji (np. na starcie, jeśli nie ustawiono tekstu)
            // Możemy ustawić domyślną pozycję, np. 0 (dla "Wszystkie")
            // Lub obsłużyć to inaczej. Dla bezpieczeństwa, załóżmy, że chcemy pierwszą opcję,
            // jeśli tekst jest pusty lub nie pasuje.
            SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
            selectedPosition = prefs.getInt("defaultCategory", 0); // Wróć do zapisanego lub domyślnego
            Log.w(TAG, "applyCurrentFilter: Nie udało się znaleźć pozycji dla tekstu '" + selectedGroupText + "'. Używam pozycji: " + selectedPosition);
        }

        Log.d(TAG, "applyCurrentFilter: Using currentFilterAdapterPosition: " + this.currentFilterAdapterPosition);

        List<Task> filteredTasks = filterTasksByGroup(new ArrayList<>(currentTasksFullList), this.currentFilterAdapterPosition);
        adapter.submitList(filteredTasks);

        Log.d(TAG, "Rozmiar filteredTasks: " + filteredTasks.size());
        Log.d(TAG, "Czy filteredTasks jest pusta? " + filteredTasks.isEmpty());


        if (filteredTasks.isEmpty()) {
            tasksRecyclerView.setVisibility(View.GONE);
            emptyStateImageView.setVisibility(View.VISIBLE);
            emptyStateTextView.setVisibility(View.VISIBLE);
            Log.d(TAG, "Filtered list is empty. Showing empty state.");
        } else {
            tasksRecyclerView.setVisibility(View.VISIBLE);
            emptyStateImageView.setVisibility(View.GONE);
            emptyStateTextView.setVisibility(View.GONE);
            Log.d(TAG, "Filtered list is not empty. Showing RecyclerView.");
        }
    }

    @Override
    public void onTaskDeleteClicked(Task taskToDelete) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.taskDao().delete(TaskEntity.fromTask(taskToDelete));
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Zadanie '" + taskToDelete.getTitle() + "' usunięte", Toast.LENGTH_SHORT).show());
        });
    }

    private List<Task> filterTasksByGroup(List<Task> allTasks, int groupPosition) {
        List<Task> filtered = new ArrayList<>();
        String[] groupNames = getResources().getStringArray(R.array.group_array_for_spinner);

        if (groupPosition < 0 || groupPosition >= groupNames.length) {
            Log.w(TAG, "filterTasksByGroup: Nieprawidłowa pozycja grupy: " + groupPosition);
            return new ArrayList<>(allTasks); // Zwróć wszystkie w przypadku błędu
        }

        String selectedGroupName = groupNames[groupPosition];
        Log.d(TAG, "Filtrowanie według grupy: " + selectedGroupName);

        switch (selectedGroupName) {
            case "Wszystkie": // Upewnij się, że nazwa "Wszystkie" jest w R.array.group_array_for_spinner
                return new ArrayList<>(allTasks); // Zwróć kopię, aby uniknąć modyfikacji oryginalnej listy

            case "Archiwum":
                for (Task t : allTasks) {
                    if (t.getGroup() == Task.Group.ARCHIVED) {
                        filtered.add(t);
                    }
                }
                return filtered;

            case "Bliski termin":
                for (Task t : allTasks) {
                    // Logika dla "Bliski termin" jest teraz obsługiwana przez TaskStateUpdaterWorker,
                    // który zmienia grupę zadania na NEAR_ENDING_DEADLINE.
                    // Filtr powinien po prostu wyświetlać zadania z tą grupą.
                    if (t.getGroup() == Task.Group.NEAR_ENDING_DEADLINE && !t.isDone()) {
                        filtered.add(t);
                    }
                }
                return filtered;
            default:
                Task.Group targetGroup = null;
                // TODO: Upewnij się, że te stringi są identyczne jak w R.array.group_array_for_spinner
                switch (selectedGroupName) {
                    case "Do zrobienia":
                        targetGroup = Task.Group.TODO;
                        break;
                    case "Ważne":
                        targetGroup = Task.Group.IMPORTANT;
                        break;
                    case "Mniej ważne":
                        targetGroup = Task.Group.LESS_IMPORTANT;
                        break;
                    case "Na wolny czas":
                        targetGroup = Task.Group.IN_FREE_TIME;
                        break;
                    case "Zakończone":
                        targetGroup = Task.Group.FINISHED;
                        break;
                }


                if (targetGroup != null) {
                    for (Task t : allTasks) {
                        if (t.getGroup() == targetGroup) {

                            if (targetGroup == Task.Group.FINISHED) {
                                filtered.add(t);
                            } else {
                                if (!t.isDone() && t.getGroup() != Task.Group.ARCHIVED) {
                                    filtered.add(t);
                                }
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "filterTasksByGroup: Nieznana nazwa grupy: " + selectedGroupName);
                }
                return filtered;
        }
    }

    // Implementacja interfejsu z TaskAdapter
    @Override
    public void onTaskEditClicked(Task task) {
        Intent intent = new Intent(MainActivity.this, TaskDetailsActivity.class);
        intent.putExtra("taskToEdit", task);
        // Nie przekazujemy już pozycji, bo edycja i tak odświeży całą listę przez LiveData
        editTaskLauncher.launch(intent);
    }

    @Override
    public void onTaskDoneChanged(Task task, boolean isDone) {
        task.setDone(isDone);
        // Jeśli zadanie jest oznaczone jako wykonane, przenieś je do grupy "Zakończone"
        // Jeśli jest odznaczone z "Zakończone", przenieś je z powrotem do "Do zrobienia" (lub innej logiki)
        if (isDone) {
            if (task.getGroup() != Task.Group.ARCHIVED) { // Nie zmieniaj grupy zarchiwizowanej
                task.setGroup(Task.Group.FINISHED);
            }
        } else {
            if (task.getGroup() == Task.Group.FINISHED) {
                task.setGroup(Task.Group.TODO); // Lub inna domyślna, jeśli nie FINISHED
            }
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.taskDao().update(TaskEntity.fromTask(task));
            runOnUiThread(() -> {
                String status = isDone ? "wykonane" : "niewykonane";
                Toast.makeText(MainActivity.this, "Zadanie '" + task.getTitle() + "' oznaczone jako " + status, Toast.LENGTH_SHORT).show();
            });
        });
    }


}