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
     * Get faculty information and department
     */
    $facultyStmt = $pdo->prepare("
        SELECT
            u.id,
            u.first_name,
            u.last_name,
            u.username,
            u.email,
            u.role,
            s.id AS section_id,
            s.name AS section_name,
            d.id AS department_id,
            d.code AS department
        FROM users u
        LEFT JOIN sections s
            ON s.id = u.section_id
        LEFT JOIN departments d
            ON d.id = COALESCE(u.department_id, s.department_id)
        WHERE u.id = :user_id
          AND u.role = 'faculty'
        LIMIT 1
    ");

    $facultyStmt->execute([
        ":user_id" => $userId
    ]);

    $faculty = $facultyStmt->fetch(PDO::FETCH_ASSOC);


    if (!$faculty) {

        http_response_code(404);

        echo json_encode([
            "success" => false,
            "message" => "Faculty account not found."
        ]);

        exit;
    }


    /*
     * Get sections in the faculty department
     */
    $sectionStmt = $pdo->prepare("
        SELECT
            s.id,
            s.name,
            COUNT(u.id) AS students
        FROM sections s
        LEFT JOIN users u
            ON u.section_id = s.id
           AND u.role = 'student'
        WHERE s.department_id = :department_id
        GROUP BY s.id, s.name
        ORDER BY s.id
    ");

    $sectionStmt->execute([
        ":department_id" => $faculty["department_id"]
    ]);

    $sections = $sectionStmt->fetchAll(PDO::FETCH_ASSOC);


    /*
     * Return faculty dashboard data
     */
    echo json_encode([
        "success" => true,

        "faculty" => [
            "id" => $faculty["id"],
            "first_name" => $faculty["first_name"],
            "last_name" => $faculty["last_name"],
            "username" => $faculty["username"],
            "email" => $faculty["email"],
            "role" => $faculty["role"]
        ],

        "department" => [
            "id" => $faculty["department_id"],
            "code" => $faculty["department"]
        ],

        "assigned_section" => [
            "id" => $faculty["section_id"],
            "name" => $faculty["section_name"]
        ],

        "sections" => $sections
    ]);

} catch (PDOException $e) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Database error."
    ]);

}

?>