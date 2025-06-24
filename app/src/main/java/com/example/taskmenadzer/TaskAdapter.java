package com.example.taskmenadzer;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskmenadzer.model.Task;
import com.google.android.material.color.MaterialColors;
import com.taskmenadzer.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskViewHolder> {

    private static final String TAG = "TaskAdapter";
    private static final DiffUtil.ItemCallback<Task> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.equals(newItem); // Upewnij się, że Task.equals() jest dobrze zaimplementowane
        }
    };
    private final OnTaskActionListener listener;

    public TaskAdapter(OnTaskActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false); // Użyj R z Twojego pakietu
        return new TaskViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = getItem(position);
        holder.bind(task);
    }

    public interface OnTaskActionListener {
        void onTaskEditClicked(Task task);

        void onTaskDoneChanged(Task task, boolean isDone);

        void onTaskDeleteClicked(Task task);
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, deadline;
        Button editButton, deleteButton;
        TextView statusDone;
        CheckBox taskDoneCheckBox;

        TaskViewHolder(View itemView, final OnTaskActionListener listener) {
            super(itemView);
            title = itemView.findViewById(R.id.taskTitle);
            description = itemView.findViewById(R.id.taskDescription);
            deadline = itemView.findViewById(R.id.taskDeadline);
            editButton = itemView.findViewById(R.id.editTaskButton);
            deleteButton = itemView.findViewById(R.id.deleteTaskButton);
            statusDone = itemView.findViewById(R.id.statusDone);
            taskDoneCheckBox = itemView.findViewById(R.id.checkbox_done);

            editButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task task = getItemFromAdapter(position);
                    if (task != null && listener != null) {
                        listener.onTaskEditClicked(task);
                    }
                }
            });

            deleteButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task task = getItemFromAdapter(position);
                    if (task != null && listener != null) {
                        new AlertDialog.Builder(itemView.getContext()).setTitle(itemView.getContext().getString(R.string.confirm_delete_title)) // Użyj string resource
                                .setMessage(itemView.getContext().getString(R.string.confirm_delete_message)) // Użyj string resource
                                .setPositiveButton(itemView.getContext().getString(R.string.delete), (dialog, which) -> listener.onTaskDeleteClicked(task)).setNegativeButton(itemView.getContext().getString(R.string.cancel), null).show();
                    }
                }
            });

            if (taskDoneCheckBox != null) {
                taskDoneCheckBox.setOnClickListener(v -> {
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Task task = getItemFromAdapter(position);
                        if (task != null && listener != null) {
                            listener.onTaskDoneChanged(task, taskDoneCheckBox.isChecked());
                        }
                    }
                });
            }
        }

        private Task getItemFromAdapter(int position) {
            if (getBindingAdapter() instanceof TaskAdapter && position >= 0 && position < ((TaskAdapter) getBindingAdapter()).getCurrentList().size()) {
                return ((TaskAdapter) getBindingAdapter()).getItem(position);
            }
            return null;
        }

        void bind(Task task) {
            if (task == null) {
                Log.w(TAG, "Próba bindowania null taska w ViewHolderze. Ustawiam puste/domyślne wartości.");
                title.setText("");
                description.setText("");
                if (deadline != null) deadline.setVisibility(View.GONE);
                if (statusDone != null) statusDone.setVisibility(View.GONE);
                if (taskDoneCheckBox != null) taskDoneCheckBox.setVisibility(View.GONE);
                editButton.setEnabled(false);
                deleteButton.setEnabled(false);
                // Zresetuj flagi (np. przekreślenie)
                title.setPaintFlags(title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                // Można też zresetować kolory do domyślnych, ale ListAdapter powinien to załatwić przy recyklingu
                return;
            }

            Context context = itemView.getContext();

            Log.d(TAG, "Binding task: " + task.getTitle() + ", isDone: " + task.isDone() + ", group: " + task.getGroup() + (task.getDeadline() != null ? ", deadline: " + task.getDeadline() : ", no deadline"));

            title.setText(task.getTitle());
            description.setText(task.getDescription());

            boolean isDeadlineVisible = false;
            if (this.deadline != null && task.getDeadline() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                this.deadline.setText(sdf.format(task.getDeadline()));
                this.deadline.setVisibility(View.VISIBLE);
                isDeadlineVisible = true;
            } else if (this.deadline != null) {
                this.deadline.setVisibility(View.GONE);
            }

            boolean isOverdueAndNotDone = false;
            boolean isDueTodayAndNotDone = false;
            if (isDeadlineVisible && !task.isDone()) {
                Calendar todayCal = Calendar.getInstance();
                todayCal.set(Calendar.HOUR_OF_DAY, 0);
                todayCal.set(Calendar.MINUTE, 0);
                todayCal.set(Calendar.SECOND, 0);
                todayCal.set(Calendar.MILLISECOND, 0);

                Calendar deadlineCal = Calendar.getInstance();
                deadlineCal.setTime(task.getDeadline());
                deadlineCal.set(Calendar.HOUR_OF_DAY, 0);
                deadlineCal.set(Calendar.MINUTE, 0);
                deadlineCal.set(Calendar.SECOND, 0);
                deadlineCal.set(Calendar.MILLISECOND, 0);

                if (deadlineCal.before(todayCal)) {
                    isOverdueAndNotDone = true;
                } else if (deadlineCal.equals(todayCal)) { // Sprawdzenie, czy termin jest dokładnie dzisiaj
                    isDueTodayAndNotDone = true;
                }
            }
            Log.d(TAG, "Task: " + task.getTitle() + ", isOverdueAndNotDone: " + isOverdueAndNotDone);


            // Ustawianie stylów
            if (task.isDone() && task.getGroup() != Task.Group.ARCHIVED) {
                // ZADANIE ZROBIONE (i nie zarchiwizowane)
                title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                int doneTextColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY);
                title.setTextColor(doneTextColor);
                description.setTextColor(doneTextColor);

                if (isDeadlineVisible && this.deadline != null) {
                    Log.d(TAG, "Task: " + task.getTitle() + " (DONE) - Setting deadline color to doneTextColor");
                    this.deadline.setTextColor(doneTextColor);
                }

                if (taskDoneCheckBox != null) taskDoneCheckBox.setVisibility(View.GONE);
                if (statusDone != null) {
                    statusDone.setText(context.getString(R.string.status_done_simple));
                    statusDone.setVisibility(View.VISIBLE);
                    statusDone.setTextColor(doneTextColor);
                }

            } else {
                // ZADANIE NIEZROBIONE LUB ZARCHIWIZOWANE
                title.setPaintFlags(title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                title.setTextColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK));
                description.setTextColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.DKGRAY));

                if (isDeadlineVisible && this.deadline != null) {
                    // Sprawdź, czy zadanie NIE jest zarchiwizowane PRZED kolorowaniem specjalnym
                    if (task.getGroup() != Task.Group.ARCHIVED && isOverdueAndNotDone) {
                        Log.d(TAG, "Task: " + task.getTitle() + " (NOT ARCHIVED, OVERDUE & NOT DONE) - Setting deadline color to RED");
                        this.deadline.setTextColor(ContextCompat.getColor(context, R.color.app_color_error));
                    } else if (task.getGroup() != Task.Group.ARCHIVED && isDueTodayAndNotDone) {
                        Log.d(TAG, "Task: " + task.getTitle() + " (NOT ARCHIVED, DUE TODAY & NOT DONE) - Setting deadline color to ORANGE");
                        this.deadline.setTextColor(ContextCompat.getColor(context, R.color.orange_deadline));
                    } else {
                        // Dla zarchiwizowanych lub jeśli żaden specjalny warunek nie jest spełniony
                        Log.d(TAG, "Task: " + task.getTitle() + " (ARCHIVED or NO SPECIAL DEADLINE STATE) - Setting deadline color to default");
                        this.deadline.setTextColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.DKGRAY));
                    }
                }

                if (task.getGroup() == Task.Group.ARCHIVED) {
                    if (taskDoneCheckBox != null) taskDoneCheckBox.setVisibility(View.GONE);
                    if (statusDone != null) {
                        statusDone.setVisibility(View.VISIBLE);
                        statusDone.setText(task.isDone() ? context.getString(R.string.status_done_archived) : context.getString(R.string.status_not_done_archived));
                        statusDone.setTextColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY));
                    }
                } else { // Nie zrobione i nie zarchiwizowane
                    if (taskDoneCheckBox != null) {
                        taskDoneCheckBox.setVisibility(View.VISIBLE);
                        taskDoneCheckBox.setOnCheckedChangeListener(null); // Zapobiegaj wywołaniu listenera podczas bindowania
                        taskDoneCheckBox.setChecked(task.isDone());      // Stan powinien być false, jeśli tu trafiliśmy
                        // Listener jest ustawiony w konstruktorze TaskViewHolder, więc nie trzeba go tu ponownie ustawiać
                    }
                    if (statusDone != null) {
                        statusDone.setVisibility(View.GONE);
                    }
                }
            }

            editButton.setEnabled(true);
            deleteButton.setEnabled(true);
        }
    }
}