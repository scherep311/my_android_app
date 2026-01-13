package com.example.drive_school;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import model.User;
import network.ApiClient;
import network.ApiService;
import network.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private boolean isEditMode = false;

    private ImageButton btnEdit;
    private ImageView ivAvatar;
    private TextView btnPickAvatar;
    private ActivityResultLauncher<String> pickImageLauncher;
    private Uri pendingAvatarUri;


    private TextView tvFullName, tvPhone;

    private EditText etEmail, etInn, etCity, etAddress;

    private ApiService api;
    private SessionManager session;

    private User loadedUser; // то, что пришло с сервера

    public ProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        pendingAvatarUri = uri;
                        // показываем сразу
                        Glide.with(this).load(uri).circleCrop().into(ivAvatar);
                        // грузим на сервер, а после — выходим из edit режима
                        uploadAvatarAndExitEdit(uri);
                    }
                }
        );
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        api = ApiClient.get().create(ApiService.class);
        session = new SessionManager(requireContext());

        // header
        btnEdit = view.findViewById(R.id.btnEdit);
        ivAvatar = view.findViewById(R.id.ivAvatar);

        btnPickAvatar = view.findViewById(R.id.btnPickAvatar);
        if (btnPickAvatar == null) {
            Toast.makeText(requireContext(), "Нет кнопки btnPickAvatar в разметке", Toast.LENGTH_LONG).show();
            return;
        }

        btnPickAvatar.setOnClickListener(v -> {
            if (!isEditMode) {
                Toast.makeText(requireContext(), "Сначала включи редактирование", Toast.LENGTH_SHORT).show();
                return;
            }
            pickImageLauncher.launch("image/*");
        });

        tvFullName = view.findViewById(R.id.tvFullName);
        tvPhone = view.findViewById(R.id.tvPhone);

        // fields
        etEmail = view.findViewById(R.id.etEmail);
        etInn = view.findViewById(R.id.etInn);
        etCity = view.findViewById(R.id.etCity);
        etAddress = view.findViewById(R.id.etAddress);

        if (etEmail == null || etInn == null || etCity == null || etAddress == null) {
            Toast.makeText(requireContext(), "Ошибка разметки профиля: проверь id полей", Toast.LENGTH_LONG).show();
            return;
        }

        // эти поля пока НЕ из бэка (если их нет в сущностях). Чтоб не было "рандома":
        etInn.setText("");
        etCity.setText("");
        etAddress.setText("");

        setEditMode(false);

        long userId = session.getUserId();
        if (userId <= 0) {
            Toast.makeText(requireContext(), "Нет userId в сессии. Перезайди.", Toast.LENGTH_LONG).show();
            return;
        }

        loadProfile(userId);

        btnEdit.setOnClickListener(v -> {
            if (!isEditMode) {
                setEditMode(true);
                Toast.makeText(requireContext(), "Редактирование включено", Toast.LENGTH_SHORT).show();
            } else {
                saveProfile(userId);
            }
        });
    }

    private void uploadAvatarAndExitEdit(Uri uri) {
        long userId = session.getUserId();
        if (userId <= 0) return;

        try {
            File file = copyUriToTempFile(uri);

            RequestBody req = RequestBody.create(file, MediaType.parse("image/*"));
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(), req);

            api.uploadAvatar(userId, part).enqueue(new Callback<User>() {
                @Override
                public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                    if (!isAdded()) return;

                    if (!response.isSuccessful() || response.body() == null) {
                        Toast.makeText(requireContext(), "Не удалось загрузить фото: " + response.code(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    loadedUser = response.body();

                    // Если хочешь, чтобы вместе с фото сохранился email (если меняла) —
                    // сохраняем текстовые поля тоже:
                    saveTextFieldsThenExit(userId);

                    Toast.makeText(requireContext(), "Фото сохранено", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Ошибка загрузки: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Не удалось прочитать фото", Toast.LENGTH_LONG).show();
        }
    }

    private void saveTextFieldsThenExit(long userId) {
        User patch = new User();
        patch.email = etEmail.getText().toString().trim();

        api.updateProfile(userId, patch, null).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    loadedUser = response.body();
                    bindUserToUi(loadedUser);
                }

                setEditMode(false);
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                // даже если email не сохранился — всё равно выходим из edit режима
                setEditMode(false);
            }
        });
    }



    private File copyUriToTempFile(Uri uri) throws Exception {
        InputStream in = requireContext().getContentResolver().openInputStream(uri);
        if (in == null) throw new IllegalStateException("Cannot open input stream");

        File temp = new File(requireContext().getCacheDir(), "avatar_" + System.currentTimeMillis() + ".jpg");
        FileOutputStream out = new FileOutputStream(temp);

        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);

        in.close();
        out.close();
        return temp;
    }



    private void loadProfile(long userId) {
        api.getProfile(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (!isAdded()) return;

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(requireContext(), "Не удалось загрузить профиль: " + response.code(), Toast.LENGTH_LONG).show();
                    return;
                }

                loadedUser = response.body();
                bindUserToUi(loadedUser);
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindUserToUi(User u) {
        String fio = safe(u.lastName) + " " + safe(u.firstName) + " " + safe(u.patronymic);
        fio = fio.trim();
        if (fio.isEmpty()) fio = "—";

        tvFullName.setText(fio);
        tvPhone.setText(safe(u.phoneNumber));

        etEmail.setText(safe(u.email));

        etEmail.setText(safe(u.email));

        String url = safe(u.avatarUrl);
        if (!url.isEmpty()) {
            String full = url.startsWith("http") ? url : ("http://10.0.2.2:8080" + url);
            Glide.with(this).load(full).circleCrop().into(ivAvatar);
        } else {
            ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
        }
    }

    private void saveProfile(long userId) {

        User patch = new User();

        patch.email = etEmail.getText().toString().trim();

        api.updateProfile(userId, patch, null).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (!isAdded()) return;

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(requireContext(), "Не удалось сохранить: " + response.code(), Toast.LENGTH_LONG).show();
                    return;
                }

                loadedUser = response.body();
                bindUserToUi(loadedUser);

                setEditMode(false);
                Toast.makeText(requireContext(), "Сохранено", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void setEditMode(boolean enabled) {
        isEditMode = enabled;

        // Разрешаем редактировать только email (остальное пока не с бэка)
        setEnabled(etEmail, enabled);
        btnPickAvatar.setVisibility(enabled ? View.VISIBLE : View.GONE);



        setEnabled(etInn, false);
        setEnabled(etCity, false);
        setEnabled(etAddress, false);

        int color = ContextCompat.getColor(
                requireContext(),
                enabled ? R.color.primaryBlue : R.color.primaryOrange
        );
        btnEdit.setColorFilter(color);

        float alpha = enabled ? 1f : 0.9f;
        etEmail.setAlpha(alpha);
        etInn.setAlpha(0.7f);
        etCity.setAlpha(0.7f);
        etAddress.setAlpha(0.7f);
    }

    private void setEnabled(EditText et, boolean enabled) {
        et.setEnabled(enabled);
        et.setFocusable(enabled);
        et.setFocusableInTouchMode(enabled);
        et.setCursorVisible(enabled);
    }
}
