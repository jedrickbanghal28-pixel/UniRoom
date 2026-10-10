<?php
/*
|--------------------------------------------------------------------------
| Account creation helpers (used by individual AND bulk creation)
|--------------------------------------------------------------------------
| validate_account()   checks the required information
| find_duplicates()    checks email / student ID against the database
| create_account()     saves the user and prepares the password
| send_account_email() sends the "your account was created" email
*/

require_once __DIR__ . '/auth.php';
require_once __DIR__ . '/mailer.php';

const GENDERS = ['Male', 'Female', 'Prefer not to say'];
const ROLES   = ['student', 'faculty'];
const MIN_AGE = 15;
const MAX_AGE = 100;

/** Departments and sections, used to check and translate names into IDs. */
function load_lookup(PDO $pdo): array
{
    $departments = $pdo->query('SELECT * FROM departments ORDER BY code')->fetchAll();
    $sections = $pdo->query('SELECT id, name, department_id FROM sections ORDER BY name')->fetchAll();
    return ['departments' => $departments, 'sections' => $sections];
}

function department_label(array $d): string
{
    return !empty($d['name']) ? $d['code'] . ' – ' . $d['name'] : $d['code'];
}

/** Finds a department by id, code or name (any letter case). */
function match_department(array $lookup, $value): ?array
{
    $v = strtolower(trim((string) $value));
    if ($v === '') return null;
    foreach ($lookup['departments'] as $d) {
        if ((string) $d['id'] === $v
            || strtolower($d['code']) === $v
            || (!empty($d['name']) && strtolower($d['name']) === $v)
            || strtolower(department_label($d)) === $v) {
            return $d;
        }
    }
    return null;
}

function match_section(array $lookup, $value): ?array
{
    $v = strtolower(trim((string) $value));
    if ($v === '') return null;
    foreach ($lookup['sections'] as $s) {
        if ((string) $s['id'] === $v || strtolower($s['name']) === $v) return $s;
    }
    return null;
}

function normalize_gender($value): ?string
{
    $v = strtolower(trim((string) $value));
    $map = ['m' => 'Male', 'male' => 'Male', 'f' => 'Female', 'female' => 'Female',
            'prefer not to say' => 'Prefer not to say', 'n/a' => 'Prefer not to say'];
    return $map[$v] ?? null;
}

/**
 * Checks one account and turns it into clean values.
 * Accepts IDs from the form (department_id, section_id) or names from a CSV
 * file (department, section).
 *
 * @return array [cleanAccount, errors] – errors is a list of ['field' => .., 'message' => ..]
 */
