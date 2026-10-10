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
     * Get students from the same department
     */
    $studentStmt = $pdo->prepare("
        SELECT
            u.id,
            u.student_id,
            u.first_name,
            u.last_name,
            u.email,
            s.name AS section
        FROM users u
        JOIN sections s
            ON s.id = u.section_id
        WHERE u.role = 'student'
          AND s.department_id = :department_id
        ORDER BY s.id, u.last_name, u.first_name
    ");

    $studentStmt->execute([
        ":department_id" => $faculty["department_id"]
    ]);

    $students = $studentStmt->fetchAll(PDO::FETCH_ASSOC);


    echo json_encode([
        "success" => true,
        "students" => $students
    ]);

} catch (PDOException $e) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Database error."
    ]);

}

?>