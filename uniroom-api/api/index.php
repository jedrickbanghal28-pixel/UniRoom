<?php
/*
|--------------------------------------------------------------------------
| UNIROOM API – single entry point
|--------------------------------------------------------------------------
| Vercel's Hobby (free) plan allows at most 12 serverless functions per
| deployment, so the whole API runs as ONE function. Every request
| (e.g. /api/login.php?x=1) is sent here by vercel.json, and this file
| runs the matching file from the routes/ folder (routes/login.php).
|
| The website and the mobile app keep using the same URLs as before.
|
| Local testing:   php -S localhost:8000 api/index.php
*/

$path = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';
$name = basename($path, '.php');

// "/", "/api", "/api/" and "/api/index.php" show the health check
if ($name === '' || $name === 'api' || $name === 'index') {
    $name = 'health';
}

// Some hosts pass the original query string only inside REQUEST_URI.
$query = parse_url($_SERVER['REQUEST_URI'] ?? '', PHP_URL_QUERY);
if ($query) {
    parse_str($query, $params);
    foreach ($params as $key => $value) {
        if (!isset($_GET[$key])) {
            $_GET[$key] = $value;
        }
    }
}

$file = dirname(__DIR__) . '/routes/' . $name . '.php';

// Only plain names like "admin_users" – never paths like "../config"
if (!preg_match('/^[a-z0-9_]+$/', $name) || !is_file($file)) {
    header('Content-Type: application/json; charset=UTF-8');
    header('Access-Control-Allow-Origin: *');
    http_response_code(404);
    echo json_encode([
        'success' => false,
        'message' => 'Unknown API endpoint: ' . $name,
    ]);
    exit;
}

require $file;