function validate_account(array $raw, array $lookup): array
{
    $errors = [];
    $get = fn($k) => trim((string) ($raw[$k] ?? ''));
    $err = function ($field, $msg) use (&$errors) { $errors[] = ['field' => $field, 'message' => $msg]; };

    $a = [
        'first_name'     => $get('first_name'),
        'last_name'      => $get('last_name'),
        'email'          => strtolower($get('email')),
        'gender'         => null,
        'age'            => null,
        'role'           => strtolower($get('role')),
        'department_id'  => null,
        'section_id'     => null,
        'student_id'     => $get('student_id'),
        'employee_id'    => $get('employee_id'),
        'contact_number' => $get('contact_number'),
    ];

    // Required for everyone
    if ($a['first_name'] === '') $err('first_name', 'First name is required.');
    if ($a['last_name'] === '')  $err('last_name', 'Last name is required.');

    if ($a['email'] === '') {
        $err('email', 'Email address is required.');
    } elseif (!filter_var($a['email'], FILTER_VALIDATE_EMAIL)) {
        $err('email', 'Email address is not valid.');
    }

    $gender = $get('gender');
    if ($gender === '') {
        $err('gender', 'Gender is required.');
    } elseif (!($a['gender'] = normalize_gender($gender))) {
        $err('gender', 'Gender must be Male, Female or Prefer not to say.');
    }

    $age = $get('age');
    if ($age === '') {
        $err('age', 'Age is required.');
    } elseif (!ctype_digit($age) || (int) $age < MIN_AGE || (int) $age > MAX_AGE) {
        $err('age', 'Age must be a whole number from ' . MIN_AGE . ' to ' . MAX_AGE . '.');
    } else {
        $a['age'] = (int) $age;
    }

    if ($a['role'] === '') {
        $err('role', 'Role is required.');
    } elseif (!in_array($a['role'], ROLES, true)) {
        $err('role', 'Role must be Student or Faculty.');
    }

    $deptValue = $get('department_id') !== '' ? $get('department_id') : $get('department');
    $dept = null;
    if ($deptValue === '') {
        $err('department', 'Department is required.');
    } elseif (!($dept = match_department($lookup, $deptValue))) {
        $err('department', 'Department "' . $deptValue . '" does not exist.');
    } else {
        $a['department_id'] = (int) $dept['id'];
    }

    // Students: section and student ID are required
    if ($a['role'] === 'student') {
        $secValue = $get('section_id') !== '' ? $get('section_id') : $get('section');
        if ($secValue === '') {
            $err('section', 'Section is required for students.');
        } elseif (!($sec = match_section($lookup, $secValue))) {
            $err('section', 'Section "' . $secValue . '" does not exist.');
        } elseif ($dept && (int) $sec['department_id'] !== (int) $dept['id']) {
            $err('section', 'Section ' . $sec['name'] . ' is not part of ' . $dept['code'] . '.');
        } else {
            $a['section_id'] = (int) $sec['id'];
        }
        if ($a['student_id'] === '') $err('student_id', 'Student ID is required for students.');
        $a['employee_id'] = '';
    } else {
        $a['student_id'] = '';
    }

    return [$a, $errors];
}

/** Email and student ID must not already be registered. */
function find_duplicates(PDO $pdo, array $a): array
{
    $errors = [];
    if ($a['email'] !== '') {
        $q = $pdo->prepare('SELECT 1 FROM users WHERE lower(email) = lower(:e) LIMIT 1');
        $q->execute([':e' => $a['email']]);
        if ($q->fetch()) $errors[] = ['field' => 'email', 'message' => 'Email address is already registered.'];
    }
    if ($a['student_id'] !== '') {
        $q = $pdo->prepare('SELECT 1 FROM users WHERE student_id = :s LIMIT 1');
        $q->execute([':s' => $a['student_id']]);
        if ($q->fetch()) $errors[] = ['field' => 'student_id', 'message' => 'Student ID is already registered.'];
    }
    return $errors;
}

/** Makes a unique username from the email, e.g. juan.delacruz, juan.delacruz2 ... */
function unique_username(PDO $pdo, string $email): string
{
    $base = preg_replace('/[^a-z0-9._]/', '', strtolower(explode('@', $email)[0])) ?: 'user';
    $base = substr($base, 0, 40);
    $q = $pdo->prepare('SELECT 1 FROM users WHERE lower(username) = :u LIMIT 1');
    for ($i = 1; $i < 1000; $i++) {
        $candidate = $i === 1 ? $base : $base . $i;
        $q->execute([':u' => $candidate]);
        if (!$q->fetch()) return $candidate;
    }
    return $base . bin2hex(random_bytes(3));
}

/**
 * Saves the account. $mode is "link" (password setup link, recommended)
 * or "temporary" (temporary password). Either way the user must set a new
 * password before using the account (must_change_password = true).
 *
 * @return array ['user' => [...], 'temporary_password' => ?, 'setup_link' => ?]
 */
