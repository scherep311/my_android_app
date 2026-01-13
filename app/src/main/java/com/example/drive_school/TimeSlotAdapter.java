package com.example.drive_school;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import model.Schedule;

import java.util.ArrayList;
import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.VH> {

    private final List<Schedule> data = new ArrayList<>();
    private int selectedPos = -1;

    public void submit(List<Schedule> slots) {
        data.clear();
        if (slots != null) data.addAll(slots);
        selectedPos = -1;
        notifyDataSetChanged();
    }

    public Schedule getSelected() {
        if (selectedPos < 0 || selectedPos >= data.size()) return null;
        return data.get(selectedPos);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_slot, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Schedule s = data.get(position);

        String start = s.startTime == null ? "" : s.startTime;
        String end = s.endTime == null ? "" : s.endTime;

        // подрежем секунды: 10:00:00 -> 10:00
        start = start.length() >= 5 ? start.substring(0, 5) : start;
        end = end.length() >= 5 ? end.substring(0, 5) : end;

        h.tv.setText(start + "-" + end);

        boolean selected = position == selectedPos;
        h.tv.setBackgroundResource(selected ? R.drawable.bg_slot_selected : R.drawable.bg_slot_blue);

        h.itemView.setOnClickListener(v -> {
            int old = selectedPos;
            selectedPos = h.getAdapterPosition();
            if (old != -1) notifyItemChanged(old);
            notifyItemChanged(selectedPos);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv;
        VH(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tvSlot);
        }
    }
}
