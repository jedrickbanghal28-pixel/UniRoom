package com.example.uniroom.network;
import com.example.uniroom.model.Models.*;
import retrofit2.*; import retrofit2.http.*;
public interface Api {
 @POST("login.php") Call<ApiResponse> login(@Body LoginRequest x);
 @POST("register.php") Call<ApiResponse> register(@Body RegisterRequest x);
 @GET("schedule.php") Call<ListResponse<Schedule>> schedule(@Query("user_id") int id);
 @GET("rooms.php") Call<ListResponse<Room>> rooms(@Query("search") String q);
 @GET("profile.php") Call<ApiResponse> profile(@Query("user_id") int id);
}