function create_account(PDO $pdo, array $a, string $mode, int $adminId): array
{
    global $config;

    $pdo->beginTransaction();
    try {
        $username = unique_username($pdo, $a['email']);
        $temporaryPassword = null;
        if ($mode === 'temporary') {
            $temporaryPassword = random_password(10);
            $hash = password_hash($temporaryPassword, PASSWORD_DEFAULT);
        } else {
            // Nobody knows this password – the user picks one through the link.
            $hash = password_hash(bin2hex(random_bytes(24)), PASSWORD_DEFAULT);
        }

        $ins = $pdo->prepare('
            INSERT INTO users (first_name, last_name, student_id, username, email, contact_number,
                               password_hash, role, section_id, gender, age, department_id,
                               employee_id, must_change_password, created_by)
            VALUES (:first_name, :last_name, :student_id, :username, :email, :contact_number,
                    :password_hash, :role, :section_id, :gender, :age, :department_id,
                    :employee_id, TRUE, :created_by)
            RETURNING id
        ');
        $ins->execute([
            ':first_name'     => $a['first_name'],
            ':last_name'      => $a['last_name'],
            ':student_id'     => $a['student_id'] !== '' ? $a['student_id'] : null,
            ':username'       => $username,
            ':email'          => $a['email'],
            ':contact_number' => $a['contact_number'] !== '' ? $a['contact_number'] : null,
            ':password_hash'  => $hash,
            ':role'           => $a['role'],
            ':section_id'     => $a['section_id'],
            ':gender'         => $a['gender'],
            ':age'            => $a['age'],
            ':department_id'  => $a['department_id'],
            ':employee_id'    => $a['employee_id'] !== '' ? $a['employee_id'] : null,
            ':created_by'     => $adminId ?: null,
        ]);
        $userId = (int) $ins->fetchColumn();

        // Faculty also go into the "faculty" list that the schedule form uses.
        if ($a['role'] === 'faculty') {
            $pdo->exec('SAVEPOINT add_faculty');
            try {
                $f = $pdo->prepare('INSERT INTO faculty (first_name, last_name, user_id, email) VALUES (:f, :l, :u, :e)');
                $f->execute([':f' => $a['first_name'], ':l' => $a['last_name'], ':u' => $userId, ':e' => $a['email']]);
                $pdo->exec('RELEASE SAVEPOINT add_faculty');
            } catch (PDOException $e) {
                // Your faculty table may have other required columns – the account is still created.
                $pdo->exec('ROLLBACK TO SAVEPOINT add_faculty');
            }
        }

        $setupLink = null;
        if ($mode !== 'temporary') {
            $setupLink = new_setup_link($pdo, $userId);
        }

        $pdo->commit();
    } catch (Throwable $e) {
        $pdo->rollBack();
        throw $e;
    }

    $user = $a;
    $user['id'] = $userId;
    $user['username'] = $username;
    return ['user' => $user, 'temporary_password' => $temporaryPassword, 'setup_link' => $setupLink];
}

/** Creates a one-time password setup link and returns it. */
function new_setup_link(PDO $pdo, int $userId): string
{
    global $config;
    // Old unused links for this user stop working.
    $pdo->prepare('UPDATE password_tokens SET used_at = now() WHERE user_id = :u AND used_at IS NULL')
        ->execute([':u' => $userId]);

    $token = bin2hex(random_bytes(32));
    $pdo->prepare("INSERT INTO password_tokens (user_id, token_hash, expires_at)
                   VALUES (:u, :h, now() + (:hours || ' hours')::interval)")
        ->execute([':u' => $userId, ':h' => hash('sha256', $token), ':hours' => (string) $config['setup_link_hours']]);

    return $config['admin_web_url'] . '/set-password.html?token=' . $token;
}

/** Password rule used everywhere: 8+ characters with letters and numbers. */
function password_problem(string $pw): ?string
{
    if (strlen($pw) < 8) return 'Password must be at least 8 characters.';
    if (!preg_match('/[A-Za-z]/', $pw) || !preg_match('/[0-9]/', $pw)) {
        return 'Password must contain both letters and numbers.';
    }
    return null;
}

/** Sends the "account created" email. Returns [sent(bool), error(string|null)]. */
function send_account_email(Mailer $mailer, array $user, array $lookup, ?string $temporaryPassword, ?string $setupLink): array
{
    global $config;
    $e = fn($s) => htmlspecialchars((string) $s, ENT_QUOTES, 'UTF-8');

    $dept = null;
    foreach ($lookup['departments'] as $d) if ((int) $d['id'] === (int) $user['department_id']) $dept = $d;
    $section = null;
    foreach ($lookup['sections'] as $s) if ((int) $s['id'] === (int) $user['section_id']) $section = $s;

    $details = [
        'Name'       => $user['first_name'] . ' ' . $user['last_name'],
        'Email'      => $user['email'],
        'Username'   => $user['username'],
        'Role'       => ucfirst($user['role']),
        'Department' => $dept ? department_label($dept) : '',
    ];
    if ($user['role'] === 'student') {
        $details['Section'] = $section['name'] ?? '';
        $details['Student ID'] = $user['student_id'];
    } elseif (!empty($user['employee_id'])) {
        $details['Employee ID'] = $user['employee_id'];
    }

    $rowsHtml = '';
    $rowsText = '';
    foreach ($details as $label => $value) {
        $rowsHtml .= '<tr><td style="padding:6px 12px 6px 0;color:#777">' . $e($label) . '</td>'
                   . '<td style="padding:6px 0;font-weight:bold">' . $e($value) . '</td></tr>';
        $rowsText .= "  $label: $value\n";
    }

    $hours = $config['setup_link_hours'];
    if ($setupLink) {
        $actionHtml = '<p>To start using your account, set your own password using the button below. '
            . 'This link works once and expires in ' . $hours . ' hours.</p>'
            . '<p style="margin:24px 0"><a href="' . $e($setupLink) . '" style="background:#800020;color:#fff;'
            . 'padding:12px 22px;border-radius:6px;text-decoration:none;font-weight:bold">Set my password</a></p>'
            . '<p style="font-size:13px;color:#777">If the button does not work, copy this link into your browser:<br>'
            . $e($setupLink) . '</p>';
        $actionText = "Set your password (link works once, expires in $hours hours):\n$setupLink\n";
    } else {
        $actionHtml = '<p>Your temporary password is:</p>'
            . '<p style="font-size:20px;font-weight:bold;letter-spacing:1px;background:#f8e9ed;padding:10px 14px;'
            . 'border-radius:6px;display:inline-block">' . $e($temporaryPassword) . '</p>'
            . '<p><strong>You will be asked to change this password the first time you log in.</strong></p>';
        $actionText = "Temporary password: $temporaryPassword\nYou will be asked to change it the first time you log in.\n";
    }

    $html = '<div style="font-family:Arial,Helvetica,sans-serif;color:#202124;max-width:560px">'
        . '<h2 style="color:#800020;margin-bottom:4px">UNIROOM</h2>'
        . '<p style="color:#777;margin-top:0">University Room Management System</p>'
        . '<p>Hello ' . $e($user['first_name']) . ',</p>'
        . '<p>An administrator has created your UNIROOM account. Here are your account details:</p>'
        . '<table style="border-collapse:collapse;margin:12px 0">' . $rowsHtml . '</table>'
        . $actionHtml
        . '<p style="font-size:13px;color:#777;margin-top:28px">Never share your password with anyone. '
        . 'If you did not expect this email, please contact your school administrator.</p>'
        . '</div>';

    $text = "Hello {$user['first_name']},\n\nAn administrator has created your UNIROOM account.\n\n"
        . "Account details:\n$rowsText\n$actionText\n"
        . "Never share your password with anyone. If you did not expect this email, contact your school administrator.\n";

    $ok = $mailer->send($user['email'], $details['Name'], 'Your UNIROOM account has been created', $html, $text);
    return [$ok, $ok ? null : $mailer->lastError];
}
