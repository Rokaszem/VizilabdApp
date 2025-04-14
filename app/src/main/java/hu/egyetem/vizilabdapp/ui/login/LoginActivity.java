package hu.egyetem.vizilabdapp.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import hu.egyetem.vizilabdapp.ui.main.MainActivity;
import hu.egyetem.vizilabdapp.R;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton, anonymousLoginButton;

    // Firebase Authentication
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Firebase inicializálása
        firebaseAuth = FirebaseAuth.getInstance();

        // UI elemek inicializálása
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        anonymousLoginButton = findViewById(R.id.anonymousLoginButton);

        // Email-jelszó bejelentkezési lehetőség
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(LoginActivity.this, getString(R.string.error_missing_email), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(LoginActivity.this, getString(R.string.error_missing_password), Toast.LENGTH_SHORT).show();
                    return;
                }

                loginWithEmailAndPassword(email, password);
            }
        });

        // Anonim bejelentkezési lehetőség
        anonymousLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginAnonymously();
            }
        });
    }

    private void loginWithEmailAndPassword(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        Toast.makeText(LoginActivity.this, getString(R.string.successful_login, user.getEmail()), Toast.LENGTH_SHORT).show();
                        // Navigáció a fő képernyőre
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    } else {
                        Toast.makeText(LoginActivity.this, getString(R.string.error_, task.getException().getMessage()), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loginAnonymously() {
        firebaseAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        Toast.makeText(LoginActivity.this, getString(R.string.guest_login_succesful), Toast.LENGTH_SHORT).show();
                        // Navigáció a fő képernyőre
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    } else {
                        Toast.makeText(LoginActivity.this, getString(R.string.error_, task.getException().getMessage()), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}