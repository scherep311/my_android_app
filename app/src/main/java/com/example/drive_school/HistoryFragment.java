package com.example.drive_school;

import android.os.Bundle;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import network.ApiClient;
import network.ApiService;
import network.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryFragment extends Fragment {

    private ApiService api;
    private SessionManager session;

    private LinearLayout layoutHistoryList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        api = ApiClient.get().create(ApiService.class);
        session = new SessionManager(requireContext());

        layoutHistoryList = view.findViewById(R.id.layoutHistoryList);

        long userId = session.getUserId();
        if (userId <= 0) {
            Toast.makeText(requireContext(), "Нет userId, перезайди", Toast.LENGTH_LONG).show();
            return;
        }

        loadHistory(userId);
    }

    private void loadHistory(long userId) {
        layoutHistoryList.removeAllViews();

        api.completedBookings(userId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call,
                                   @NonNull Response<List<Map<String, Object>>> response) {

                if (!isAdded()) return;

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(requireContext(), "Ошибка истории: " + response.code(), Toast.LENGTH_LONG).show();
                    addEmpty();
                    return;
                }

                List<Map<String, Object>> list = response.body();
                if (list.isEmpty()) {
                    addEmpty();
                    return;
                }

                for (Map<String, Object> item : list) {
                    Map<String, Object> schedule = asMap(item.get("schedule"));
                    if (schedule == null) continue;

                    addHistoryCard(schedule);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Сеть: " + t.getMessage(), Toast.LENGTH_LONG).show();
                addEmpty();
            }
        });
    }

    private void addHistoryCard(Map<String, Object> schedule) {
        String date = safe(schedule.get("date"));
        String start = safe(schedule.get("startTime"));
        String end = safe(schedule.get("endTime"));

        if (start.length() >= 5) start = start.substring(0, 5);
        if (end.length() >= 5) end = end.substring(0, 5);

        String formattedDate = formatDateRu(date);

        CardView card = new CardView(requireContext());
        card.setRadius(dp(18));
        card.setCardElevation(dp(4));
        card.setUseCompatPadding(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
        );
        lp.bottomMargin = dp(8); // расстояние между карточками
        card.setLayoutParams(lp);

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(20), 0, dp(20), 0);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView tvDate = new TextView(requireContext());
        tvDate.setText(formattedDate);
        tvDate.setTextSize(16);
        tvDate.setTextColor(requireContext().getColor(R.color.primaryBlue));

        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        );
        tvDate.setLayoutParams(leftLp);

        TextView tvTime = new TextView(requireContext());
        tvTime.setText(start + "–" + end);
        tvTime.setTextSize(16);
        tvTime.setTextColor(requireContext().getColor(R.color.primaryBlue));

        row.addView(tvDate);
        row.addView(tvTime);

        card.addView(row);
        layoutHistoryList.addView(card);
    }

    private void addEmpty() {
        layoutHistoryList.removeAllViews();
        TextView tv = new TextView(requireContext());
        tv.setText("Пока нет завершённых занятий");
        tv.setTextSize(14);
        tv.setTextColor(requireContext().getColor(R.color.textGrey));
        tv.setPadding(0, dp(8), 0, 0);
        layoutHistoryList.addView(tv);
    }

    private Map<String, Object> asMap(Object o) {
        if (o instanceof Map) return (Map<String, Object>) o;
        return null;
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
