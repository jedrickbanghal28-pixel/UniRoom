<?php
/*
|--------------------------------------------------------------------------
| Add a class schedule – with automatic conflict detection
|--------------------------------------------------------------------------
| Before saving, the API checks the same day for an overlapping time in:
|   * the same ROOM     – two classes cannot use one room at the same time
|   * the same FACULTY  – a teacher cannot teach two classes at the same time
|   * the same SECTION  – a section cannot have two classes at the same time
| Two times overlap when:  existing.start < new.end  AND  new.start < existing.end
| (a class that ends at 10:00 does NOT conflict with one that starts at 10:00).
|
| POST { section_id*, subject_id*, faculty_id*, room_id*, day_of_week*, start_time*, end_time* }
|
| 422 → missing / invalid fields   { errors: [{field, message}] }
| 409 → conflict                   { conflicts: [{type, message, ...}] }
| 200 → saved                      { schedule_id }
*/

header("Content-Type: application/json");

require_once __DIR__ . '/_lib/db.php';
require_once __DIR__ . '/_lib/auth.php';

require_role(['admin', 'faculty']);

/** @var PDO $pdo */

$data = body();

$sectionId = (int) ($data["section_id"] ?? 0);
$subjectId = (int) ($data["subject_id"] ?? 0);
$roomId    = (int) ($data["room_id"] ?? 0);
$facultyId = (int) ($data["faculty_id"] ?? 0);
$dayOfWeek = ucfirst(strtolower(trim($data["day_of_week"] ?? "")));
$startTime = trim($data["start_time"] ?? "");
$endTime   = trim($data["end_time"] ?? "");

$days = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];

/*
 * Required field validation
 */
$errors = [];
if ($sectionId <= 0) $errors[] = ["field" => "section_id", "message" => "Section is required."];
if ($subjectId <= 0) $errors[] = ["field" => "subject_id", "message" => "Subject is required."];
if ($facultyId <= 0) $errors[] = ["field" => "faculty_id", "message" => "Faculty is required."];
if (!in_array($dayOfWeek, $days, true)) $errors[] = ["field" => "day_of_week", "message" => "Day is required."];
if (!preg_match('/^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/', $startTime)) $errors[] = ["field" => "start_time", "message" => "Start time is required."];
if (!preg_match('/^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/', $endTime)) $errors[] = ["field" => "end_time", "message" => "End time is required."];
if ($roomId <= 0) $errors[] = ["field" => "room_id", "message" => "Room is required."];

if ($errors) {
    respond(422, [
        "success" => false,
        "message" => "Please complete the required fields marked with *.",
        "errors" => $errors
    ]);
}

if (strtotime($endTime) <= strtotime($startTime)) {
    respond(422, [
        "success" => false,
        "message" => "End time must be later than the start time.",
        "errors" => [["field" => "end_time", "message" => "End time must be later than the start time."]]
    ]);
}

try {

    /*
     * Find every class on the same day whose time overlaps AND that uses the
     * same room, the same faculty member or the same section.
     */
    $stmt = $pdo->prepare("
        SELECT
            sc.id,
            sc.section_id,
            sc.room_id,
            sc.faculty_id,
            sec.name AS section,
            sub.code AS subject_code,
            sub.name AS subject,
            r.room_name AS room,
            CONCAT(f.first_name, ' ', f.last_name) AS faculty,
            sc.day_of_week,
            TO_CHAR(sc.start_time, 'HH24:MI') AS start_time,
            TO_CHAR(sc.end_time, 'HH24:MI') AS end_time
        FROM schedules sc
        JOIN sections sec ON sec.id = sc.section_id
        JOIN subjects sub ON sub.id = sc.subject_id
        JOIN rooms r      ON r.id = sc.room_id
        LEFT JOIN faculty f ON f.id = sc.faculty_id
        WHERE lower(sc.day_of_week) = lower(:day)
          AND sc.start_time < CAST(:end_time AS time)
          AND CAST(:start_time AS time) < sc.end_time
          AND (sc.room_id = :room_id OR sc.faculty_id = :faculty_id OR sc.section_id = :section_id)
        ORDER BY sc.start_time
    ");

    $stmt->execute([
        ":day"        => $dayOfWeek,
        ":start_time" => $startTime,
        ":end_time"   => $endTime,
        ":room_id"    => $roomId,
        ":faculty_id" => $facultyId,
        ":section_id" => $sectionId
    ]);

    $overlaps = $stmt->fetchAll(PDO::FETCH_ASSOC);

    if ($overlaps) {

        $fmt = function ($t) { return date("g:i A", strtotime($t)); };
        $conflicts = [];

        foreach ($overlaps as $c) {
            $when = "{$c['day_of_week']} {$fmt($c['start_time'])} – {$fmt($c['end_time'])}";
            $class = "{$c['subject_code']} {$c['subject']} ({$c['section']})";

            if ((int) $c["room_id"] === $roomId) {
                $conflicts[] = [
                    "type" => "room",
                    "message" => "The selected room is already occupied during this time.",
                    "detail" => "{$c['room']} is used by $class on $when."
                ];
            }
            if ((int) $c["faculty_id"] === $facultyId) {
                $conflicts[] = [
                    "type" => "faculty",
                    "message" => "The selected faculty member already has a class during this time.",
                    "detail" => trim($c['faculty']) . " teaches $class in {$c['room']} on $when."
                ];
            }
            if ((int) $c["section_id"] === $sectionId) {
                $conflicts[] = [
                    "type" => "section",
                    "message" => "The selected section already has a class during this time.",
                    "detail" => "{$c['section']} has {$c['subject_code']} {$c['subject']} in {$c['room']} on $when."
                ];
            }
        }

        $first = $conflicts[0]["type"];
        $headline = [
            "room"    => "The selected room is already occupied during this time. Please choose another room or schedule.",
            "faculty" => "The selected faculty member already has a class during this time. Please choose another faculty member or schedule.",
            "section" => "The selected section already has a class during this time. Please choose another schedule.",
        ][$first];

        respond(409, [
            "success" => false,
            "conflict" => true,
            "message" => "Scheduling Conflict: " . $headline,
            "conflicts" => $conflicts
        ]);
    }


    /*
     * No conflicts – save the schedule.
     */
    $stmt = $pdo->prepare("
        INSERT INTO schedules (
            section_id,
            subject_id,
            room_id,
            faculty_id,
            day_of_week,
            start_time,
            end_time
        )
        VALUES (
            :section_id,
            :subject_id,
            :room_id,
            :faculty_id,
            :day_of_week,
            :start_time,
            :end_time
        )
        RETURNING id
    ");

    $stmt->execute([
        ":section_id" => $sectionId,
        ":subject_id" => $subjectId,
        ":room_id" => $roomId,
        ":faculty_id" => $facultyId,
        ":day_of_week" => $dayOfWeek,
        ":start_time" => $startTime,
        ":end_time" => $endTime
    ]);

    $scheduleId = $stmt->fetchColumn();

    echo json_encode([
        "success" => true,
        "message" => "Schedule saved successfully. No conflicts found.",
        "schedule_id" => $scheduleId
    ]);

} catch (PDOException $e) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Database error."
    ]);

}

?>
