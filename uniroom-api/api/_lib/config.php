<?php
/*
|--------------------------------------------------------------------------
| UNIROOM API configuration
|--------------------------------------------------------------------------
| Values are read from environment variables. On Vercel, set them under
| Project → Settings → Environment Variables. For local testing you can
| copy .env.example to .env (or set them in your terminal).
*/

// Load a local .env file if there is one (Vercel uses real env vars instead).
$envFile = dirname(__DIR__, 2) . '/.env';
if (is_file($envFile)) {
    foreach (file($envFile, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES) as $line) {
        $line = trim($line);
        if ($line === '' || $line[0] === '#' || strpos($line, '=') === false) continue;
        [$k, $v] = array_map('trim', explode('=', $line, 2));
        $v = trim($v, "\"'");
        if (getenv($k) === false) putenv("$k=$v");
    }
}

function env(string $key, $default = null)
{
    $v = getenv($key);
    return ($v === false || $v === '') ? $default : $v;
}

return [
    /*
     * Supabase database (Project Settings → Database → Connection string).
     * Use the *Transaction pooler* on Vercel (port 6543). The user looks like
     * "postgres.abcdefghijklmnop".
     */
    'db' => [
        'host'     => env('DB_HOST', 'localhost'),
        'port'     => env('DB_PORT', '5432'),
        'dbname'   => env('DB_NAME', 'postgres'),
        'username' => env('DB_USER', 'postgres'),
        'password' => env('DB_PASSWORD', ''),
        'sslmode'  => env('DB_SSLMODE', 'prefer'),   // use "require" for Supabase
    ],

    // Secret used to sign login tokens. Use a long random string in production.
    'app_secret' => env('APP_SECRET', 'change-this-local-dev-secret'),

    // Address of the admin website (used for links inside emails).
    'admin_web_url' => rtrim(env('ADMIN_WEB_URL', 'http://localhost:5500'), '/'),

    // Websites that may call this API from a browser (comma separated). "*" = any.
    'allowed_origins' => env('ALLOWED_ORIGINS', '*'),

    // Gmail SMTP. SMTP_PASS must be a Google "App password", not your normal password.
    'smtp' => [
        'host'      => env('SMTP_HOST', 'smtp.gmail.com'),
        'port'      => (int) env('SMTP_PORT', '465'),
        'secure'    => env('SMTP_SECURE', 'ssl'),        // ssl (465), tls (587) or none
        'username'  => env('SMTP_USER', ''),
        'password'  => env('SMTP_PASS', ''),
        'from'      => env('MAIL_FROM', env('SMTP_USER', '')),
        'from_name' => env('MAIL_FROM_NAME', 'UNIROOM'),
    ],

    // How long a password-setup link stays valid.
    'setup_link_hours' => (int) env('SETUP_LINK_HOURS', '48'),
];
