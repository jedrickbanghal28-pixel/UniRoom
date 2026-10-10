<?php
/*
 * Students of one department (admin → Departments → click a department).
 *
 * GET ?department_id=1   (admin login token required)
 * → { success, department: {id, code, name}, students: [ {id, first_name,
 *      last_name, email, student_id, section, status} ] }
 * Sorted by section, then surname, then first name.
 */
require_once __DIR__ . '/../lib/db.php';
require_once __DIR__ . '/../lib/auth.php';

require_role(['admin']);

$departmentId = (int) ($_GET['department_id'] ?? 0);
if ($departmentId <= 0) respond(400, ['success' => false, 'message' => 'Department is required.']);

try {
    $q = $pdo->prepare('SELECT id, code, name FROM departments WHERE id = :id');
    $q->execute([':id' => $departmentId]);
    $dept = $q->fetch(PDO::FETCH_ASSOC);
    if (!$dept) respond(404, ['success' => false, 'message' => 'Department not found.']);

    $q = $pdo->prepare("
        SELECT u.id, u.first_name, u.last_name, u.email, u.student_id,
               s.name AS section, u.must_change_password
        FROM users u
        LEFT JOIN sections s ON s.id = u.section_id
        WHERE u.role = 'student'
          AND COALESCE(u.department_id, s.department_id) = :d
        ORDER BY s.name NULLS LAST, lower(u.last_name), lower(u.first_name), lower(u.email)
    ");
    $q->execute([':d' => $departmentId]);

    $students = [];
    foreach ($q->fetchAll(PDO::FETCH_ASSOC) as $u) {
        $u['status'] = $u['must_change_password'] ? 'Pending setup' : 'Active';
        unset($u['must_change_password']);
        $students[] = $u;
    }

    echo json_encode(['success' => true, 'department' => $dept, 'students' => $students]);
} catch (PDOException $e) {
    respond(500, ['success' => false, 'message' => 'Database error.']);
}
