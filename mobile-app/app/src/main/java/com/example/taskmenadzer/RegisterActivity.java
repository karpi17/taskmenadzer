package com.example.taskmenadzer; // Upewnij się, że to jest Twój poprawny pakiet

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
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.taskmenadzer.R;

import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private TextInputEditText etEmailRegister, etPasswordRegister, etConfirmPasswordRegister;
    private TextInputLayout tilEmailRegister, tilPasswordRegister, tilConfirmPasswordRegister;
    private Button btnRegister;
    private TextView tvGoToLogin;
    private ProgressBar progressBarRegister; // Dodajemy ProgressBar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // Upewnij się, że nazwa layoutu jest poprawna

        // Inicjalizacja Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Inicjalizacja widoków
        etEmailRegister = findViewById(R.id.etEmailRegister); // Użyj swoich ID
        etPasswordRegister = findViewById(R.id.etPasswordRegister); // Użyj swoich ID
        etConfirmPasswordRegister = findViewById(R.id.etConfirmPasswordRegister); // Użyj swoich ID

        tilEmailRegister = findViewById(R.id.tilEmailRegister);
        tilPasswordRegister = findViewById(R.id.tilPasswordRegister);
        tilConfirmPasswordRegister = findViewById(R.id.tilConfirmPasswordRegister);

        btnRegister = findViewById(R.id.btnRegister); // Użyj swoich ID
        tvGoToLogin = findViewById(R.id.tvGoToLogin); // Użyj swoich ID
        progressBarRegister = findViewById(R.id.progressBarRegister); // Pamiętaj dodać ProgressBar do XML

        // Listener dla przycisku rejestracji
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegistration();
            }
        });

        // Listener dla przejścia do ekranu logowania
        tvGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Zakończ RegisterActivity
            }
        });
    }

    private void performRegistration() {
        String email = Objects.requireNonNull(etEmailRegister.getText()).toString().trim();
        String password = Objects.requireNonNull(etPasswordRegister.getText()).toString().trim();
        String confirmPassword = Objects.requireNonNull(etConfirmPasswordRegister.getText()).toString().trim();
        tilEmailRegister.setError(null);
        tilPasswordRegister.setError(null);
        tilConfirmPasswordRegister.setError(null);
        // Podstawowa walidacja
        if (TextUtils.isEmpty(email)) {
            tilEmailRegister.setError("Adres e-mail jest wymagany");
            etEmailRegister.requestFocus();
            return;
        } else {
            tilEmailRegister.setError(null);
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmailRegister.setError("Wprowadź poprawny adres e-mail");
            etEmailRegister.requestFocus();
            return;
        } else {
            tilEmailRegister.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            tilPasswordRegister.setError("Hasło jest wymagane");
            etPasswordRegister.requestFocus();
            return;
        } else {
            tilPasswordRegister.setError(null);
        }

        if (password.length() < 6) {
            tilPasswordRegister.setError("Hasło musi mieć co najmniej 6 znaków");
            etPasswordRegister.requestFocus();
            return;
        } else {
            tilPasswordRegister.setError(null);
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPasswordRegister.setError("Potwierdzenie hasła jest wymagane");
            etConfirmPasswordRegister.requestFocus();
            return;
        } else {
            tilConfirmPasswordRegister.setError(null);
        }

        if (!password.equals(confirmPassword)) {
            tilConfirmPasswordRegister.setError("Hasła nie są zgodne");
            etConfirmPasswordRegister.requestFocus();
            return;
        } else {
            tilConfirmPasswordRegister.setError(null);
        }

        progressBarRegister.setVisibility(View.VISIBLE); // Pokaż ProgressBar

        // Rejestracja użytkownika w Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBarRegister.setVisibility(View.GONE); // Ukryj ProgressBar
                        if (task.isSuccessful()) {
                            // Rejestracja pomyślna
                            Toast.makeText(RegisterActivity.this, "Rejestracja pomyślna.", Toast.LENGTH_SHORT).show();
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user != null) {
                                user.sendEmailVerification()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(RegisterActivity.this, "E-mail weryfikacyjny wysłany.", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                            }


                            // Przejdź do ekranu logowania lub bezpośrednio do aplikacji
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            // Aby wyczyścić stos aktywności, aby użytkownik nie wrócił do rejestracji
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish(); // Zakończ RegisterActivity
                        } else {
                            // Jeśli rejestracja się nie powiedzie, wyświetl komunikat użytkownikowi.
                            Toast.makeText(RegisterActivity.this, "Rejestracja nie powiodła się: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}