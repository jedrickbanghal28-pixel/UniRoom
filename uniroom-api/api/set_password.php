<?php
/*
 * Password setup from the email link (public – the token is the proof).
 *
 * GET  ?token=...                    → check the link, returns the user's name/email
 * POST { token, new_password }       → saves the password
 */
require_once __DIR__ . '/_lib/db.php';
require_once __DIR__ . '/_lib/accounts.php';

$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
$data = $method === 'POST' ? body() : $_GET;
$token = trim((string) ($data['token'] ?? ''));

if ($token === '' || !ctype_xdigit($token)) {
    respond(400, ['success' => false, 'message' => 'This password setup link is not valid.']);
}

try {
    $q = $pdo->prepare('
        SELECT t.id AS token_id, t.expires_at, t.used_at, u.id, u.first_name, u.email, u.role
        FROM password_tokens t
        JOIN users u ON u.id = t.user_id
        WHERE t.token_hash = :h
        LIMIT 1
    ');
    $q->execute([':h' => hash('sha256', $token)]);
    $row = $q->fetch();

    if (!$row) respond(404, ['success' => false, 'message' => 'This password setup link is not valid.']);
    if ($row['used_at'] !== null) {
        respond(410, ['success' => false, 'message' => 'This link has already been used. Ask your administrator for a new one.']);
    }
    if (strtotime($row['expires_at']) < time()) {
        respond(410, ['success' => false, 'message' => 'This link has expired. Ask your administrator for a new one.']);
    }

    if ($method !== 'POST') {
        echo json_encode([
            'success' => true,
            'user' => ['first_name' => $row['first_name'], 'email' => $row['email'], 'role' => $row['role']],
        ]);
        exit;
    }

    $password = (string) ($data['new_password'] ?? '');
    if ($password === '') {
        respond(422, ['success' => false, 'message' => 'New password is required.', 'errors' => [['field' => 'new_password', 'message' => 'New password is required.']]]);
    }
    if ($problem = password_problem($password)) {
        respond(422, ['success' => false, 'message' => $problem, 'errors' => [['field' => 'new_password', 'message' => $problem]]]);
    }

    $pdo->beginTransaction();
    $pdo->prepare('UPDATE users SET password_hash = :h, must_change_password = FALSE WHERE id = :id')
        ->execute([':h' => password_hash($password, PASSWORD_DEFAULT), ':id' => $row['id']]);
    $pdo->prepare('UPDATE password_tokens SET used_at = now() WHERE user_id = :u AND used_at IS NULL')
        ->execute([':u' => $row['id']]);
    $pdo->commit();

    echo json_encode(['success' => true, 'message' => 'Your password has been set. You can now log in.']);

} catch (PDOException $e) {
    if ($pdo->inTransaction()) $pdo->rollBack();
    respond(500, ['success' => false, 'message' => 'Database error.']);
}
