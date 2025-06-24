package com.example.taskmenadzer.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class Task implements Serializable {
    private int id;
    private final String title;
    private final String description;
    private final Date deadline;
    private Group group;
    private boolean isDone;
    private final boolean notifiedNearDeadline;
    private boolean notifiedIndividuallyNearDeadline;
    private Long archivedAtTimestamp;

    public Task(String title, String description, Date deadline, Group group, boolean isDone,
                boolean notifiedNearDeadline, Boolean notifiedIndividuallyNearDeadlineFromConstructor, Long archivedAtTimestamp) {
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.group = group;
        this.isDone = isDone;
        this.notifiedNearDeadline = notifiedNearDeadline;
        this.notifiedIndividuallyNearDeadline = notifiedIndividuallyNearDeadlineFromConstructor;
        this.archivedAtTimestamp = archivedAtTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id &&
                isDone == task.isDone &&
                notifiedNearDeadline == task.notifiedNearDeadline &&
                notifiedIndividuallyNearDeadline == task.notifiedIndividuallyNearDeadline &&
                Objects.equals(title, task.title) &&
                Objects.equals(description, task.description) &&
                Objects.equals(deadline, task.deadline) &&
                group == task.group &&
                Objects.equals(archivedAtTimestamp, task.archivedAtTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, deadline, group, isDone,
                notifiedNearDeadline, notifiedIndividuallyNearDeadline, archivedAtTimestamp);
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Date getDeadline() {
        return deadline;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
        if (group != Group.ARCHIVED) {
            this.archivedAtTimestamp = null;
        }
    }

    public boolean isArchived() {
        return this.group == Group.ARCHIVED;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
    }

    public boolean isNotifiedNearDeadline() {
        return notifiedNearDeadline;
    }



    public boolean isNotifiedIndividuallyNearDeadline() {
        return notifiedIndividuallyNearDeadline;
    }

    public void setNotifiedIndividuallyNearDeadline(boolean notifiedIndividuallyNearDeadline) {
        this.notifiedIndividuallyNearDeadline = notifiedIndividuallyNearDeadline;
    }

    public Long getArchivedAtTimestamp() {
        return archivedAtTimestamp;
    }

    public void setArchivedAtTimestamp(Long archivedAtTimestamp) {
        this.archivedAtTimestamp = archivedAtTimestamp;
    }

    public void setArchived(boolean b) {
    }

    public enum Group {
        TODO, IMPORTANT, LESS_IMPORTANT, IN_FREE_TIME, FINISHED, NEAR_ENDING_DEADLINE, ARCHIVED
        // Rozważ dodanie TRASH, jeśli planujesz "miękkie usuwanie" w przyszłości
    }
}