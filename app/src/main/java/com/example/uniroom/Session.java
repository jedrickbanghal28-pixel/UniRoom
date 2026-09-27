package com.example.uniroom;

import android.content.Context;
import android.content.Intent;

import com.example.uniroom.model.Models.User;

public class Session {

    private final android.content.SharedPreferences preferences;

    Session(Context context) {
        preferences = context.getSharedPreferences("uniroom_session", Context.MODE_PRIVATE);
    }

    void save(User user) {
        preferences.edit()
                .putInt("id", user.id)
                .putString("name", user.first_name + " " + user.last_name)
                .putString("first_name", user.first_name)
                .putString("last_name", user.last_name)
                .putString("username", user.username)
                .putString("role", user.role)
                .putString("section", user.section_name)
                .putString("program", user.program)
                .apply();
    }

    int id() {
        return preferences.getInt("id", -1);
    }

    String name() {
        return preferences.getString("name", "Student");
    }

    String role() {
        return preferences.getString("role", "student");
    }

    String preferencesSection() {
        return preferences.getString("section", "");
    }

    String preferencesProgram() {
        return preferences.getString("program", "");
    }

    boolean loggedIn() {
        return id() > 0;
    }

    void clear() {
        preferences.edit().clear().apply();
    }

    void logout(Context context) {
        clear();
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    boolean protect(Context context) {
        if (loggedIn()) return true;
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        return false;
    }
}
