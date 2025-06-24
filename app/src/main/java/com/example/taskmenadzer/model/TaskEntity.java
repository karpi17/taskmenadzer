package com.example.taskmenadzer.model;

import android.util.Log;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "tasks")
public class TaskEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String description;
    public long deadline; // timestamp
    public String group; // Przechowuje nazwę enuma jako String

    @ColumnInfo(name = "notified_near_deadline", defaultValue = "0")
    public boolean notifiedNearDeadline;

    @ColumnInfo(name = "archived_at_timestamp") // Nazwa kolumny w bazie
    public Long archivedAtTimestamp; // Znacznik czasu, kiedy zadanie zostało zarchiwizowane

    @ColumnInfo(name = "is_archived", defaultValue = "0")
    public boolean isArchived; // Rozważ, czy ta flaga jest nadal potrzebna, jeśli masz 'group' i 'archivedAtTimestamp'

    @ColumnInfo(name = "is_done", defaultValue = "0")
    public boolean isDone;

    @ColumnInfo(name = "notified_individually_near_deadline", defaultValue = "0")
    public boolean notifiedIndividuallyNearDeadline;

    public static TaskEntity fromTask(Task task) {
        if (task == null) return null;
        TaskEntity entity = new TaskEntity();
        entity.id = task.getId();
        entity.title = task.getTitle();
        entity.description = task.getDescription();
        entity.deadline = task.getDeadline() != null ? task.getDeadline().getTime() : 0;
        entity.group = task.getGroup() != null ? task.getGroup().name() : Task.Group.TODO.name();

        // Ustaw isArchived na podstawie grupy LUB dedykowanej flagi w Task, jeśli istnieje
        entity.isArchived = (task.getGroup() == Task.Group.ARCHIVED); // Jeśli 'isArchived' ma być synchronizowane z grupą
        // LUB entity.isArchived = task.isArchived(); // Jeśli Task ma swoje pole isArchived niezależne od grupy

        entity.isDone = task.isDone();
        entity.notifiedNearDeadline = task.isNotifiedNearDeadline(); // Rozważ, czy ta flaga jest nadal używana
        entity.notifiedIndividuallyNearDeadline = task.isNotifiedIndividuallyNearDeadline();

        entity.archivedAtTimestamp = task.getArchivedAtTimestamp(); // <<<< DODAJ TO

        return entity;
    }

    public Task toTask() {
        Task.Group taskDomainGroup = Task.Group.TODO; // Domyślna wartość
        if (this.group != null) {
            try {
                taskDomainGroup = Task.Group.valueOf(this.group);
            } catch (IllegalArgumentException e) {
                Log.e("TaskEntity", "Nieznana wartość grupy w bazie danych: " + this.group + " dla zadania ID: " + this.id + ". Użyto domyślnej.", e);
                // taskDomainGroup pozostaje TODO
            }
        } else {
            Log.w("TaskEntity", "Grupa była null dla zadania ID: " + this.id + ". Użyto domyślnej.");
            // taskDomainGroup pozostaje TODO
        }

        Task task = new Task(
                this.title,
                this.description,
                this.deadline > 0 ? new Date(this.deadline) : null,
                taskDomainGroup,
                this.isDone,
                this.notifiedNearDeadline, // <- najpierw notifiedNearDeadline
                this.notifiedIndividuallyNearDeadline, // <- potem notifiedIndividuallyNearDeadline
                this.archivedAtTimestamp
        );
        task.setId(this.id);
        task.setNotifiedIndividuallyNearDeadline(this.notifiedIndividuallyNearDeadline); // Jeśli jest setter
        task.setArchived(taskDomainGroup == Task.Group.ARCHIVED); // Ustaw na podstawie grupy
        task.setArchivedAtTimestamp(this.archivedAtTimestamp); // <<<< DODAJ TO

        // Jeśli masz flagę notifiedNearDeadline i nadal jest potrzebna w Task:
        // task.setNotifiedNearDeadline(this.notifiedNearDeadline);

        return task;
    }
}