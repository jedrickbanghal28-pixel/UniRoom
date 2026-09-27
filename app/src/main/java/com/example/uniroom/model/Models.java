package com.example.uniroom.model;

public class Models {

    public static class LoginRequest {
        public String username, password;
        public LoginRequest(String u, String p) { username = u; password = p; }
    }

    public static class RegisterRequest {
        public String first_name, last_name, student_id, username, email, contact_number, password;
        public int section_id;
        public RegisterRequest(String a, String b, String c, String d, String e, String f, String g, int h) {
            first_name = a; last_name = b; student_id = c; username = d; email = e;
            contact_number = f; password = g; section_id = h;
        }
    }

    public static class User {
        public int id;
        public String first_name, last_name, student_id, username, email, contact_number,
                role, program, section_name;
    }

    public static class ApiResponse {
        public boolean success;
        public String message;
        public User user;
        public Room room;
    }

    public static class StudentRoom {
        public int section_id, room_id, floor, capacity;
        public String section_name, room_name, building, status;
    }

    public static class StudentRoomResponse {
        public boolean success;
        public String message;
        public StudentRoom room;
    }

    public static class Schedule {
        public int id;
        public String subject_code, subject_name, day_of_week, start_time, end_time,
                room_name, building, faculty_name;
    }

    public static class Room {
        public int id, floor, capacity;
        public String room_name, building, status;
    }

    public static class RoomRequest {
        public int id, floor, capacity;
        public String room_name, building, status;
        public RoomRequest(int id, String room_name, String building, int floor, int capacity, String status) {
            this.id = id; this.room_name = room_name; this.building = building;
            this.floor = floor; this.capacity = capacity; this.status = status;
        }
    }

    public static class DeleteRequest {
        public int id;
        public DeleteRequest(int id) { this.id = id; }
    }

    public static class ListResponse<T> {
        public boolean success;
        public String message;
        public java.util.List<T> data;
    }
}
