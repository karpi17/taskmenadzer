package com.example.taskmenadzer.data.remote;

import com.example.taskmenadzer.model.Task; // Assuming Task model exists or will be moved/created
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface TaskApi {
    
    @GET("api/tasks")
    Call<List<Task>> getTasks(@Header("X-User-ID") String userId);

    @POST("api/tasks")
    Call<Task> createTask(@Body Task task, @Header("X-Firebase-UID") String firebaseUid);

    @PUT("api/tasks/{id}")
    Call<Task> updateTask(@Path("id") String id, @Body Task task);

    @DELETE("api/tasks/{id}")
    Call<Void> deleteTask(@Path("id") String id);
}
