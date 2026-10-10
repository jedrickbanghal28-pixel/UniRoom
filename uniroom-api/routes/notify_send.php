<?php
/*
 * Faculty → students notification (Android app, "Notify a section").
 *
 * POST (faculty login token required)
 *   { program: "BSIT" | "BSMT", section: "BSIT-A1" | "" (whole course),
 *     kind: "transfer" | "announcement", title, body }
 *
 * The notification is saved; every student of that course/section sees it
 * in their alerts (notifications.php).
 */
require_once __DIR__ . '/../lib/db.php';
require_once __DIR__ . '/../lib/auth.php';

$token = require_role(['faculty', 'admin']);
$data = body();

$program = strtoupper(trim((string) ($data['program'] ?? '')));
if ($program === 'BSMEDTECH' || $program === 'MEDTECH') $program = 'BSMT';
$sectionName = trim((string) ($data['section'] ?? ''));
$kind = ($data['kind'] ?? '') === 'transfer' ? 'transfer' : 'announcement';
$title = trim((string) ($data['title'] ?? ''));
$text = trim((string) ($data['body'] ?? ''));

$errors = [];
if ($program === '') $errors[] = ['field' => 'program', 'message' => 'Course is required.'];
if ($title === '') $errors[] = ['field' => 'title', 'message' => 'Title is required.'];
if ($text === '') $errors[] = ['field' => 'body', 'message' => 'Message is required.'];
if ($errors) respond(422, ['success' => false, 'message' => 'Please complete the required fields marked with *.', 'errors' => $errors]);
if (mb_strlen($title) > 150 || mb_strlen($text) > 500) {
    respond(422, ['success' => false, 'message' => 'The message is too long.']);
}

try {
    $q = $pdo->prepare('SELECT id, code FROM departments WHERE upper(code) = :c');
    $q->execute([':c' => $program]);
    $dept = $q->fetch(PDO::FETCH_ASSOC);
    if (!$dept) respond(404, ['success' => false, 'message' => 'Course "' . $program . '" does not exist.']);

    $sectionId = null;
    if ($sectionName !== '') {
        $q = $pdo->prepare('SELECT id FROM sections WHERE lower(name) = lower(:n) AND department_id = :d');
        $q->execute([':n' => $sectionName, ':d' => $dept['id']]);
        $sectionId = $q->fetchColumn();
        if (!$sectionId) {
            respond(404, ['success' => false, 'message' => 'Section ' . $sectionName . ' is not part of ' . $dept['code'] . '.']);
        }
    }

    $q = $pdo->prepare("SELECT first_name, last_name FROM users WHERE id = :id");
    $q->execute([':id' => $token['uid']]);
    $sender = $q->fetch(PDO::FETCH_ASSOC);
    if (!$sender) respond(401, ['success' => false, 'message' => 'Your session has expired. Please log in again.']);
    $senderName = trim($sender['first_name'] . ' ' . $sender['last_name']);

    $ins = $pdo->prepare("
        INSERT INTO notifications (sender_id, sender_name, department_id, section_id, kind, title, body)
        VALUES (:s, :sn, :d, :sec, :k, :t, :b)
        RETURNING id
    ");
    $ins->execute([
        ':s' => $token['uid'], ':sn' => $senderName, ':d' => $dept['id'],
        ':sec' => $sectionId ?: null, ':k' => $kind, ':t' => $title, ':b' => $text,
    ]);
    $id = (int) $ins->fetchColumn();

    // How many students will receive it
    $c = $pdo->prepare("
        SELECT COUNT(*) FROM users u
        LEFT JOIN sections s ON s.id = u.section_id
        WHERE u.role = 'student'
          AND COALESCE(u.department_id, s.department_id) = :d
          AND (CAST(:sec AS INTEGER) IS NULL OR u.section_id = CAST(:sec AS INTEGER))
    ");
    $c->execute([':d' => $dept['id'], ':sec' => $sectionId ?: null]);
    $count = (int) $c->fetchColumn();

    echo json_encode([
        'success' => true,
        'message' => 'Notification sent to ' . $count . ' student' . ($count === 1 ? '' : 's') . '.',
        'id' => $id,
        'recipients' => $count,
    ]);
} catch (PDOException $e) {
    respond(500, ['success' => false, 'message' => 'Database error.']);
}
