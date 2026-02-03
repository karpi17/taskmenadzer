package com.example.taskmenadzer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.taskmenadzer.data.remote.RetrofitClient;
import com.example.taskmenadzer.data.remote.TaskApi;
import com.example.taskmenadzer.model.AppDatabase;
import com.example.taskmenadzer.model.Task;
import com.example.taskmenadzer.model.TaskEntity;
import com.example.taskmenadzer.notifications.NotificationWorker;
import com.example.taskmenadzer.ui.charts.ChartFactory;
import com.example.taskmenadzer.utils.ThemeManager;
import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskActionListener {
    private static final String TAG = "MainActivity";
    private AppDatabase db;
    private TaskAdapter adapter;
    private RecyclerView tasksRecyclerView;
    private ActivityResultLauncher<Intent> addTaskLauncher;
    private ActivityResultLauncher<Intent> editTaskLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getDatabase(getApplicationContext());

        // Setup UI Components
        setupRecyclerView();
        setupChart();
        setupButtons();
        setupLaunchers();

        // Load Data
        observeTasks();
    }

    private void setupRecyclerView() {
        tasksRecyclerView = findViewById(R.id.rvTasks); // Changed ID to match new XML
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(this);
        tasksRecyclerView.setAdapter(adapter);
    }

    private void setupChart() {
        LineChart lineChart = findViewById(R.id.lineChart);
        if (lineChart != null) {
            ChartFactory.configureLineChart(lineChart);
            // TODO: Populate with real data from DB
            lineChart.invalidate();
        }
    }

    private void setupButtons() {
        FloatingActionButton fabAddTask = findViewById(R.id.fabAddTask);
        fabAddTask.setOnClickListener(v -> {
            performHapticFeedback();
            Intent intent = new Intent(MainActivity.this, TaskDetailsActivity.class);
            addTaskLauncher.launch(intent);
        });

        findViewById(R.id.btnSync).setOnClickListener(v -> {
            performHapticFeedback();
            syncData();
        });
    }

    private void performHapticFeedback() {
        if (ThemeManager.isHapticEnabled(this)) {
            getWindow().getDecorView().performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    private void syncData() {
        Toast.makeText(this, "Syncing with Command Central...", Toast.LENGTH_SHORT).show();
        
        // Trigger immediate background sync via WorkManager
        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(com.example.taskmenadzer.work.SyncWorker.class)
                .build();
        WorkManager.getInstance(this).enqueue(syncRequest);
        
        // Placeholder for immediate UI feedback (simulated)
    }

    // ... (rest of methods)

    private void setupWorkers() {
        // Periodic Sync every 15 minutes
        androidx.work.PeriodicWorkRequest syncRequest = new androidx.work.PeriodicWorkRequest.Builder(
                com.example.taskmenadzer.work.SyncWorker.class, 
                15, java.util.concurrent.TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "BackgroundSync", 
                androidx.work.ExistingPeriodicWorkPolicy.KEEP, 
                syncRequest);
    }

    private void setupLaunchers() {
        addTaskLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Task newTask = (Task) result.getData().getSerializableExtra("newTask");
                if (newTask != null) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        db.taskDao().insert(TaskEntity.fromTask(newTask));
                        triggerNotification(newTask.getTitle());
                    });
                }
            }
        });

        editTaskLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Task updatedTask = (Task) result.getData().getSerializableExtra("newTask");
                if (updatedTask != null) {
                    AppDatabase.databaseWriteExecutor.execute(() -> 
                        db.taskDao().update(TaskEntity.fromTask(updatedTask))
                    );
                }
            }
        });
    }

    private void triggerNotification(String taskTitle) {
        // Trigger WorkManager for immediate notification (demonstration)
        // In reality, this would be scheduled based on due date
        // passing Data is skipped for brevity in this snippet
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .build(); // simplified
        WorkManager.getInstance(this).enqueue(request);
    }

    private void observeTasks() {
        db.taskDao().getAllTasksLiveData().observe(this, taskEntities -> {
            if (taskEntities != null) {
                List<Task> tasks = new ArrayList<>();
                for (TaskEntity entity : taskEntities) {
                    tasks.add(entity.toTask());
                }
                adapter.submitList(tasks);
            }
        });
    }

    @Override
    public void onTaskEditClicked(Task task) {
        Intent intent = new Intent(MainActivity.this, TaskDetailsActivity.class);
        intent.putExtra("taskToEdit", task);
        editTaskLauncher.launch(intent);
    }

    @Override
    public void onTaskDeleteClicked(Task task) {
        AppDatabase.databaseWriteExecutor.execute(() -> 
            db.taskDao().delete(TaskEntity.fromTask(task))
        );
    }

    @Override
    public void onTaskDoneChanged(Task task, boolean isDone) {
         task.setDone(isDone);
         AppDatabase.databaseWriteExecutor.execute(() -> 
            db.taskDao().update(TaskEntity.fromTask(task))
         );
    }
}