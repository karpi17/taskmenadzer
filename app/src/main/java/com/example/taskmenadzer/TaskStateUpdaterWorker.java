package com.example.taskmenadzer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.taskmenadzer.model.AppDatabase;
import com.example.taskmenadzer.model.Task;
import com.example.taskmenadzer.model.TaskEntity;
import com.example.taskmenadzer.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TaskStateUpdaterWorker extends Worker {
    private static final String TAG = "TaskStateUpdaterWorker";
    private AppDatabase db;
    private final Context appContext;

    // ... (definicje kanałów powiadomień bez zmian) ...
    private static final String TASK_UPDATE_SUMMARY_CHANNEL_ID = "TASK_UPDATE_SUMMARY_CHANNEL";
    private static final String TASK_UPDATE_SUMMARY_CHANNEL_NAME = "Podsumowania aktualizacji zadań";
    private static final String TASK_UPDATE_SUMMARY_CHANNEL_DESC = "Powiadomienia o automatycznych zmianach stanu zadań";
    private static final int TASK_UPDATE_SUMMARY_NOTIFICATION_ID = 200;
    public static final String ACTION_MARK_AS_DONE = "com.example.taskmenadzer.ACTION_MARK_AS_DONE";
    public static final String EXTRA_TASK_ID = "com.example.taskmenadzer.EXTRA_TASK_ID";
    private static final String INDIVIDUAL_TASK_ALERT_CHANNEL_ID = "INDIVIDUAL_TASK_ALERT_CHANNEL";
    private static final String INDIVIDUAL_TASK_ALERT_CHANNEL_NAME = "Alerty o zadaniach";
    private static final String INDIVIDUAL_TASK_ALERT_CHANNEL_DESC = "Powiadomienia o zbliżającym się terminie dla konkretnych zadań.";
    public static final int INDIVIDUAL_TASK_ALERT_NOTIFICATION_ID_BASE = 30000;


    public TaskStateUpdaterWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.appContext = context.getApplicationContext();
        try {
            this.db = AppDatabase.getDatabase(this.appContext);
            Log.d(TAG, "Konstruktor: AppDatabase zainicjalizowana.");
            createNotificationChannels(this.appContext);
        } catch (Exception e) {
            Log.e(TAG, "BŁĄD w konstruktorze!", e);
        }
    }

    private void createNotificationChannels(Context context) {
        // Kanały są potrzebne od Oreo wzwyż
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            Log.e(TAG, "NotificationManager nie jest dostępny.");
            return;
        }

        NotificationChannel summaryChannel = new NotificationChannel(
                TASK_UPDATE_SUMMARY_CHANNEL_ID,
                TASK_UPDATE_SUMMARY_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT);
        summaryChannel.setDescription(TASK_UPDATE_SUMMARY_CHANNEL_DESC);
        notificationManager.createNotificationChannel(summaryChannel);

        NotificationChannel individualAlertChannel = new NotificationChannel(
                INDIVIDUAL_TASK_ALERT_CHANNEL_ID,
                INDIVIDUAL_TASK_ALERT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH); // Ważne alerty
        individualAlertChannel.setDescription(INDIVIDUAL_TASK_ALERT_CHANNEL_DESC);
        notificationManager.createNotificationChannel(individualAlertChannel);

        Log.d(TAG, "Kanały powiadomień utworzone/zaktualizowane.");
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(TAG, "Rozpoczynam pracę TaskStateUpdaterWorker.");

        if (db == null) {
            Log.e(TAG, "Baza danych (db) jest null! Kończę z niepowodzeniem.");
            return Result.failure();
        }

        // Zmienne do zliczania zmian dla powiadomienia podsumowującego
        int tasksArchivedCount = 0;
        int tasksMovedToNearDeadlineCount = 0;
        int tasksRestoredToTodoCount = 0;
        int tasksAutoDeletedCount = 0; // <<<< NOWA ZMIENNA
        boolean globalChangedSomething = false;

        try {
            SharedPreferences settingsPrefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE);
            boolean individualNotificationEnabled = settingsPrefs.getBoolean("individualNearDeadlineNotificationEnabled", false);
            int nearDeadlineDaysSetting = settingsPrefs.getInt("nearDeadlineDays", 3); // Domyślnie 3 dni
            boolean autoArchiveEnabled = settingsPrefs.getBoolean("autoArchiveEnabled", true); // Domyślnie włączone
            int autoArchiveOverdueDaysSetting = settingsPrefs.getInt("autoArchiveOverdueDays", 7); // Domyślnie 7 dni
            boolean summaryNotificationEnabled = settingsPrefs.getBoolean("taskUpdateSummaryNotificationEnabled", false);

            // NOWE USTAWIENIA
            boolean autoDeleteArchivedEnabled = settingsPrefs.getBoolean("autoDeleteArchivedEnabled", false);
            int autoDeleteArchivedAfterDays = settingsPrefs.getInt("autoDeleteArchivedAfterDays", 30); // Domyślnie 30 dni

            Log.d(TAG, "Ustawienia: IndywidualnePowiadomienia=" + individualNotificationEnabled +
                    ", PrógDni=" + nearDeadlineDaysSetting +
                    ", AutoArchiwizacja=" + autoArchiveEnabled +
                    ", DniDoArchiwizacji=" + autoArchiveOverdueDaysSetting +
                    ", AutoUsuwanieZarchiwizowanych=" + autoDeleteArchivedEnabled + // <<<< NOWY LOG
                    ", DniDoAutoUsunięcia=" + autoDeleteArchivedAfterDays + // <<<< NOWY LOG
                    ", Podsumowania=" + summaryNotificationEnabled);

            List<TaskEntity> allActiveEntities = db.taskDao().getAllActiveTasks(
                    Task.Group.FINISHED.name(), // Przekaż rzeczywistą nazwę grupy FINISHED
                    Task.Group.ARCHIVED.name()  // Przekaż rzeczywistą nazwę grupy ARCHIVED
            );

            if (allActiveEntities == null) {
                Log.w(TAG, "getAllActiveTasks() zwróciło null. Traktuję jako pustą listę.");
                allActiveEntities = new ArrayList<>();
            }

            Log.d(TAG, "Pobrano " + (allActiveEntities.isEmpty() ? "0" : allActiveEntities.size()) + " aktywnych zadań do przetworzenia.");


            Date now = new Date(); // Używamy java.util.Date dla spójności z Room i typem Deadline
            long currentTimeMillis = now.getTime(); // Dla timestampów

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(now);
            calendar.add(Calendar.DAY_OF_YEAR, nearDeadlineDaysSetting);
            Date nearDeadlineThreshold = calendar.getTime();

            // --- PĘTLA PRZETWARZANIA AKTYWNYCH ZADAŃ ---
            for (TaskEntity entity : allActiveEntities) {
                Task task = entity.toTask(); // Konwersja do obiektu biznesowego
                boolean taskProcessedByArchiveLogicThisIteration = false;
                boolean taskStateChangedThisIteration = false;
                String originalGroup = entity.group;
                boolean originalNotifiedState = entity.notifiedIndividuallyNearDeadline;

                Log.v(TAG, "Przetwarzanie zadania ID: " + entity.id + ", Tytuł: " + entity.title + ", Grupa: " + originalGroup);

                // 1. Logika Archiwizacji (jeśli włączona)
                if (autoArchiveEnabled && task.getDeadline() != null && !task.isDone() &&
                        (originalGroup == null || !originalGroup.equals(Task.Group.ARCHIVED.name()))) {
                    Calendar deadlineCal = Calendar.getInstance();
                    deadlineCal.setTime(task.getDeadline());
                    deadlineCal.add(Calendar.DAY_OF_YEAR, autoArchiveOverdueDaysSetting);

                    if (deadlineCal.getTime().before(now)) {
                        Log.i(TAG, "Archiwizowanie zadania (ID: " + task.getId() + ") - przeterminowane o >= " + autoArchiveOverdueDaysSetting + " dni.");
                        entity.group = Task.Group.ARCHIVED.name();
                        entity.notifiedIndividuallyNearDeadline = false;
                        entity.archivedAtTimestamp = currentTimeMillis; // <<<< USTAW TIMESTAMP ARCHIWIZACJI
                        taskStateChangedThisIteration = true;
                        taskProcessedByArchiveLogicThisIteration = true;
                    }
                }

                // Kontynuuj, jeśli zadanie nie zostało właśnie zarchiwizowane
                if (!taskProcessedByArchiveLogicThisIteration) {
                    if (task.getDeadline() != null && !task.isDone()) {
                        boolean isApproachingDeadline = task.getDeadline().after(now) && task.getDeadline().before(nearDeadlineThreshold);
                        boolean isOverdue = task.getDeadline().before(now);

                        if ((isApproachingDeadline || isOverdue) &&
                                (originalGroup == null || !originalGroup.equals(Task.Group.NEAR_ENDING_DEADLINE.name()))) {
                            Log.i(TAG, "Zmiana grupy na NEAR_ENDING_DEADLINE dla zadania (ID: " + task.getId() + ")");
                            entity.group = Task.Group.NEAR_ENDING_DEADLINE.name();
                            if (individualNotificationEnabled) { // << NOWY WARUNEK TUTAJ
                                entity.notifiedIndividuallyNearDeadline = true; // << USTAW OD RAZU
                            }
                            taskStateChangedThisIteration = true;
                        } else if (!(isApproachingDeadline || isOverdue) &&
                                originalGroup != null && originalGroup.equals(Task.Group.NEAR_ENDING_DEADLINE.name())) {
                            Log.i(TAG, "Przywracanie grupy na TODO dla zadania (ID: " + task.getId() + ")");
                            entity.group = Task.Group.TODO.name();
                            entity.notifiedIndividuallyNearDeadline = false; // Resetuj
                            entity.archivedAtTimestamp = null;
                            taskStateChangedThisIteration = true;
                        } else if (originalGroup != null && originalGroup.equals(Task.Group.NEAR_ENDING_DEADLINE.name())) { // Już było w NEAR_ENDING
                            if (individualNotificationEnabled && !entity.notifiedIndividuallyNearDeadline) { // I nie było jeszcze notyfikowane
                                Log.d(TAG, "Zadanie (ID: " + task.getId() + ") jest już w NEAR_ENDING_DEADLINE i nie było powiadomione, wysyłanie indywidualnego powiadomienia.");
                                entity.notifiedIndividuallyNearDeadline = true;
                                taskStateChangedThisIteration = true;
                            }
                        }
                    }
                }

                if (taskStateChangedThisIteration) {
                    try {
                        db.taskDao().update(entity);
                        Log.d(TAG, "Zaktualizowano w DB zadanie ID: " + entity.id + ". Nowa grupa: " + entity.group + ", NotifiedInd: " + entity.notifiedIndividuallyNearDeadline + ", ArchivedAt: " + entity.archivedAtTimestamp);
                        globalChangedSomething = true;

                        if (taskProcessedByArchiveLogicThisIteration) {
                            tasksArchivedCount++;
                        } else if (entity.group != null && entity.group.equals(Task.Group.NEAR_ENDING_DEADLINE.name()) && (originalGroup == null || !originalGroup.equals(Task.Group.NEAR_ENDING_DEADLINE.name()))) {
                            tasksMovedToNearDeadlineCount++;
                        } else if (entity.group != null && entity.group.equals(Task.Group.TODO.name()) && originalGroup != null && originalGroup.equals(Task.Group.NEAR_ENDING_DEADLINE.name())) {
                            tasksRestoredToTodoCount++;
                        }

                        if (individualNotificationEnabled &&
                                entity.group != null && entity.group.equals(Task.Group.NEAR_ENDING_DEADLINE.name()) &&
                                entity.notifiedIndividuallyNearDeadline && !originalNotifiedState) { // originalNotifiedState to stan PRZED zmianami w tej iteracji
                            sendIndividualNearDeadlineNotification(appContext, entity.toTask());
                        }
                    } catch (Exception e_db_update) {
                        Log.e(TAG, "BŁĄD aktualizacji zadania (ID: " + entity.id + ") w DB", e_db_update);
                    }
                }
            } // Koniec pętli for (allActiveEntities)

            // --- LOGIKA AUTOMATYCZNEGO USUWANIA ZARCHIWIZOWANYCH ZADAŃ ---
            if (autoDeleteArchivedEnabled && autoDeleteArchivedAfterDays > 0) {
                Log.i(TAG, "Rozpoczynanie procesu automatycznego usuwania starych zarchiwizowanych zadań (starszych niż " + autoDeleteArchivedAfterDays + " dni).");
                List<TaskEntity> archivedTasks = db.taskDao().getArchivedTasksWithTimestamp(); // Użyj nowej metody DAO

                if (archivedTasks == null) {
                    Log.w(TAG, "getArchivedTasksWithTimestamp() zwróciło null. Traktuję jako pustą listę.");
                    archivedTasks = new ArrayList<>();
                }

                if (!archivedTasks.isEmpty()) {
                    long deletionThresholdMillis = currentTimeMillis - TimeUnit.DAYS.toMillis(autoDeleteArchivedAfterDays);
                    Log.d(TAG, "Aktualny czas: " + currentTimeMillis + ", Próg usuwania (timestamp): " + deletionThresholdMillis);

                    for (TaskEntity archivedTask : archivedTasks) {
                        if (archivedTask.archivedAtTimestamp != null && archivedTask.archivedAtTimestamp < deletionThresholdMillis) {
                            try {
                                db.taskDao().delete(archivedTask);
                                tasksAutoDeletedCount++;
                                globalChangedSomething = true; // Zaszła zmiana w danych
                                Log.i(TAG, "Automatycznie usunięto zarchiwizowane zadanie ID: " + archivedTask.id +
                                        " (zostało zarchiwizowane: " + new Date(archivedTask.archivedAtTimestamp) + ")");
                            } catch (Exception e_db_delete) {
                                Log.e(TAG, "BŁĄD podczas automatycznego usuwania zarchiwizowanego zadania ID: " + archivedTask.id, e_db_delete);
                            }
                        } else {
                            if (archivedTask.archivedAtTimestamp == null) {
                                Log.w(TAG, "Zarchiwizowane zadanie ID: " + archivedTask.id + " nie ma ustawionego archivedAtTimestamp. Pomijam.");
                            } else {
                                Log.v(TAG, "Zarchiwizowane zadanie ID: " + archivedTask.id + " (zarchiwizowano: " + new Date(archivedTask.archivedAtTimestamp) + ") nie jest jeszcze wystarczająco stare do usunięcia.");
                            }
                        }
                    }
                } else {
                    Log.i(TAG, "Brak zarchiwizowanych zadań z timestampem do przetworzenia pod kątem usunięcia.");
                }
            } else if (autoDeleteArchivedEnabled) {
                Log.w(TAG, "Automatyczne usuwanie zarchiwizowanych jest włączone, ale 'autoDeleteArchivedAfterDays' jest ustawione na 0 lub mniej. Usuwanie nie zostanie wykonane.");
            } else {
                Log.i(TAG, "Automatyczne usuwanie zarchiwizowanych zadań jest wyłączone.");
            }

            // --- WYSYŁANIE POWIADOMIENIA PODSUMOWUJĄCEGO ---
            if (globalChangedSomething && summaryNotificationEnabled) {
                StringBuilder summaryTextBuilder = new StringBuilder();
                if (tasksArchivedCount > 0) {
                    summaryTextBuilder.append(tasksArchivedCount).append(tasksArchivedCount == 1 ? " zadanie zarchiwizowane" : " zarchiwizowanych zadań");
                }
                if (tasksMovedToNearDeadlineCount > 0) {
                    if (summaryTextBuilder.length() > 0) summaryTextBuilder.append(", ");
                    summaryTextBuilder.append(tasksMovedToNearDeadlineCount).append(tasksMovedToNearDeadlineCount == 1 ? " zadanie zbliża się do terminu" : " zadań zbliża się do terminu");
                }
                if (tasksRestoredToTodoCount > 0) {
                    if (summaryTextBuilder.length() > 0) summaryTextBuilder.append(", ");
                    summaryTextBuilder.append(tasksRestoredToTodoCount).append(tasksRestoredToTodoCount == 1 ? " zadanie przywrócono" : " zadań przywrócono");
                }
                if (tasksAutoDeletedCount > 0) { // <<<< DODANO DO PODSUMOWANIA
                    if (summaryTextBuilder.length() > 0) summaryTextBuilder.append(", ");
                    summaryTextBuilder.append(tasksAutoDeletedCount).append(tasksAutoDeletedCount == 1 ? " zadanie usunięto automatycznie" : " zadań usunięto automatycznie");
                }

                if (summaryTextBuilder.length() > 0) {
                    String notificationContent = summaryTextBuilder.toString().trim();
                    if (!notificationContent.isEmpty()) {
                        notificationContent = notificationContent.substring(0, 1).toUpperCase() + notificationContent.substring(1) + ".";
                        sendSummaryNotification(appContext, notificationContent);
                    }
                } else {
                    Log.d(TAG, "Podsumowanie włączone, ale brak zdarzeń do zaraportowania.");
                }
            } else if (globalChangedSomething) {
                Log.i(TAG, "Zakończono pracę: Zaszły zmiany w danych, ale podsumowanie wyłączone.");
            } else {
                Log.i(TAG, "Zakończono pracę: Brak zmian w danych.");
            }
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "KRYTYCZNY BŁĄD w TaskStateUpdaterWorker! Kończę z niepowodzeniem.", e);
            return Result.failure();
        }
    }

    private void sendIndividualNearDeadlineNotification(Context context, Task task) {
        // Sprawdzenie uprawnień przed próbą wysłania (dla API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                Log.w(TAG, "Powiadomienia są wyłączone dla aplikacji. Nie można wysłać indywidualnego alertu.");
                return;
            }
            // Można by też sprawdzić specifico dla kanału, ale globalne wystarczy na tym etapie
        }

        Log.d(TAG, "Przygotowanie do wysłania indywidualnego powiadomienia dla zadania ID: " + task.getId());
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("TASK_ID_TO_OPEN", task.getId());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                INDIVIDUAL_TASK_ALERT_NOTIFICATION_ID_BASE + task.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        Intent markAsDoneIntent = new Intent(context, TaskActionReceiver.class); // Użyj swojego BroadcastReceivera
        markAsDoneIntent.setAction(ACTION_MARK_AS_DONE);
        markAsDoneIntent.putExtra(EXTRA_TASK_ID, task.getId());
        // Dodajemy identyfikator zadania, aby receiver wiedział, które zadanie oznaczyć

        // Tworzymy unikalny requestCode dla PendingIntent akcji, aby uniknąć konfliktów
        // Możemy użyć ID zadania i dodać do niego stałą wartość, aby odróżnić od głównego PendingIntent
        int markAsDoneRequestCode = INDIVIDUAL_TASK_ALERT_NOTIFICATION_ID_BASE + task.getId() + 1;

        PendingIntent markAsDonePendingIntent = PendingIntent.getBroadcast(
                context,
                markAsDoneRequestCode,
                markAsDoneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        String notificationTitle = context.getString(R.string.individual_notification_title); // Użyj stringów z zasobów
        String deadlineString = "";
        if (task.getDeadline() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            deadlineString = " (Termin: " + sdf.format(task.getDeadline()) + ")";
        }
        String notificationContent = context.getString(R.string.individual_notification_content_with_deadline, task.getTitle(), deadlineString);

        int iconResId = R.drawable.ic_notification; // Użyj swojej ikony
        int markAsDoneIconResId = R.drawable.ic_check_circle; // Ikona dla przycisku "Oznacz jako wykonane"
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, INDIVIDUAL_TASK_ALERT_CHANNEL_ID)
                .setSmallIcon(iconResId)
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationContent))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .addAction(markAsDoneIconResId, context.getString(R.string.notification_action_mark_as_done), markAsDonePendingIntent); // <<<< DODANO AKCJĘ

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(INDIVIDUAL_TASK_ALERT_NOTIFICATION_ID_BASE + task.getId(), builder.build());
            Log.i(TAG, "Wysłano indywidualne powiadomienie z akcją 'Oznacz jako wykonane' dla zadania ID: " + task.getId());
        } catch (SecurityException e) {
            Log.e(TAG, "Brak uprawnienia POST_NOTIFICATIONS (API 33+) lub inny problem z bezpieczeństwem.", e);
        } catch (Exception e) {
            Log.e(TAG, "Nieoczekiwany błąd podczas wysyłania indywidualnego powiadomienia.", e);
        }
    }

    private void sendSummaryNotification(Context context, String contentText) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                Log.w(TAG, "Powiadomienia są wyłączone dla aplikacji. Nie można wysłać podsumowania.");
                return;
            }
        }

        Log.d(TAG, "Przygotowanie do wysłania powiadomienia podsumowującego.");

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                TASK_UPDATE_SUMMARY_NOTIFICATION_ID, // Użyj stałego ID dla tego typu powiadomienia
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String notificationTitle = context.getString(R.string.summary_notification_title); // Użyj stringów z zasobów
        int iconResId = R.drawable.ic_icon_notification; // Użyj swojej ikony dla podsumowania
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, TASK_UPDATE_SUMMARY_CHANNEL_ID)
                .setSmallIcon(iconResId)
                .setContentTitle(notificationTitle)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            // Upewnij się, że masz uprawnienie POST_NOTIFICATIONS w Manifeście dla API 33+
            notificationManager.notify(TASK_UPDATE_SUMMARY_NOTIFICATION_ID, builder.build());
            Log.i(TAG, "Wysłano powiadomienie podsumowujące: " + contentText);
        } catch (SecurityException e) {
            Log.e(TAG, "Brak uprawnienia POST_NOTIFICATIONS (API 33+) lub inny problem z bezpieczeństwem przy wysyłaniu podsumowania.", e);
        } catch (Exception e) {
            Log.e(TAG, "Nieoczekiwany błąd podczas wysyłania powiadomienia podsumowującego.", e);
        }
    }
}