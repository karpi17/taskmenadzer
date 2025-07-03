package com.example.taskmenadzer; // UPEWNIJ SIĘ, ŻE TO TWÓJ POPRAWNY PAKIET

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private TextInputEditText etEmailLogin, etPasswordLogin;
    private TextInputLayout tilEmailLogin, tilPasswordLogin;
    private ProgressBar progressBarLogin;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.taskmenadzer.R.layout.activity_login);

        // Inicjalizacja Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword); // Użyj poprawnego R

        // ... listenery dla btnLogin i tvGoToRegister ...

        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        // Inicjalizacja widoków
        etEmailLogin = findViewById(R.id.etEmailLogin);
        etPasswordLogin = findViewById(R.id.etPasswordLogin);
        tilEmailLogin = findViewById(R.id.tilEmailLogin);
        tilPasswordLogin = findViewById(R.id.tilPasswordLogin);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvGoToRegister = findViewById(R.id.tvGoToRegister);
        progressBarLogin = findViewById(R.id.progressBarLogin);

        // Listener dla przycisku logowania
        btnLogin.setOnClickListener(v -> performLogin());

        // Listener dla przejścia do ekranu rejestracji
        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            // Nie kończymy LoginActivity, aby użytkownik mógł wrócić, jeśli się rozmyśli
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Sprawdź, czy użytkownik jest już zalogowany (np. przy ponownym otwarciu aplikacji)
        // i zaktualizuj UI odpowiednio.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Użytkownik jest już zalogowany, przejdź do MainActivity
            Toast.makeText(this, "Już zalogowany: " + currentUser.getEmail(), Toast.LENGTH_SHORT).show();
            navigateToMainActivity();
        }
    }
    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Resetowanie hasła");

        // Ustawienie layoutu dla dialogu (EditText do wpisania emaila)
        final View customLayout = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        builder.setView(customLayout);

        builder.setPositiveButton("Wyślij link", (dialog, which) -> {
            TextInputEditText etEmailReset = customLayout.findViewById(R.id.etEmailReset);
            String email = etEmailReset.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(LoginActivity.this, "Wprowadź adres e-mail", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(LoginActivity.this, "Wprowadź poprawny adres e-mail", Toast.LENGTH_SHORT).show();
                return;
            }

            sendPasswordResetEmail(email);
        });

        builder.setNegativeButton("Anuluj", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @OptIn(markerClass = UnstableApi.class)
    private void sendPasswordResetEmail(String email) {
        progressBarLogin.setVisibility(View.VISIBLE); // Pokaż progressBar
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressBarLogin.setVisibility(View.GONE); // Ukryj progressBar
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Link do resetowania hasła został wysłany na Twój e-mail.", Toast.LENGTH_LONG).show();
                    } else {
                        Log.w(TAG, "sendPasswordResetEmail:failure", task.getException());
                        // Obsługa błędów - np. jeśli użytkownik nie istnieje, Firebase też zwróci błąd
                        // FirebaseAuthInvalidUserException może tu wystąpić
                        try {
                            throw Objects.requireNonNull(task.getException());
                        } catch (FirebaseAuthInvalidUserException e) {
                            Toast.makeText(LoginActivity.this, "Nie znaleziono użytkownika z tym adresem e-mail.", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(LoginActivity.this, "Nie udało się wysłać linku: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
    private void performLogin() {
        String email = Objects.requireNonNull(etEmailLogin.getText()).toString().trim();
        String password = Objects.requireNonNull(etPasswordLogin.getText()).toString().trim();

        // Resetowanie poprzednich błędów
        tilEmailLogin.setError(null);
        tilPasswordLogin.setError(null);

        boolean isValid = true;

        if (TextUtils.isEmpty(email)) {
            tilEmailLogin.setError("Adres e-mail jest wymagany");
            etEmailLogin.requestFocus();
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmailLogin.setError("Wprowadź poprawny adres e-mail");
            etEmailLogin.requestFocus();
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            tilPasswordLogin.setError("Hasło jest wymagane");
            if (isValid) etPasswordLogin.requestFocus();
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        progressBarLogin.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<>() {
                    @OptIn(markerClass = UnstableApi.class)
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBarLogin.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(LoginActivity.this, "Zalogowano pomyślnie: " + (user != null ? user.getEmail() : ""), Toast.LENGTH_SHORT).show();
                            navigateToMainActivity();
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            // TUTAJ OBSŁUGA KONKRETNYCH BŁĘDÓW FIREBASE
                            try {
                                throw Objects.requireNonNull(task.getException());
                            } catch (FirebaseAuthInvalidUserException e) {
                                tilEmailLogin.setError("Nie znaleziono użytkownika z tym adresem e-mail.");
                                etEmailLogin.requestFocus();
                            } catch (
                                    FirebaseAuthInvalidCredentialsException e) {
                                tilEmailLogin.setError("Nieprawidłowy e-mail lub hasło. Sprawdź dane.");
                                etPasswordLogin.requestFocus();
                                 } catch (Exception e) {
                                Toast.makeText(LoginActivity.this, "Błąd logowania: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        // Wyczyść stos aktywności, aby użytkownik nie wrócił do ekranu logowania przyciskiem "wstecz"
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Zakończ LoginActivity
    }
}