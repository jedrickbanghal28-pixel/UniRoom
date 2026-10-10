<?php
/*
 * Individual account creation (admin only).
 *
 * POST {
 *   first_name*, last_name*, email*, gender*, age*, role*, department_id*,
 *   section_id* (students), student_id* (students), employee_id, contact_number,
 *   password_mode: "link" (default, recommended) | "temporary"
 * }
 *
 * 422 → { errors: [{field, message}] }  missing / invalid information
 * 409 → { errors: [...] }               email or student ID already registered
 * 200 → { user, email_sent, email_error? }
 */
require_once __DIR__ . '/../lib/db.php';
require_once __DIR__ . '/../lib/accounts.php';

$admin = require_role(['admin']);
$data = body();
$mode = ($data['password_mode'] ?? 'temporary') === 'link' ? 'link' : 'temporary';

try {
    $lookup = load_lookup($pdo);

    [$account, $errors] = validate_account($data, $lookup);
    if ($errors) {
        respond(422, [
            'success' => false,
            'message' => 'Please complete the required fields marked with *.',
            'errors' => $errors,
        ]);
    }

    $duplicates = find_duplicates($pdo, $account);
    if ($duplicates) {
        respond(409, [
            'success' => false,
            'message' => $duplicates[0]['message'],
            'errors' => $duplicates,
        ]);
    }

    $result = create_account($pdo, $account, $mode, (int) $admin['uid']);
    $mailer = new Mailer($config['smtp']);
    [$sent, $mailError] = send_account_email($mailer, $result['user'], $lookup, $result['temporary_password'], $result['setup_link']);

    $response = [
        'success' => true,
        'message' => $sent
            ? 'Account created. An email was sent to ' . $account['email'] . '.'
            : 'Account created, but the email could not be sent.',
        'user' => [
            'id' => $result['user']['id'],
            'username' => $result['user']['username'],
            'email' => $result['user']['email'],
            'name' => $account['first_name'] . ' ' . $account['last_name'],
        ],
        'password_mode' => $mode,
        'email_sent' => $sent,
    ];
    if (!$sent) {
        // Let the admin give the details to the user another way.
        $response['email_error'] = $mailError;
        if ($result['temporary_password']) $response['temporary_password'] = $result['temporary_password'];
        if ($result['setup_link']) $response['setup_link'] = $result['setup_link'];
    }
    echo json_encode($response);

} catch (PDOException $e) {
    if ($e->getCode() === '23505') {
        respond(409, ['success' => false, 'message' => 'This email or student ID is already registered.']);
    }
    respond(500, ['success' => false, 'message' => 'Account could not be created (database error).']);
}
