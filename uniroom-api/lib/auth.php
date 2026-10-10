<?php
/*
|--------------------------------------------------------------------------
| Login tokens
|--------------------------------------------------------------------------
| After a successful admin/faculty login the API returns a signed token.
| The website sends it back as "Authorization: Bearer <token>".
| Protected endpoints call require_role(['admin']) etc.
*/

function b64url_encode(string $data): string
{
    return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
}

function b64url_decode(string $data): string
{
    return base64_decode(strtr($data, '-_', '+/') . str_repeat('=', (4 - strlen($data) % 4) % 4));
}

function make_token(array $user, int $hours = 8): string
{
    global $config;
    $payload = b64url_encode(json_encode([
        'uid'  => (int) $user['id'],
        'role' => $user['role'],
        'exp'  => time() + $hours * 3600,
    ]));
    $sig = b64url_encode(hash_hmac('sha256', $payload, $config['app_secret'], true));
    return $payload . '.' . $sig;
}

function read_token(): ?array
{
    global $config;
    $header = $_SERVER['HTTP_AUTHORIZATION'] ?? ($_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '');
    if ($header === '' && function_exists('getallheaders')) {
        foreach (getallheaders() as $k => $v) {
            if (strcasecmp($k, 'Authorization') === 0) $header = $v;
        }
    }
    if (!preg_match('/^Bearer\s+(\S+)$/i', $header, $m)) return null;

    $parts = explode('.', $m[1]);
    if (count($parts) !== 2) return null;
    [$payload, $sig] = $parts;

    $expected = b64url_encode(hash_hmac('sha256', $payload, $config['app_secret'], true));
    if (!hash_equals($expected, $sig)) return null;

    $data = json_decode(b64url_decode($payload), true);
    if (!is_array($data) || ($data['exp'] ?? 0) < time()) return null;
    return $data;
}

/** Stops the request with 401/403 unless the caller is logged in with one of $roles. */
function require_role(array $roles): array
{
    $t = read_token();
    if (!$t) {
        respond(401, ['success' => false, 'message' => 'Your session has expired. Please log in again.']);
    }
    if (!in_array($t['role'], $roles, true)) {
        respond(403, ['success' => false, 'message' => 'You do not have permission to do this.']);
    }
    return $t;
}

/** Random password that is easy to read (no 0/O, 1/l/I). */
function random_password(int $length = 10): string
{
    $chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789';
    $out = '';
    for ($i = 0; $i < $length; $i++) {
        $out .= $chars[random_int(0, strlen($chars) - 1)];
    }
    return $out;
}
