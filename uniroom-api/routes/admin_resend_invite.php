<?php
/*
 * Sends the account email again with a NEW setup link or temporary password
 * (admin only). Only for accounts that have not set their own password yet.
 *
 * POST { user_id, password_mode: "link" | "temporary" }
 */
require_once __DIR__ . '/../lib/db.php';
require_once __DIR__ . '/../lib/accounts.php';

require_role(['admin']);
$data = body();
$userId = (int) ($data['user_id'] ?? 0);
$mode = ($data['password_mode'] ?? 'link') === 'temporary' ? 'temporary' : 'link';

if ($userId <= 0) respond(400, ['success' => false, 'message' => 'User ID is required.']);

try {
    $q = $pdo->prepare('SELECT * FROM users WHERE id = :id');
    $q->execute([':id' => $userId]);
    $user = $q->fetch();
    if (!$user) respond(404, ['success' => false, 'message' => 'User not found.']);
    if (!$user['must_change_password']) {
        respond(409, ['success' => false, 'message' => 'This user has already set their own password.']);
    }

    $temporaryPassword = null;
    $setupLink = null;
    $pdo->beginTransaction();
    if ($mode === 'temporary') {
        $temporaryPassword = random_password(10);
        $pdo->prepare('UPDATE users SET password_hash = :h WHERE id = :id')
            ->execute([':h' => password_hash($temporaryPassword, PASSWORD_DEFAULT), ':id' => $userId]);
        $pdo->prepare('UPDATE password_tokens SET used_at = now() WHERE user_id = :u AND used_at IS NULL')
            ->execute([':u' => $userId]);
    } else {
        $setupLink = new_setup_link($pdo, $userId);
    }
    $pdo->commit();

    $lookup = load_lookup($pdo);
    $mailer = new Mailer($config['smtp']);
    [$sent, $mailError] = send_account_email($mailer, $user, $lookup, $temporaryPassword, $setupLink);

    $response = [
        'success' => true,
        'email_sent' => $sent,
        'message' => $sent ? 'A new email was sent to ' . $user['email'] . '.' : 'The email could not be sent.',
    ];
    if (!$sent) {
        $response['email_error'] = $mailError;
        if ($temporaryPassword) $response['temporary_password'] = $temporaryPassword;
        if ($setupLink) $response['setup_link'] = $setupLink;
    }
    echo json_encode($response);

} catch (PDOException $e) {
    if ($pdo->inTransaction()) $pdo->rollBack();
    respond(500, ['success' => false, 'message' => 'Database error.']);
}
