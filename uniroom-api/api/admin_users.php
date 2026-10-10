<?php

header("Content-Type: application/json");

require_once __DIR__ . '/_lib/db.php';
require_once __DIR__ . '/_lib/auth.php';

require_role(['admin']);

/** @var PDO $pdo */

try {

    $stmt = $pdo->query("
        SELECT
            u.id,
            u.first_name,
            u.last_name,
            u.student_id,
            u.employee_id,
            u.username,
            u.email,
            u.contact_number,
            u.role,
            u.gender,
            u.age,
            u.must_change_password,
            s.name AS section,
            COALESCE(d.code, ds.code) AS department
        FROM users u
        LEFT JOIN sections s
            ON s.id = u.section_id
        LEFT JOIN departments d
            ON d.id = u.department_id
        LEFT JOIN departments ds
            ON ds.id = s.department_id
        ORDER BY u.id ASC
    ");

    $users = $stmt->fetchAll(PDO::FETCH_ASSOC);

    foreach ($users as &$u) {
        $u["must_change_password"] = (bool) $u["must_change_password"];
        // "Pending setup" = account created but the user has not set a password yet
        $u["status"] = $u["must_change_password"] ? "Pending setup" : "Active";
    }

    echo json_encode([
        "success" => true,
        "users" => $users
    ]);

} catch (PDOException $e) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Database error."
    ]);

}

?>
