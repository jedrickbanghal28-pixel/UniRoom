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
    // Explain WHY the connection failed (the password is never shown).
    $raw = $e->getMessage();
    if ($db['password'] !== '') $raw = str_replace($db['password'], '***', $raw);

    if (stripos($raw, 'password authentication failed') !== false) {
        $hint = 'Wrong DB_PASSWORD. Use the Supabase database password (or reset it in Supabase → Project Settings → Database).';
    } elseif (stripos($raw, 'Tenant or user not found') !== false) {
        $hint = 'Wrong DB_USER or DB_HOST. Copy both again from Supabase → Connect → Transaction pooler (user looks like postgres.xxxxxxxx).';
    } elseif (stripos($raw, 'could not translate host name') !== false || stripos($raw, 'Name or service not known') !== false) {
        $hint = 'Wrong DB_HOST. Copy it again from Supabase → Connect → Transaction pooler.';
    } elseif (stripos($raw, 'timeout') !== false || stripos($raw, 'timed out') !== false) {
        $hint = 'The database did not answer. Check DB_HOST and DB_PORT (6543), and that the Supabase project is not paused.';
    } elseif ($db['password'] === '' || strpos($db['password'], 'PUT_') === 0) {
        $hint = 'DB_PASSWORD is empty or still the placeholder. Add your Supabase database password in Vercel and redeploy.';
    } else {
        $hint = 'Check DB_HOST, DB_PORT, DB_USER and DB_PASSWORD in Vercel, then redeploy.';
    }

    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Database connection failed.',
        'reason'  => $hint,
        'details' => $raw,
        'using'   => ['host' => $db['host'], 'port' => $db['port'], 'user' => $db['username'], 'dbname' => $db['dbname']],
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
