<?php
/*
 * Change password (used on first login with a temporary password).
 * Works for the admin website AND the mobile app.
 *
 * POST { login (email or username), current_password, new_password }
 */
require_once __DIR__ . '/../lib/db.php';
require_once __DIR__ . '/../lib/accounts.php';

$data = body();
$login = trim((string) ($data['login'] ?? ($data['email'] ?? '')));
$current = (string) ($data['current_password'] ?? '');
$new = (string) ($data['new_password'] ?? '');

$errors = [];
if ($login === '') $errors[] = ['field' => 'login', 'message' => 'Email or username is required.'];
if ($current === '') $errors[] = ['field' => 'current_password', 'message' => 'Current password is required.'];
if ($new === '') $errors[] = ['field' => 'new_password', 'message' => 'New password is required.'];
if ($errors) respond(422, ['success' => false, 'message' => 'Please complete the required fields marked with *.', 'errors' => $errors]);

if ($problem = password_problem($new)) {
    respond(422, ['success' => false, 'message' => $problem, 'errors' => [['field' => 'new_password', 'message' => $problem]]]);
}
if ($new === $current) {
    respond(422, ['success' => false, 'message' => 'Your new password must be different from the temporary password.',
                  'errors' => [['field' => 'new_password', 'message' => 'Use a different password.']]]);
}

try {
    $q = $pdo->prepare('SELECT id, password_hash FROM users WHERE lower(email) = lower(:l) OR lower(username) = lower(:l) LIMIT 1');
    $q->execute([':l' => $login]);
    $user = $q->fetch();
    if (!$user || !password_verify($current, $user['password_hash'])) {
        respond(401, ['success' => false, 'message' => 'Current password is incorrect.',
                      'errors' => [['field' => 'current_password', 'message' => 'Current password is incorrect.']]]);
    }

    $pdo->prepare('UPDATE users SET password_hash = :h, must_change_password = FALSE WHERE id = :id')
        ->execute([':h' => password_hash($new, PASSWORD_DEFAULT), ':id' => $user['id']]);

    echo json_encode(['success' => true, 'message' => 'Password changed successfully.']);
} catch (PDOException $e) {
    respond(500, ['success' => false, 'message' => 'Database error.']);
}
