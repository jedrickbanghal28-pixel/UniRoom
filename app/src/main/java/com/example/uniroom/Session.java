package com.example.uniroom;
import android.content.*; import com.example.uniroom.model.Models.User;
public class Session { android.content.SharedPreferences p; Session(Context c){p=c.getSharedPreferences("u",0);} void save(User u){p.edit().putInt("id",u.id).putString("name",u.first_name+" "+u.last_name).apply();} int id(){return p.getInt("id",-1);} String name(){return p.getString("name","Student");} void clear(){p.edit().clear().apply();} }
