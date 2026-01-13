package com.example.drive_school;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import network.ApiService;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilPhone, tilPassword;
    private TextInputEditText etPhone, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tilPhone = findViewById(R.id.tilPhone);
        tilPassword = findViewById(R.id.tilPassword);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);

        Button btnLogin = findViewById(R.id.btnLogin);

        findViewById(R.id.tvGoRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );

        btnLogin.setOnClickListener(v -> {
            // простая валидация
            String phone = etPhone.getText() == null ? "" : etPhone.getText().toString().trim();
            String pass = etPassword.getText() == null ? "" : etPassword.getText().toString();

            tilPhone.setError(null);
            tilPassword.setError(null);

            boolean ok = true;

            if (TextUtils.isEmpty(phone)) {
                tilPhone.setError("Введите номер телефона");
                ok = false;
            }

            if (TextUtils.isEmpty(pass)) {
                tilPassword.setError("Введите пароль");
                ok = false;
            }

            if (!ok) return;

            ApiService api = network.ApiClient.get().create(ApiService.class);
            network.SessionManager session =
                    new network.SessionManager(this);

            api.login(phone, pass).enqueue(new retrofit2.Callback<Object>() {
                @Override
                public void onResponse(retrofit2.Call<Object> call, retrofit2.Response<Object> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        android.widget.Toast.makeText(LoginActivity.this,
                                "Ошибка входа: " + response.code(), android.widget.Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Gson обычно десериализует Object как LinkedTreeMap
                    Object body = response.body();

                    // Пытаемся достать id:
                    Long userId = null;
                    if (body instanceof java.util.Map) {
                        Object idObj = ((java.util.Map<?, ?>) body).get("id");
                        if (idObj == null) idObj = ((java.util.Map<?, ?>) body).get("userId");
                        if (idObj instanceof Number) userId = ((Number) idObj).longValue();
                    }

                    if (userId == null) {
                        android.widget.Toast.makeText(LoginActivity.this,
                                "Не смогла получить id из ответа логина", android.widget.Toast.LENGTH_LONG).show();
                        return;
                    }

                    session.saveUserId(userId);

                    startActivity(new android.content.Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }

                @Override
                public void onFailure(retrofit2.Call<Object> call, Throwable t) {
                    android.widget.Toast.makeText(LoginActivity.this,
                            "Сервер недоступен: " + t.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                }
            });

            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
