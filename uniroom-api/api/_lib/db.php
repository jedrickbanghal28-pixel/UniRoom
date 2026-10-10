<?php
/*
|--------------------------------------------------------------------------
| Shared start-up for every endpoint
|--------------------------------------------------------------------------
| - JSON response header
| - CORS (the admin website runs on a different Vercel domain)
| - PDO connection to Supabase Postgres
| - body() helper to read the JSON request
*/

$config = require __DIR__ . '/config.php';

header('Content-Type: application/json; charset=UTF-8');

/* ---------- CORS ---------- */
$origin  = $_SERVER['HTTP_ORIGIN'] ?? '';
$allowed = array_map('trim', explode(',', $config['allowed_origins']));
if (in_array('*', $allowed, true)) {
    header('Access-Control-Allow-Origin: *');
} elseif ($origin !== '' && in_array(rtrim($origin, '/'), array_map(fn($o) => rtrim($o, '/'), $allowed), true)) {
    header('Access-Control-Allow-Origin: ' . $origin);
    header('Vary: Origin');
}
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');
header('Access-Control-Max-Age: 86400');

// Browsers send an OPTIONS "preflight" request first – answer it and stop.
if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'OPTIONS') {
    http_response_code(204);
    exit;
}

/* ---------- Database ---------- */
try {
    $db  = $config['db'];
    $dsn = "pgsql:host={$db['host']};port={$db['port']};dbname={$db['dbname']};sslmode={$db['sslmode']}";

    $pdo = new PDO($dsn, $db['username'], $db['password'], [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        // Supabase's transaction pooler (port 6543) does not keep server-side
        // prepared statements between requests, so let PDO prepare them itself.
        PDO::ATTR_EMULATE_PREPARES   => true,
    ]);
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
if (!function_exists('body')) {
    function body()
    {
        $input = file_get_contents('php://input');
        $data  = json_decode($input, true);
        return is_array($data) ? $data : [];
    }
}

/*
 * Send a JSON response and stop.
 */
if (!function_exists('respond')) {
    function respond(int $status, array $payload)
    {
        http_response_code($status);
        echo json_encode($payload);
        exit;
    }
}
