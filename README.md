# UniRoom – Android + PHP/PostgreSQL

This version keeps the existing Java/Groovy Android project and adds a polished student experience plus live assigned-room integration.

## What was improved

- Cleaner UniRoom login and registration screens
- Correct BS-IT A1 and BS-MedTech A1 section IDs
- Persistent Android login session
- Student dashboard with live assigned room
- Student dashboard with live schedule / next-class summary
- Live schedule screen using the PHP schedule API instead of hardcoded classes
- Live room browser using the PHP rooms API
- Room filters: All, Tech Hub, Floor 3, Available
- Cleaner profile screen and working logout
- Working back/home/schedule/find-room/profile navigation
- Student room API: `student_room.php`

## PHP setup

Copy `php_api/student_room.php` to:

`C:\xampp\htdocs\php_api\student_room.php`

Keep the existing working `db.php`, `config.php`, `login.php`, `register.php`, `profile.php`, `schedule.php`, and `rooms.php` in that folder.

The Android app expects the API base URL configured in:

`app/src/main/java/com/example/uniroom/network/Client.java`

Example for a phone connected to the same Wi-Fi as the PC:

`http://YOUR-PC-IP/php_api/`

## Room data flow

Faculty website → `assign_room.php` → PostgreSQL `section_rooms` → `student_room.php` → Android Student Dashboard.

## Important database table

The live database needs the `section_rooms` bridge table:

```sql
CREATE TABLE IF NOT EXISTS section_rooms (
    id SERIAL PRIMARY KEY,
    section_id INTEGER NOT NULL UNIQUE,
    room_id INTEGER NOT NULL,
    CONSTRAINT fk_section_rooms_section
        FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE CASCADE,
    CONSTRAINT fk_section_rooms_room
        FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
);
```

## Testing order

1. Assign a room to BSIT-A1 or BSMedTech-A1 from the faculty website.
2. Register a student in the matching section.
3. Log in to Android using that student account.
4. The Student Dashboard should show the assigned room.
5. Open My Schedule and confirm the student's database schedule appears.
6. Open Find a Room and confirm rooms are loaded from PostgreSQL.
7. Open My Profile and test Log Out.

The project is Java + XML and uses Groovy Gradle scripts.
