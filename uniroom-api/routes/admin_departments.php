<?php

header("Content-Type: application/json");

require_once __DIR__ . '/../lib/db.php';
require_once __DIR__ . '/../lib/auth.php';

require_role(['admin']);

/** @var PDO $pdo */

try {

    $stmt = $pdo->query("
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

    $departments = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo json_encode([
        "success" => true,
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