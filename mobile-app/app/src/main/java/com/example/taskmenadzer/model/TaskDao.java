package com.example.taskmenadzer.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY deadline ASC")
        // Przykładowe sortowanie
    LiveData<List<TaskEntity>> getAllTasksLiveData(); // Zamiast List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1") // Dodaj LIMIT 1 dla pewności
    TaskEntity findById(int taskId);

    @Query("SELECT * FROM tasks WHERE `group` = 'ARCHIVED' AND archived_at_timestamp IS NOT NULL")
    List<TaskEntity> getArchivedTasksWithTimestamp();
    @Insert
    void insert(TaskEntity task);

    @Update
    void update(TaskEntity task);


    @Delete
    void delete(TaskEntity taskEntity);

    @Query("SELECT * FROM tasks WHERE (`group` IS NULL OR (`group` != :groupFinished AND `group` != :groupArchived)) AND is_done = 0 ORDER BY deadline ASC")
    List<TaskEntity> getAllActiveTasks(String groupFinished, String groupArchived);

    // W TaskDao.java
    @Query("SELECT * FROM tasks WHERE `group` = :groupName AND notified_near_deadline = 0 AND is_done = 0 ORDER BY deadline ASC")
    List<TaskEntity> getTasksInGroupNotNotified(String groupName);
}