package com.example.taskmenadzer;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.taskmenadzer.model.AppDatabase;
import com.example.taskmenadzer.model.Task;
import com.example.taskmenadzer.model.TaskEntity;

public class TaskActionReceiver extends BroadcastReceiver {

    // Przeniesione/zdefiniowane stałe (lub odwołuj się do nich z TaskStateUpdaterWorker, jeśli tam są publiczne)
    public static final String ACTION_MARK_AS_DONE = "com.example.taskmenadzer.ACTION_MARK_AS_DONE";
    public static final String EXTRA_TASK_ID = "com.example.taskmenadzer.EXTRA_TASK_ID";

    private static final String TAG = "TaskActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "TaskActionReceiver - onReceive triggered");

        if (intent == null || intent.getAction() == null) {
            Log.e(TAG, "Intent lub akcja jest null.");
            return;
        }

        String action = intent.getAction();
        // Użyj własnej stałej EXTRA_TASK_ID
        int taskId = intent.getIntExtra(EXTRA_TASK_ID, -1);

        if (taskId == -1) {
            Log.e(TAG, "Nieprawidłowe ID zadania.");
            return;
        }

        // Użyj własnej stałej ACTION_MARK_AS_DONE
        if (ACTION_MARK_AS_DONE.equals(action)) {
            Log.d(TAG, "Akcja: Oznacz jako wykonane dla zadania ID: " + taskId);

            AppDatabase db = AppDatabase.getDatabase(context.getApplicationContext());

            AppDatabase.databaseWriteExecutor.execute(() -> {
                TaskEntity taskEntity = db.taskDao().findById(taskId);
                if (taskEntity != null) {
                    taskEntity.isDone = true;
                    taskEntity.group = Task.Group.FINISHED.name(); // Lub inną odpowiednią grupę dla wykonanych zadań

                    try {
                        db.taskDao().update(taskEntity);
                        Log.d(TAG, "Zadanie ID: " + taskId + " oznaczone jako wykonane.");

                        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        if (notificationManager != null) {
                            // Użyj tego samego ID, co przy tworzeniu powiadomienia w TaskStateUpdaterWorker
                            // Załóżmy, że TaskStateUpdaterWorker.INDIVIDUAL_TASK_ALERT_NOTIFICATION_ID_BASE jest public static final
                            int notificationIdToCancel = TaskStateUpdaterWorker.INDIVIDUAL_TASK_ALERT_NOTIFICATION_ID_BASE + taskId;
                            notificationManager.cancel(notificationIdToCancel);
                            Log.d(TAG, "Powiadomienie (ID: " + notificationIdToCancel + ") dla zadania (Task ID: " + taskId + ") anulowane.");
                        } else {
                            Log.e(TAG, "Nie można uzyskać NotificationManager do anulowania powiadomienia.");
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Błąd podczas aktualizacji zadania lub anulowania powiadomienia: " + e.getMessage(), e);
                    }
                } else {
                    Log.e(TAG, "Nie znaleziono zadania o ID: " + taskId + " w bazie danych.");
                }
            });
        } else {
            Log.d(TAG, "Odebrano nieznaną akcję: " + action);
        }
    }
}