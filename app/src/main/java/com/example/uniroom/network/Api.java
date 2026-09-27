package com.example.uniroom.network;

import com.example.uniroom.model.Models.*;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface Api {

    @POST("login.php")
    Call<ApiResponse> login(@Body LoginRequest x);

    @POST("register.php")
    Call<ApiResponse> register(@Body RegisterRequest x);

    @GET("schedule.php")
    Call<ListResponse<Schedule>> schedule(@Query("user_id") int id);

    @GET("rooms.php")
    Call<ListResponse<Room>> rooms(@Query("search") String q);

    @GET("profile.php")
    Call<ApiResponse> profile(@Query("user_id") int id);

    @GET("student_room.php")
    Call<StudentRoomResponse> studentRoom(@Query("user_id") int id);

    @POST("room_create.php")
    Call<ApiResponse> createRoom(@Body RoomRequest x);

    @POST("room_update.php")
    Call<ApiResponse> updateRoom(@Body RoomRequest x);

    @POST("room_delete.php")
    Call<ApiResponse> deleteRoom(@Body DeleteRequest x);
}
