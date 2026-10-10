<?php

header("Content-Type: application/json; charset=UTF-8");

require_once __DIR__ . '/_lib/db.php';
require_once __DIR__ . '/_lib/auth.php';

require_role(['admin']);

/** @var PDO $pdo */

try {

    $studentStmt = $pdo->query("
        SELECT COUNT(*)
        FROM users
        WHERE role = 'student'
    ");

    $totalStudents = (int) $studentStmt->fetchColumn();


    /*
     * Count faculty
     */
    $facultyStmt = $pdo->query("
        SELECT COUNT(*)
        FROM users
        WHERE role = 'faculty'
    ");

    $totalFaculty = (int) $facultyStmt->fetchColumn();


    /*
     * Count departments
     */
    $departmentStmt = $pdo->query("
        SELECT COUNT(*)
        FROM departments
    ");

    $totalDepartments = (int) $departmentStmt->fetchColumn();


    /*
     * Count sections
     */
    $sectionStmt = $pdo->query("
        SELECT COUNT(*)
        FROM sections
    ");

    $totalSections = (int) $sectionStmt->fetchColumn();


    /*
     * Count all users
     */
    $activeUserStmt = $pdo->query("
        SELECT COUNT(*)
        FROM users
    ");

    $activeUsers = (int) $activeUserStmt->fetchColumn();


    /*
     * Get department overview
     */
    $departmentQuery = $pdo->query("
        SELECT
            d.id,
            d.code,
            COUNT(DISTINCT s.id) AS sections,
            COUNT(DISTINCT CASE
                WHEN u.role = 'student' THEN u.id
            END) AS students
        FROM departments d
        LEFT JOIN sections s
            ON s.department_id = d.id
        LEFT JOIN users u
            ON u.section_id = s.id
        GROUP BY d.id, d.code
        ORDER BY d.code ASC
    ");

    $departments = $departmentQuery->fetchAll(PDO::FETCH_ASSOC);


    /*
     * Return dashboard information
     */
    echo json_encode([
        "success" => true,

        "statistics" => [
            "students" => $totalStudents,
            "faculty" => $totalFaculty,
            "departments" => $totalDepartments,
            "sections" => $totalSections,
            "active_users" => $activeUsers
        ],

        "departments" => $departments
    ]);

} catch (PDOException $e) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Database error."
    ]);
}

?>