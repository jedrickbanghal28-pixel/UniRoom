<?php

require_once __DIR__ . '/db.php';

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
    $stmt = $pdo->prepare("
        SELECT
            u.id,
            u.username,
            u.email,
            u.first_name,
            u.last_name,
            u.role,
            u.password_hash
        FROM users u
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

    echo json_encode([
        'success' => true,
        'message' => 'Login successful.',
        'user' => $user
    ]);

} catch (PDOException $e) {

    http_response_code(500);

    echo json_encode([
        'success' => false,
        'message' => 'Database error.'
    ]);

}
?>