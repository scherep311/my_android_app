package model;

public class Schedule {
    public Long id;             // schedule_id
    public Instructor instructor;

    // LocalDate/LocalTime приходят строками
    public String date;         // "2026-01-06"
    public String startTime;    // "10:00:00"
    public String endTime;      // "12:00:00"
    public String status;
}

