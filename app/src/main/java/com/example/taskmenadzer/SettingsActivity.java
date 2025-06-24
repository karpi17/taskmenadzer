package com.example.taskmenadzer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

// Upewnij się, że ten import R jest poprawny dla Twojego projektu
import com.taskmenadzer.R; // Zakładam, że ten import jest poprawny

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
    // Istniejące pola
    private TextInputEditText daysEditText;
    private Spinner defaultCategorySpinner;
    private SharedPreferences prefs;
    private SwitchMaterial switchAutoArchive;
    private TextInputLayout autoArchiveDaysInputLayout; // << Zakładam, że masz to pole i odpowiada R.id.autoArchiveDaysInputLayout
    private TextInputEditText editTextAutoArchiveDays;
    private SwitchMaterial switchTaskUpdateSummaryNotification;
    private SwitchMaterial switchIndividualNearDeadlineNotification;
    private SwitchMaterial darkModeSwitch;

    // NOWE POLA dla automatycznego usuwania
    private SwitchMaterial switchAutoDeleteArchived;
    private TextInputLayout autoDeleteArchivedDaysInputLayout; // To jest pole dla auto-usuwania, które już miałeś
    private TextInputEditText editTextAutoDeleteArchivedDays;

    private static final String TAG = "SettingsActivity";

    // Domyślne wartości ustawień
    private static final int DEFAULT_NEAR_DEADLINE_DAYS = 3;
    private static final int DEFAULT_AUTO_ARCHIVE_OVERDUE_DAYS = 7;
    private static final boolean DEFAULT_AUTO_ARCHIVE_ENABLED = true; // Zmieniono na true, jeśli tak ma być domyślnie
    private static final boolean DEFAULT_TASK_SUMMARY_NOTIFICATION_ENABLED = false;
    private static final boolean DEFAULT_INDIVIDUAL_NOTIFICATION_ENABLED = false;
    private static final boolean DEFAULT_DARK_MODE_ENABLED = false;
    private static final int DEFAULT_CATEGORY_POSITION = 0;
    private static final boolean DEFAULT_AUTO_DELETE_ARCHIVED_ENABLED = false;
    private static final int DEFAULT_AUTO_DELETE_ARCHIVED_DAYS = 30;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        initializeViews();
        setupDefaultCategorySpinner();
        loadSettings();
        setupListeners();
    }

    private void initializeViews() {
        switchTaskUpdateSummaryNotification = findViewById(R.id.switchTaskUpdateSummaryNotification);
        switchIndividualNearDeadlineNotification = findViewById(R.id.switchIndividualNearDeadlineNotification);
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        daysEditText = findViewById(R.id.daysEditText);
        defaultCategorySpinner = findViewById(R.id.defaultCategorySpinner);
        Button saveButton = findViewById(R.id.saveButton);
        Button backButton = findViewById(R.id.backButton);

        switchAutoArchive = findViewById(R.id.switchAutoArchive);

        autoArchiveDaysInputLayout = findViewById(R.id.autoArchiveDaysInputLayout); // << Inicjalizacja pola dla autoarchiwizacji
        editTextAutoArchiveDays = findViewById(R.id.editTextAutoArchiveDays);


        // NOWE WIDOKI (dla automatycznego usuwania)
        switchAutoDeleteArchived = findViewById(R.id.switchAutoDeleteArchived);
        autoDeleteArchivedDaysInputLayout = findViewById(R.id.autoDeleteArchivedDaysInputLayout); // Pole dla auto-usuwania
        editTextAutoDeleteArchivedDays = findViewById(R.id.editTextAutoDeleteArchivedDays);

        saveButton.setOnClickListener(v -> saveSettingsAndFinish());
        backButton.setOnClickListener(v -> {
            Log.d(TAG, "Przycisk Wstecz kliknięty.");
            finish();
        });
    }

    private void setupDefaultCategorySpinner() {
        String[] categories = {"Wszystkie", "Do zrobienia", "Ważne", "Mniej ważne", "Na wolny czas", "Zakończone", "Archiwum", "Bliski termin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        defaultCategorySpinner.setAdapter(adapter);
    }

    private void loadSettings() {
        switchTaskUpdateSummaryNotification.setChecked(prefs.getBoolean("taskUpdateSummaryNotificationEnabled", DEFAULT_TASK_SUMMARY_NOTIFICATION_ENABLED));
        switchIndividualNearDeadlineNotification.setChecked(prefs.getBoolean("individualNearDeadlineNotificationEnabled", DEFAULT_INDIVIDUAL_NOTIFICATION_ENABLED));
        daysEditText.setText(String.valueOf(prefs.getInt("nearDeadlineDays", DEFAULT_NEAR_DEADLINE_DAYS)));
        darkModeSwitch.setChecked(prefs.getBoolean("darkMode", DEFAULT_DARK_MODE_ENABLED));
        defaultCategorySpinner.setSelection(prefs.getInt("defaultCategory", DEFAULT_CATEGORY_POSITION));

        // Ustawienia autoarchiwizacji
        boolean autoArchiveEnabled = prefs.getBoolean("autoArchiveEnabled", DEFAULT_AUTO_ARCHIVE_ENABLED);
        switchAutoArchive.setChecked(autoArchiveEnabled);
        editTextAutoArchiveDays.setText(String.valueOf(prefs.getInt("autoArchiveOverdueDays", DEFAULT_AUTO_ARCHIVE_OVERDUE_DAYS)));
        // Upewnij się, że używasz poprawnego pola `autoArchiveDaysInputLayout` dla autoarchiwizacji
        if (this.autoArchiveDaysInputLayout != null) { // Sprawdzenie, czy pole jest zainicjalizowane
            this.autoArchiveDaysInputLayout.setVisibility(autoArchiveEnabled ? View.VISIBLE : View.GONE);
        } else {

            findViewById(R.id.autoArchiveDaysInputLayout).setVisibility(autoArchiveEnabled ? View.VISIBLE : View.GONE);
        }


        // WCZYTYWANIE NOWYCH USTAWIEŃ (automatyczne usuwanie)
        boolean autoDeleteEnabled = prefs.getBoolean("autoDeleteArchivedEnabled", DEFAULT_AUTO_DELETE_ARCHIVED_ENABLED);
        switchAutoDeleteArchived.setChecked(autoDeleteEnabled);
        editTextAutoDeleteArchivedDays.setText(String.valueOf(prefs.getInt("autoDeleteArchivedAfterDays", DEFAULT_AUTO_DELETE_ARCHIVED_DAYS)));
        this.autoDeleteArchivedDaysInputLayout.setVisibility(autoDeleteEnabled ? View.VISIBLE : View.GONE);
    }

    private void setupListeners() {
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            prefs.edit().putBoolean("darkMode", isChecked).apply(); // Natychmiastowy zapis
            Log.d(TAG, "Dark mode ustawiony na: " + isChecked);
        });

        // Listener dla przełącznika autoarchiwizacji
        switchAutoArchive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("autoArchiveEnabled", isChecked).apply(); // Natychmiastowy zapis
            Log.d(TAG, "Auto archiwizacja ustawiona na: " + isChecked);
            // Upewnij się, że używasz poprawnego pola `autoArchiveDaysInputLayout` dla autoarchiwizacji
            if (this.autoArchiveDaysInputLayout != null) {
                this.autoArchiveDaysInputLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            } else {
                // Alternatywa, jeśli nie masz globalnego pola dla tego konkretnego layoutu
                findViewById(R.id.autoArchiveDaysInputLayout).setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        // LISTENER DLA NOWEGO PRZEŁĄCZNIKA AUTOMATYCZNEGO USUWANIA
        switchAutoDeleteArchived.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("autoDeleteArchivedEnabled", isChecked).apply(); // Natychmiastowy zapis
            Log.d(TAG, "Auto usuwanie zarchiwizowanych ustawione na: " + isChecked);
            // Używasz poprawnego pola `autoDeleteArchivedDaysInputLayout` dla auto-usuwania
            this.autoDeleteArchivedDaysInputLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

    }

    private void saveSettingsAndFinish() {
        SharedPreferences.Editor editor = prefs.edit();

        try {
            editor.putInt("nearDeadlineDays", Integer.parseInt(Objects.requireNonNull(daysEditText.getText()).toString()));
        } catch (NumberFormatException e) {
            Log.w(TAG, "Nie udało się sparsować 'nearDeadlineDays'. Użyto domyślnej: " + DEFAULT_NEAR_DEADLINE_DAYS, e);
            editor.putInt("nearDeadlineDays", DEFAULT_NEAR_DEADLINE_DAYS);
        }

        editor.putInt("defaultCategory", defaultCategorySpinner.getSelectedItemPosition());

        editor.putBoolean("darkMode", darkModeSwitch.isChecked());
        editor.putBoolean("autoArchiveEnabled", switchAutoArchive.isChecked());

        try {
            String autoArchiveDaysStr = Objects.requireNonNull(editTextAutoArchiveDays.getText()).toString();
            int autoArchiveDaysToSave = autoArchiveDaysStr.isEmpty() ? DEFAULT_AUTO_ARCHIVE_OVERDUE_DAYS : Integer.parseInt(autoArchiveDaysStr);
            if (autoArchiveDaysToSave <= 0 && switchAutoArchive.isChecked()) { // Jeśli archiwizacja włączona, dni muszą być > 0
                Log.w(TAG, "'autoArchiveOverdueDays' musi być większe od 0, gdy autoarchiwizacja jest włączona. Ustawiono na domyślną: " + DEFAULT_AUTO_ARCHIVE_OVERDUE_DAYS);
                autoArchiveDaysToSave = DEFAULT_AUTO_ARCHIVE_OVERDUE_DAYS;
            } else if (autoArchiveDaysToSave < 0) { // Ogólnie nie powinno być ujemne
                autoArchiveDaysToSave = 0;
            }
            editor.putInt("autoArchiveOverdueDays", autoArchiveDaysToSave);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Nie udało się sparsować 'autoArchiveOverdueDays'. Użyto domyślnej: " + DEFAULT_AUTO_ARCHIVE_OVERDUE_DAYS, e);
            editor.putInt("autoArchiveOverdueDays", DEFAULT_AUTO_ARCHIVE_OVERDUE_DAYS);
        }

        editor.putBoolean("taskUpdateSummaryNotificationEnabled", switchTaskUpdateSummaryNotification.isChecked());
        editor.putBoolean("individualNearDeadlineNotificationEnabled", switchIndividualNearDeadlineNotification.isChecked());

        // ZAPISYWANIE NOWYCH USTAWIEŃ (auto-usuwanie)
        boolean autoDeleteEnabled = switchAutoDeleteArchived.isChecked(); // Już zapisane w listenerze
        editor.putBoolean("autoDeleteArchivedEnabled", autoDeleteEnabled); // Potwierdzenie

        if (autoDeleteEnabled) {
            try {
                String autoDeleteDaysStr = Objects.requireNonNull(editTextAutoDeleteArchivedDays.getText()).toString();
                // Jeśli pole jest puste, a opcja włączona, użyj domyślnej
                int autoDeleteDaysToSave = autoDeleteDaysStr.isEmpty() ? DEFAULT_AUTO_DELETE_ARCHIVED_DAYS : Integer.parseInt(autoDeleteDaysStr);
                if (autoDeleteDaysToSave <= 0) {
                    Log.w(TAG, "'autoDeleteArchivedAfterDays' musi być większe od 0, gdy opcja jest włączona. Ustawiono na domyślną: " + DEFAULT_AUTO_DELETE_ARCHIVED_DAYS);
                    autoDeleteDaysToSave = DEFAULT_AUTO_DELETE_ARCHIVED_DAYS;
                }
                editor.putInt("autoDeleteArchivedAfterDays", autoDeleteDaysToSave);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Nie udało się sparsować 'autoDeleteArchivedAfterDays'. Użyto domyślnej: " + DEFAULT_AUTO_DELETE_ARCHIVED_DAYS, e);
                editor.putInt("autoDeleteArchivedAfterDays", DEFAULT_AUTO_DELETE_ARCHIVED_DAYS);
            }
        } else {
            // Jeśli opcja jest wyłączona, zapisz ostatnio używaną (lub domyślną) wartość dni
            editor.putInt("autoDeleteArchivedAfterDays", prefs.getInt("autoDeleteArchivedAfterDays", DEFAULT_AUTO_DELETE_ARCHIVED_DAYS));
        }

        editor.apply();

        Log.i(TAG, "Ustawienia zapisane.");
        setResult(RESULT_OK);
        finish();
    }
}