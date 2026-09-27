<?php

header('Content-Type: application/json; charset=UTF-8');

$config = require __DIR__ . '/config.php';

try {

    $dsn = "pgsql:host={$config['host']};port={$config['port']};dbname={$config['dbname']}";

    $pdo = new PDO(
        $dsn,
        $config['username'],
        $config['password'],
        [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
        ]
    );

} catch (PDOException $e) {

    http_response_code(500);

    echo json_encode([
        'success' => false,
        'message' => 'Database connection failed.'
    ]);

    exit;
}


/*
 * Read JSON request body
 */
function body()
{
    $input = file_get_contents('php://input');

    $data = json_decode($input, true);

    return is_array($data) ? $data : [];
}