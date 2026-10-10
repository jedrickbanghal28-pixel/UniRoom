# UniRoom – native Android app (Java + XML)

This is the "New UI Overhaul" web prototype rebuilt as **real Android screens**:
Java activities and XML layouts, no WebView. It uses only the standard Android
framework (no extra libraries), the **Inter** font and **Lucide** icons, like the prototype.

## Open it in Android Studio
1. Unzip the folder.
2. Android Studio → **File › Open…** → choose the `UniRoom` folder (the one with `settings.gradle.kts`).
3. Let Gradle sync (it downloads the Android Gradle Plugin the first time).
4. Press **Run ▶** on an emulator or phone (Android 8.0 / API 26 or newer).

If Android Studio asks to upgrade the Android Gradle Plugin, you can accept.

## Signing in

There is **no sign-up in the app**. The admin creates every student and faculty account on the UNIROOM admin website (BSIT or BSMEDTECH). The user then receives an email with their **username** and a **temporary password**.

1. Sign in with the username (or email) and the temporary password.
2. The app asks for a new password (8+ characters, letters and numbers). You can't continue until it is changed.
3. The dashboard opens with your own name, course, section, class schedule and notifications.

The sign-in is remembered on the phone until **Log out**. The phone needs internet. The API address is in `data/ApiClient.java`.

## Notifications

- **Faculty** open *Notify a section*. They choose a section (or **All sections** of the course), pick a room change or an announcement, and send.
- The notification is saved in the Supabase `notifications` table.
- **Students** of that course and section see it in *Notifications* (the bell on Home, with an unread count). They also get a phone notification:
  - every 30 seconds while the app is open
  - about every 15 minutes when it is closed (the shortest interval Android allows)
- Read and unread status is saved per student.

## Screens
| Prototype page        | Android                                      |
|-----------------------|----------------------------------------------|
| Login                 | `screens/LoginActivity` + `activity_login.xml` |
| StudentHome           | `tabs/StudentHomeTab` + `tab_student_home.xml` |
| FacultyHome           | `tabs/FacultyHomeTab` + `tab_faculty_home.xml` |
| Schedule              | `tabs/ScheduleTab` + `tab_schedule.xml`       |
| FindRoom (students)   | `tabs/FindRoomTab` + `tab_find_room.xml`      |
| AssignedRooms (faculty) | `tabs/MyRoomsTab` + `tab_my_rooms.xml`      |
| Profile               | `tabs/ProfileTab` + `tab_profile.xml`         |
| Alerts                | `screens/AlertsActivity` + `activity_alerts.xml` |
| SendNotification      | `screens/SendNotificationActivity` + `activity_notify.xml` |
| RoomDetailSheet       | `ui/RoomDetailSheet` + `sheet_room_detail.xml` |
| RoomLocationViewer    | `screens/RoomLocationActivity` + `activity_room_location.xml` |
| BottomNav             | built in `screens/MainActivity` (`item_nav.xml`) |

## How the code is organised
```
app/src/main/java/com/uniroom/app/
  model/    Room, ClassSession, Student, Teacher, AppAlert, Program ...
  data/     DemoData (rooms, timetable, users), AppState (who is signed in, alerts,
            rooms), ApiClient (talks to the API), StudentMapper (API answer → Student / classes /
            notifications), SavedSession (remembers the sign-in), Clock (time + date helpers)
  screens/  The activities (Login, Main, Alerts, Notify, RoomLocation)
  notify/   Notifier (phone notifications), NotificationCheckJob (background check)
  tabs/     The four bottom-navigation tabs shown inside MainActivity
  ui/       Reusable pieces: BottomSheet, FormField, SelectField, Pills,
            Segmented (program switcher), Rows (list rows), NextClassCard ...
app/src/main/res/
  layout/   One XML file per screen, plus item_*.xml rows and view_*.xml parts
  values/   colors.xml, themes.xml (student blue / faculty maroon), styles.xml
  drawable/ shapes, ripples and Lucide icons (ic_*.xml)
  drawable-nodpi/ the three Tech Hub room photos
  font/     Inter (regular, medium, semibold, bold)
```

## Good to know
- **Demo clock.** Like the prototype, the app pretends it is *Wednesday 23 September 2026, 7:40 AM*
  so the home screens always show classes. To use the phone's real time, set
  `USE_DEMO_CLOCK = false` in `data/Clock.java`.
- **Student vs faculty colours.** There are two themes in `values/themes.xml`. Layouts use
  `?attr/uniPrimary` and `?attr/uniPrimarySoft`, so the same XML turns blue or maroon.
- **What comes from the database:** accounts, student schedules, notifications and read status. Faculty room assignments on *My Rooms* still use the built-in list.
- **Moving the screens into another project.** Copy the `java/com/uniroom/app` folders and the
  `res` folders, then change `package com.uniroom.app` / `import com.uniroom.app.R` to your
  package name (Android Studio: right-click the package → *Refactor › Rename*), and copy the
  `<activity>` entries from `AndroidManifest.xml`.

## Credits
- Icons: [Lucide](https://lucide.dev) (ISC license), converted to Android vector drawables.
- Font: [Inter](https://rsms.me/inter/) (SIL Open Font License, see `INTER_FONT_LICENSE.txt`).
