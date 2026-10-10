<?php

header("Content-Type: application/json");

require_once __DIR__ . '/_lib/db.php';

/** @var PDO $pdo */

$userId = isset($_GET["user_id"])
    ? (int) $_GET["user_id"]
    : 0;

if ($userId <= 0) {

    http_response_code(400);

    echo json_encode([
        "success" => false,
        "message" => "Invalid user ID."
    ]);

    exit;
}

try {

    /*
     * Find the faculty member's department
     */
    $facultyStmt = $pdo->prepare("
        SELECT
            COALESCE(u.department_id, s.department_id) AS department_id
        FROM users u
        LEFT JOIN sections s
            ON s.id = u.section_id
        WHERE u.id = :user_id
          AND u.role = 'faculty'
        LIMIT 1
    ");

    $facultyStmt->execute([
        ":user_id" => $userId
    ]);

    $faculty = $facultyStmt->fetch(PDO::FETCH_ASSOC);

    if (!$faculty || $faculty["department_id"] === null) {

        http_response_code(404);

        echo json_encode([
            "success" => false,
            "message" => "Faculty account not found."
        ]);

        exit;
    }


    /*
     * Get faculty members who teach in the same department, with the
     * subject(s) they handle there, their class schedule and rooms.
     * A faculty member belongs to the department when:
     *   - a subject is assigned to them for this department (faculty_subjects), or
     *   - they have a class with a section of this department, or
     *   - their own account is in this department.
     */
    $facultyStmt = $pdo->prepare("
        SELECT
            f.id,
            f.first_name,
            f.last_name,
            COALESCE(u.email, f.email) AS email,

            (SELECT string_agg(DISTINCT sub.code || ' – ' || sub.name, ', ')
               FROM faculty_subjects fs
               JOIN subjects sub ON sub.id = fs.subject_id
              WHERE fs.faculty_id = f.id
                AND fs.department_id = :department_id) AS subjects,

            (SELECT string_agg(
                        sc.day_of_week || ' ' ||
                        TO_CHAR(sc.start_time, 'FMHH12:MI AM') || ' – ' ||
                        TO_CHAR(sc.end_time, 'FMHH12:MI AM') || ' (' || sec.name || ')',
                        '; '
                        ORDER BY array_position(
                            ARRAY['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'],
                            sc.day_of_week),
                        sc.start_time)
               FROM schedules sc
               JOIN sections sec ON sec.id = sc.section_id
              WHERE sc.faculty_id = f.id
                AND sec.department_id = :department_id) AS schedule,

            (SELECT string_agg(DISTINCT r.room_name, ', ')
               FROM schedules sc
               JOIN sections sec ON sec.id = sc.section_id
               JOIN rooms r ON r.id = sc.room_id
              WHERE sc.faculty_id = f.id
                AND sec.department_id = :department_id) AS rooms

        FROM faculty f
        LEFT JOIN users u
            ON u.id = f.user_id
        LEFT JOIN sections us
            ON us.id = u.section_id
        WHERE EXISTS (
                SELECT 1 FROM faculty_subjects fs
                WHERE fs.faculty_id = f.id AND fs.department_id = :department_id)
           OR EXISTS (
                SELECT 1 FROM schedules sc
                JOIN sections sec ON sec.id = sc.section_id
                WHERE sc.faculty_id = f.id AND sec.department_id = :department_id)
           OR COALESCE(u.department_id, us.department_id) = :department_id
        ORDER BY f.last_name, f.first_name
    ");

    $facultyStmt->execute([
        ":department_id" => $faculty["department_id"]
    ]);

    $members = $facultyStmt->fetchAll(PDO::FETCH_ASSOC);


    echo json_encode([
        "success" => true,
        "faculty" => $members
    ]);

} catch (PDOException $e) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Database error."
    ]);

}

?>