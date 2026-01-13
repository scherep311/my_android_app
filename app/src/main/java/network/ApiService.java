package network;

import model.Booking;
import model.Schedule;
import model.User;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.http.Multipart;
import retrofit2.http.Part;


import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ===== AUTH =====
    @POST("api/client/auth/register")
    Call<User> register(
            @Body User user,
            @Query("password") String password
    );

    @Multipart
    @POST("api/client/profile/avatar")
    Call<User> uploadAvatar(
            @Query("userId") Long userId,
            @Part MultipartBody.Part file
    );


    @POST("api/client/auth/login")
    Call<Object> login(
            @Query("phone") String phone,
            @Query("password") String password
    );

    // ===== HOME =====
    @GET("api/client/home")
    Call<Map<String, Object>> home(@Query("userId") Long userId);

    // ===== PROFILE =====
    @GET("api/client/profile")
    Call<User> getProfile(@Query("userId") Long userId);

    @PUT("api/client/profile")
    Call<User> updateProfile(
            @Query("userId") Long userId,
            @Body User patch,
            @Query("newPassword") String newPassword // может быть null
    );

    // ===== SCHEDULE =====
    @GET("api/client/schedule/free")
    Call<List<Schedule>> freeSlots(
            @Query("userId") Long userId,
            @Query("date") String dateIso
    );

    @GET("api/client/schedule/instructor/{instructorId}")
    Call<List<Schedule>> allByInstructor(@Path("instructorId") Long instructorId);

    // ===== BOOKINGS =====
    @POST("api/client/bookings/book")
    Call<Booking> book(
            @Query("userId") Long userId,
            @Query("scheduleId") Long scheduleId
    );

    @POST("api/client/bookings/{bookingId}/cancel")
    Call<Void> cancelBooking(
            @Path("bookingId") Long bookingId,
            @Query("userId") Long userId
    );

    @GET("api/client/bookings")
    Call<List<Booking>> myBookings(@Query("userId") Long userId);

    @GET("/api/client/booking/completed")
    Call<List<Map<String, Object>>> completedBookings(@Query("userId") long userId);

}
