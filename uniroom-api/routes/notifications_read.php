<?php
/*
 * Mark notifications as read for the signed-in student.
 *
 * POST (student login token required)  { id: 12 }  or  { all: true }
 */
require_once __DIR__ . '/../lib/db.php';
require_once __DIR__ . '/../lib/auth.php';

$token = require_role(['student']);
$data = body();

try {
    if (!empty($data['all'])) {
        $q = $pdo->prepare("
            INSERT INTO notification_reads (notification_id, user_id)
            SELECT n.id, u.id
            FROM users u
            LEFT JOIN sections s ON s.id = u.section_id
            JOIN notifications n
              ON n.department_id = COALESCE(u.department_id, s.department_id)
             AND (n.section_id IS NULL OR n.section_id = u.section_id)
            WHERE u.id = :me
            ON CONFLICT DO NOTHING
        ");
        $q->execute([':me' => $token['uid']]);
    } else {
        $id = (int) ($data['id'] ?? 0);
        if ($id <= 0) respond(400, ['success' => false, 'message' => 'Notification ID is required.']);
        $q = $pdo->prepare("
            INSERT INTO notification_reads (notification_id, user_id)
            VALUES (:n, :me) ON CONFLICT DO NOTHING
        ");
        $q->execute([':n' => $id, ':me' => $token['uid']]);
    }
    echo json_encode(['success' => true, 'message' => 'Marked as read.']);
} catch (PDOException $e) {
    respond(500, ['success' => false, 'message' => 'Database error.']);
}
