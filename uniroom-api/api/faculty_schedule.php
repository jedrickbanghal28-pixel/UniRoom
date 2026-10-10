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
     * Get the faculty member's department
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
     * Get schedules from the same department
     */
    $scheduleStmt = $pdo->prepare("
        SELECT
            s.id,
            sec.name AS section,
            sub.name AS subject,
            r.room_name AS room,
            CONCAT(f.first_name, ' ', f.last_name) AS faculty,
            s.day_of_week,
            s.start_time,
            s.end_time
        FROM schedules s

        JOIN sections sec
            ON sec.id = s.section_id

        JOIN subjects sub
            ON sub.id = s.subject_id

        JOIN rooms r
            ON r.id = s.room_id

        JOIN faculty f
            ON f.id = s.faculty_id

        WHERE sec.department_id = :department_id

        ORDER BY
            s.id ASC
    ");

    $scheduleStmt->execute([
        ":department_id" => $faculty["department_id"]
    ]);

    $schedules = $scheduleStmt->fetchAll(PDO::FETCH_ASSOC);


    echo json_encode([
        "success" => true,
        "schedules" => $schedules
    ]);

} catch (PDOException $e) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Database error."
    ]);

}

?>