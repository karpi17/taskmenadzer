package com.example.taskmenadzer.work;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.taskmenadzer.data.remote.RetrofitClient;
import com.example.taskmenadzer.data.remote.TaskApi;
import com.example.taskmenadzer.model.AppDatabase;
import com.example.taskmenadzer.model.Task;
import com.example.taskmenadzer.model.TaskEntity;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting Background Sync...");
        
        // In a real app, you'd get the current User ID from shared prefs or auth manager
        String userId = "test-user-id"; 
        
        TaskApi api = RetrofitClient.getClient();
        try {
            Response<List<Task>> response = api.getTasks(userId).execute(); // Synchronous call
            if (response.isSuccessful() && response.body() != null) {
                AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
                for (Task task : response.body()) {
                    // Check if exists by remoteId
                    TaskEntity existing = null; // db.taskDao().findByRemoteId(task.getId()); // Ideal
                    // Fallback: This requires TaskDao update. For this Step, we map and insert.
                    TaskEntity entity = TaskEntity.fromTask(task);
                    entity.remoteId = task.getRemoteId(); // Ensure mapped
                    db.taskDao().insert(entity); 
                }
                Log.d(TAG, "Sync Successful. Tasks updated: " + response.body().size());
                return Result.success();
            } else {
                Log.e(TAG, "Sync Failed with code: " + response.code());
                return Result.retry();
            }
        } catch (IOException e) {
            Log.e(TAG, "Sync Error", e);
            return Result.retry();
        }
    }
}
