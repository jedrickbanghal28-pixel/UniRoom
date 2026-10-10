<?php

require_once __DIR__ . '/../lib/db.php';
require_once __DIR__ . '/../lib/auth.php';

$data = body();

$username = trim($data['username'] ?? '');
$password = $data['password'] ?? '';

if ($username === '' || $password === '') {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'Please enter username/email and password.'
    ]);
    exit;
}

try {
    // Section and program come along so the Android app can show
    // "BSIT • BSIT-A1" / "BSMT • BSMT-A1" right after login.
    $stmt = $pdo->prepare("
        SELECT
            u.id,
            u.username,
            u.email,
            u.first_name,
            u.last_name,
            u.student_id,
            u.contact_number,
            u.role,
            u.section_id,
            s.name AS section_name,
            d.code AS program,
            u.must_change_password,
            u.password_hash
        FROM users u
        LEFT JOIN sections s ON s.id = u.section_id
        LEFT JOIN departments d ON d.id = COALESCE(u.department_id, s.department_id)
        WHERE lower(u.username) = lower(:login)
           OR lower(u.email) = lower(:login)
        LIMIT 1
    ");

    $stmt->execute([
        ':login' => $username
    ]);

    $user = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$user || !password_verify($password, $user['password_hash'])) {
        http_response_code(401);

        echo json_encode([
            'success' => false,
            'message' => 'Invalid username/email or password.'
        ]);

        exit;
    }

    unset($user['password_hash']);

    // true = the user logged in with a temporary password and must pick a
    // new one (call change_password.php) before continuing.
    $user['must_change_password'] = (bool) $user['must_change_password'];
    $user['section_id'] = $user['section_id'] === null ? 0 : (int) $user['section_id'];

    // The app gets a login token (for notifications) only after the user has
    // replaced the temporary password with their own.
    $response = [
        'success' => true,
        'message' => $user['must_change_password']
            ? 'Please change your temporary password to continue.'
            : 'Login successful.',
        'user' => $user
    ];
    if (!$user['must_change_password']) {
        $response['token'] = make_token($user, 24 * 7);
    }

    echo json_encode($response);

} catch (PDOException $e) {

    http_response_code(500);

    echo json_encode([
        'success' => false,
        'message' => 'Database error.'
    ]);

}
?>