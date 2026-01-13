package com.example.drive_school;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import network.ApiClient;
import network.ApiService;
import network.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private ApiService api;
    private SessionManager session;

    private TextView tvUserName, tvCar, tvCarNumber, tvHours;
    private LinearLayout layoutUpcomingList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        api = ApiClient.get().create(ApiService.class);
        session = new SessionManager(requireContext());

        tvUserName = view.findViewById(R.id.tvUserName);
        tvCar = view.findViewById(R.id.tvCar);
        tvCarNumber = view.findViewById(R.id.tvCarNumber);
        tvHours = view.findViewById(R.id.tvHours);
        layoutUpcomingList = view.findViewById(R.id.layoutUpcomingList);

        resetUi();

        long userId = session.getUserId();
        if (userId <= 0) {
            Toast.makeText(requireContext(), "Нет userId, перезайди", Toast.LENGTH_LONG).show();
            return;
        }

        loadHome(userId);
    }

    private void resetUi() {
        tvUserName.setText("—");
        tvCar.setText("Инструктор не назначен");
        tvCarNumber.setText("");
        tvHours.setText("0");
        layoutUpcomingList.removeAllViews();
    }

    private long toLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0L; }
    }

    private void loadHome(long userId) {
        api.home(userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {

                if (!isAdded()) return;

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(requireContext(), "Ошибка Home: " + response.code(), Toast.LENGTH_LONG).show();
                    resetUi();
                    return;
                }

                Map<String, Object> body = response.body();


                Object hours = body.get("hours");
                tvHours.setText(String.valueOf(toInt(hours)));



                Map<String, Object> instructor = asMap(body.get("instructor"));
                if (instructor != null) {

                    // === ФИО ИНСТРУКТОРА ===
                    Map<String, Object> user = asMap(instructor.get("user"));
                    if (user != null) {
                        String fio = (safe(user.get("lastName")) + " " +
                                safe(user.get("firstName")) + " " +
                                safe(user.get("patronymic"))).trim();
                        tvUserName.setText(fio.isEmpty() ? "Инструктор" : fio);
                    } else {
                        tvUserName.setText("Инструктор");
                    }

                    // === АВТО ===
                    String carModel = safe(instructor.get("carModel"));
                    String carColor = safe(instructor.get("carColor"));
                    String carPlate = safe(instructor.get("carPlate"));

                    String carText = carModel;
                    if (!carColor.isEmpty()) carText += " (" + carColor + ")";
                    if (carText.trim().isEmpty()) carText = "Инструктор назначен";

                    tvCar.setText(carText);
                    tvCarNumber.setText(carPlate);

                } else {
                    tvUserName.setText("Инструктор не назначен");
                    tvCar.setText("Инструктор не назначен");
                    tvCarNumber.setText("");
                }



                layoutUpcomingList.removeAllViews();
                List<?> bookings = asList(body.get("upcomingBookings"));
                if (bookings != null && !bookings.isEmpty()) {
                    for (Object o : bookings) {
                        Map<String, Object> booking = asMap(o);
                        if (booking == null) continue;

                        long bookingId = toLong(booking.get("id"));
                        Map<String, Object> schedule = asMap(booking.get("schedule"));
                        if (schedule != null && bookingId > 0) {
                            addBookingCard(bookingId, schedule);
                        }
                    }
                } else {
                    addEmptyUpcoming();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call,
                                  @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Сеть: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void addBookingCard(long bookingId, Map<String, Object> schedule) {
        String date = safe(schedule.get("date"));          // "2026-01-06"
        String start = safe(schedule.get("startTime"));    // "10:00:00"
        String end = safe(schedule.get("endTime"));        // "12:00:00"

        if (start.length() >= 5) start = start.substring(0, 5);
        if (end.length() >= 5) end = end.substring(0, 5);

        String formattedDate = formatDateRu(date);

        CardView card = new CardView(requireContext());
        card.setRadius(dp(18));
        card.setCardElevation(dp(4));
        card.setUseCompatPadding(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = dp(4);
        card.setLayoutParams(lp);

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(12), dp(8), dp(16), dp(8));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView tvDate = new TextView(requireContext());
        tvDate.setText(formattedDate);
        tvDate.setTextSize(16);

        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        );
        tvDate.setLayoutParams(leftLp);

        TextView tvTime = new TextView(requireContext());
        tvTime.setText(start + "–" + end);
        tvTime.setTextSize(16);
        tvTime.setPadding(dp(10), 0, dp(10), 0);

        TextView btnCancel = new TextView(requireContext());
        btnCancel.setText("Отменить");
        btnCancel.setTextSize(14);
        btnCancel.setPadding(dp(16), dp(10), dp(16), dp(10)); // чуть “квадратнее”
        btnCancel.setClickable(true);
        btnCancel.setFocusable(true);
        btnCancel.setTextColor(requireContext().getColor(R.color.cardWhite));
        btnCancel.setBackgroundResource(R.drawable.bg_cancel_orange); // если не нравится — скажи, сделаем отдельный drawable

        btnCancel.setOnClickListener(v -> {
            long userId = session.getUserId();

            api.cancelBooking(bookingId, userId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (!isAdded()) return;

                    if (response.isSuccessful()) {
                        Toast.makeText(requireContext(),
                                "Запись отменена ✅",
                                Toast.LENGTH_SHORT).show();
                        loadHome(userId);
                    } else {
                        String err = "";
                        try { err = response.errorBody() != null ? response.errorBody().string() : ""; } catch (Exception ignored) {}
                        Toast.makeText(requireContext(),
                                "Ошибка отмены: " + response.code() + " " + err,
                                Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Сеть: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });


        row.addView(tvDate);
        row.addView(tvTime);
        row.addView(btnCancel);

        card.addView(row);
        layoutUpcomingList.addView(card);
    }



    private void addEmptyUpcoming() {
        TextView tv = new TextView(requireContext());
        tv.setText("Нет предстоящих записей");
        tv.setTextSize(14);
        tv.setPadding(0, dp(8), 0, 0);
        layoutUpcomingList.addView(tv);
    }


    private Map<String, Object> asMap(Object o) {
        if (o instanceof Map) return (Map<String, Object>) o;
        return null;
    }

    private List<?> asList(Object o) {
        if (o instanceof List) return (List<?>) o;
        return null;
    }

    private int toInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
    }

    private String safe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private String formatDateRu(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) return "—";
        try {
            LocalDate d = LocalDate.parse(isoDate);
            return d.format(DateTimeFormatter.ofPattern("EE, dd.MM", new Locale("ru")));
        } catch (Exception e) {
            return isoDate;
        }
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(v * d);
    }
}
