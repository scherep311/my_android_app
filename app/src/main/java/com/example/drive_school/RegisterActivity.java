package com.example.drive_school;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import model.User;
import network.ApiService;
import retrofit2.Call;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilLastName, tilFirstName, tilMiddleName, tilPhone, tilEmail, tilPass1, tilPass2;
    private TextInputEditText etLastName, etFirstName, etMiddleName, etPhone, etEmail, etPass1, etPass2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        tilLastName = findViewById(R.id.tilLastName);
        tilFirstName = findViewById(R.id.tilFirstName);
        tilMiddleName = findViewById(R.id.tilMiddleName);
        tilPhone = findViewById(R.id.tilRegPhone);
        tilEmail = findViewById(R.id.tilEmail);
        tilPass1 = findViewById(R.id.tilRegPassword);
        tilPass2 = findViewById(R.id.tilRegPassword2);

        etLastName = findViewById(R.id.etLastName);
        etFirstName = findViewById(R.id.etFirstName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etPhone = findViewById(R.id.etRegPhone);
        etEmail = findViewById(R.id.etEmail);
        etPass1 = findViewById(R.id.etRegPassword);
        etPass2 = findViewById(R.id.etRegPassword2);

        findViewById(R.id.tvGoLogin).setOnClickListener(v -> finish());

        findViewById(R.id.btnRegister).setOnClickListener(v -> {
            clearErrors();

            String ln = text(etLastName);
            String fn = text(etFirstName);
            String mn = text(etMiddleName);
            String phone = text(etPhone);
            String email = text(etEmail);
            String p1 = text(etPass1);
            String p2 = text(etPass2);

            boolean ok = true;

            if (TextUtils.isEmpty(ln)) { tilLastName.setError("Введите фамилию"); ok = false; }
            if (TextUtils.isEmpty(fn)) { tilFirstName.setError("Введите имя"); ok = false; }
            if (TextUtils.isEmpty(mn)) { tilMiddleName.setError("Введите отчество"); ok = false; }

            if (TextUtils.isEmpty(phone)) { tilPhone.setError("Введите телефон"); ok = false; }

            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.setError("Введите корректный email");
                ok = false;
            }

            if (TextUtils.isEmpty(p1) || p1.length() < 6) {
                tilPass1.setError("Пароль минимум 6 символов");
                ok = false;
            }

            if (!p1.equals(p2)) {
                tilPass2.setError("Пароли не совпадают");
                ok = false;
            }

            if (!ok) return;

            ApiService api = network.ApiClient.get().create(ApiService.class);
            network.SessionManager session =
                    new network.SessionManager(this);

            model.User u = new model.User();
            u.lastName = ln;
            u.firstName = fn;
            u.patronymic = mn;
            u.phoneNumber = phone;
            u.role = "student";      // ВАЖНО из-за NOT NULL на сервере
            u.isActive = true;

            api.register(u, p1).enqueue(new retrofit2.Callback<model.User>() {
                @Override
                public void onResponse(retrofit2.Call<model.User> call,
                                       retrofit2.Response<model.User> response) {

                    if (!response.isSuccessful() || response.body() == null) {
                        android.widget.Toast.makeText(RegisterActivity.this,
                                "Ошибка регистрации: " + response.code(),
                                android.widget.Toast.LENGTH_LONG).show();
                        return;
                    }

                    model.User created = response.body();
                    if (created.id == null) {
                        android.widget.Toast.makeText(RegisterActivity.this,
                                "Сервер не вернул id", android.widget.Toast.LENGTH_LONG).show();
                        return;
                    }

                    session.saveUserId(created.id);
                    startActivity(new android.content.Intent(RegisterActivity.this, MainActivity.class));
                    finish();
                }

                @Override
                public void onFailure(retrofit2.Call<model.User> call, Throwable t) {
                    android.widget.Toast.makeText(RegisterActivity.this,
                            "Сервер недоступен: " + t.getMessage(),
                            android.widget.Toast.LENGTH_LONG).show();
                }
            });


            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void clearErrors() {
        tilLastName.setError(null);
        tilFirstName.setError(null);
        tilMiddleName.setError(null);
        tilPhone.setError(null);
        tilEmail.setError(null);
        tilPass1.setError(null);
        tilPass2.setError(null);
    }

    private String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
}
