package com.example.drive_school;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.CalendarMonth;
import com.kizitonwose.calendar.core.DayPosition;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.ViewContainer;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class LessonsFragment extends Fragment {

    private CalendarView calendarView;
    private TextView tvMonth;
    private LocalDate selectedDate = null;

    private TimeSlotAdapter slotAdapter;

    private static final String[] MONTHS_RU = {
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    };

    public LessonsFragment() {}

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_lessons, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvMonth = view.findViewById(R.id.tvMonth);
        calendarView = view.findViewById(R.id.calendarView);

        ImageButton prev = view.findViewById(R.id.btnPrevMonth);
        ImageButton next = view.findViewById(R.id.btnNextMonth);

        RecyclerView rvSlots = view.findViewById(R.id.rvSlots);
        rvSlots.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        int space = dp(10);
        rvSlots.addItemDecoration(new GridSpacingItemDecoration(3, space, true));
        rvSlots.setClipToPadding(false);
        rvSlots.setPadding(dp(10), dp(14), dp(10), dp(18));

        slotAdapter = new TimeSlotAdapter();
        rvSlots.setAdapter(slotAdapter);

        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(12);
        YearMonth endMonth = currentMonth.plusMonths(12);
        DayOfWeek firstDayOfWeek = DayOfWeek.MONDAY;

        calendarView.setup(startMonth, endMonth, firstDayOfWeek);
        calendarView.scrollToMonth(currentMonth);

        calendarView.setMonthScrollListener(new Function1<CalendarMonth, Unit>() {
            @Override
            public Unit invoke(CalendarMonth month) {
                int m = month.getYearMonth().getMonthValue();
                tvMonth.setText(MONTHS_RU[m - 1]);
                return Unit.INSTANCE;
            }
        });

        calendarView.setDayBinder(new MonthDayBinder<DayViewContainer>() {
            @NonNull
            @Override
            public DayViewContainer create(@NonNull View v) {
                return new DayViewContainer(v);
            }

            @Override
            public void bind(@NonNull DayViewContainer container, @NonNull CalendarDay day) {
                container.bind(day);
            }
        });
        LocalDate today = LocalDate.now();
        selectedDate = today;
        calendarView.scrollToMonth(YearMonth.from(today));
        calendarView.notifyDateChanged(today);
        loadSlotsFromServer(today);

        prev.setOnClickListener(v -> {
            CalendarMonth month = calendarView.findFirstVisibleMonth();
            if (month != null) calendarView.scrollToMonth(month.getYearMonth().minusMonths(1));
        });

        next.setOnClickListener(v -> {
            CalendarMonth month = calendarView.findFirstVisibleMonth();
            if (month != null) calendarView.scrollToMonth(month.getYearMonth().plusMonths(1));
        });

        view.findViewById(R.id.btnBook).setOnClickListener(v -> {
            model.Schedule selected = slotAdapter.getSelected();
            if (selected == null || selected.id == null) {
                android.widget.Toast.makeText(requireContext(), "Выбери время", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            network.ApiService api = network.ApiClient.get().create(network.ApiService.class);
            network.SessionManager session = new network.SessionManager(requireContext());
            long userId = session.getUserId();

            api.book(userId, selected.id).enqueue(new retrofit2.Callback<model.Booking>() {
                @Override
                public void onResponse(retrofit2.Call<model.Booking> call,
                                       retrofit2.Response<model.Booking> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        android.widget.Toast.makeText(requireContext(), "Запись создана ✅", android.widget.Toast.LENGTH_SHORT).show();

                        // после записи можно обновить слоты, чтобы забронированный исчез
                        if (selectedDate != null) {
                            loadSlotsFromServer(selectedDate);
                        }

                    } else {
                        String err = "";
                        try { err = response.errorBody() != null ? response.errorBody().string() : ""; } catch (Exception ignored) {}
                        android.widget.Toast.makeText(requireContext(),
                                "Ошибка записи: " + response.code() + " " + err,
                                android.widget.Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<model.Booking> call, Throwable t) {
                    android.widget.Toast.makeText(requireContext(), "Сервер недоступен: " + t.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                }
            });
        });
    }


    private void loadSlotsFromServer(@NonNull LocalDate date) {
        network.ApiService api = network.ApiClient.get().create(network.ApiService.class);
        network.SessionManager session = new network.SessionManager(requireContext());

        long userId = session.getUserId();
        String iso = date.toString(); // yyyy-MM-dd

        api.freeSlots(userId, iso).enqueue(new retrofit2.Callback<List<model.Schedule>>() {
            @Override
            public void onResponse(retrofit2.Call<List<model.Schedule>> call,
                                   retrofit2.Response<List<model.Schedule>> response) {

                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    List<model.Schedule> list = response.body();
                    slotAdapter.submit(list);

                    if (list == null || list.isEmpty()) {
                        android.widget.Toast.makeText(requireContext(),
                                "На " + iso + " слотов нет", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String err = "";
                    try { err = response.errorBody() != null ? response.errorBody().string() : ""; } catch (Exception ignored) {}
                    android.widget.Toast.makeText(requireContext(),
                            "Ошибка слотов: " + response.code() + " " + err,
                            android.widget.Toast.LENGTH_LONG).show();
                    slotAdapter.submit(java.util.Collections.emptyList());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<model.Schedule>> call, Throwable t) {
                if (!isAdded()) return;
                android.widget.Toast.makeText(requireContext(),
                        "Сервер недоступен: " + t.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                slotAdapter.submit(java.util.Collections.emptyList());
            }
        });
    }

    private int dp(int value) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (value * d);
    }

    private static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spanCount;
        private final int spacing;
        private final boolean includeEdge;

        GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(@NonNull android.graphics.Rect outRect,
                                   @NonNull View view,
                                   @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {

            int position = parent.getChildAdapterPosition(view);
            if (position == RecyclerView.NO_POSITION) return;

            int column = position % spanCount;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;
                if (position < spanCount) outRect.top = spacing;
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) outRect.top = spacing;
            }
        }
    }

    private class DayViewContainer extends ViewContainer {
        private final TextView tvDay;
        private CalendarDay day;

        DayViewContainer(@NonNull View view) {
            super(view);
            tvDay = view.findViewById(R.id.tvDay);

            view.setOnClickListener(v -> {
                if (day == null) return;
                if (day.getPosition() != DayPosition.MonthDate) return;

                LocalDate newDate = day.getDate();
                LocalDate oldDate = selectedDate;
                selectedDate = newDate;

                if (oldDate != null) calendarView.notifyDateChanged(oldDate);
                calendarView.notifyDateChanged(newDate);

                if (newDate.isBefore(LocalDate.now())) {
                    slotAdapter.submit(java.util.Collections.emptyList());
                    android.widget.Toast.makeText(requireContext(),
                            "Прошлые даты недоступны",
                            android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                LessonsFragment.this.loadSlotsFromServer(newDate);
            });
        }

        void bind(CalendarDay day) {
            this.day = day;

            tvDay.setText(String.valueOf(day.getDate().getDayOfMonth()));
            tvDay.setAlpha(day.getPosition() == DayPosition.MonthDate ? 1f : 0.25f);

            boolean isSelected = selectedDate != null && day.getDate().equals(selectedDate);
            if (isSelected) {
                tvDay.setBackgroundResource(R.drawable.bg_day_selected);
                tvDay.setTextColor(requireContext().getColor(R.color.cardWhite));
            } else {
                tvDay.setBackground(null);
                tvDay.setTextColor(requireContext().getColor(R.color.textBlack));
            }
        }
    }
}
