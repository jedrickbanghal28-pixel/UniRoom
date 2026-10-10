<?php
/*
 * The signed-in student's notifications (newest first).
 *
 * GET (student login token required)
 * → { success, unread, data: [ {id, kind, title, body, from, section,
 *                               created_at (UTC, ISO 8601), unread} ] }
 */
require_once __DIR__ . '/../lib/db.php';
require_once __DIR__ . '/../lib/auth.php';

$token = require_role(['student']);

try {
    $q = $pdo->prepare("
        SELECT u.section_id, COALESCE(u.department_id, s.department_id) AS department_id
        FROM users u LEFT JOIN sections s ON s.id = u.section_id
        WHERE u.id = :id
    ");
    $q->execute([':id' => $token['uid']]);
    $me = $q->fetch(PDO::FETCH_ASSOC);
    if (!$me) respond(401, ['success' => false, 'message' => 'Your session has expired. Please log in again.']);

    $q = $pdo->prepare("
        SELECT n.id, n.kind, n.title, n.body, n.sender_name AS from_name,
               COALESCE(sec.name, 'All sections') AS section,
               to_char(n.created_at AT TIME ZONE 'UTC', 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"') AS created_at,
               (r.user_id IS NULL) AS unread
        FROM notifications n
        LEFT JOIN sections sec ON sec.id = n.section_id
        LEFT JOIN notification_reads r ON r.notification_id = n.id AND r.user_id = :me
        WHERE n.department_id = :d
          AND (n.section_id IS NULL OR n.section_id = :sec)
        ORDER BY n.created_at DESC, n.id DESC
        LIMIT 100
    ");
    $q->execute([':me' => $token['uid'], ':d' => $me['department_id'], ':sec' => $me['section_id']]);

    $rows = [];
    $unread = 0;
    foreach ($q->fetchAll(PDO::FETCH_ASSOC) as $r) {
        $isUnread = (bool) $r['unread'];
        if ($isUnread) $unread++;
        $rows[] = [
            'id' => (int) $r['id'],
            'kind' => $r['kind'],
            'title' => $r['title'],
            'body' => $r['body'],
            'from' => $r['from_name'],
            'section' => $r['section'],
            'created_at' => $r['created_at'],
            'unread' => $isUnread,
        ];
    }

    echo json_encode(['success' => true, 'unread' => $unread, 'data' => $rows]);
} catch (PDOException $e) {
    respond(500, ['success' => false, 'message' => 'Database error.']);
}
