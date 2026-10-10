<?php

require_once __DIR__ . '/../lib/db.php';

$userId = (int) ($_GET['user_id'] ?? 0);

$s = $pdo->prepare("
    SELECT u.id, u.first_name, u.last_name, u.student_id, u.username, u.email,
           u.contact_number, u.role, s.name AS section_name, d.code AS program
    FROM users u
    LEFT JOIN sections s ON s.id = u.section_id
    LEFT JOIN departments d ON d.id = COALESCE(u.department_id, s.department_id)
    WHERE u.id = :i
");
$s->execute([':i' => $userId]);
$u = $s->fetch(PDO::FETCH_ASSOC);

if (!$u) {
    http_response_code(404);
    echo json_encode(['success' => false, 'message' => 'User not found.']);
    exit;
}

echo json_encode(['success' => true, 'message' => 'Profile loaded.', 'user' => $u]);
