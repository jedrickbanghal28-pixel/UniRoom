<?php

require_once __DIR__ . '/../lib/db.php';

if (!function_exists('body')) {
    function body(): array
    {
        $input = file_get_contents('php://input');
        $data = json_decode($input, true);

        return is_array($data) ? $data : [];
    }
}

$pdo = $pdo ?? null;

if (!$pdo instanceof PDO) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Database connection is not available.'
    ]);
    exit;
}

header("Content-Type: application/json");

$data = body();

/*
| The Android app sends the section name ("BSIT-A1", "BSMT-A2"). Turn it
| into section_id + department_id so the student lands in the right program.
*/
$sectionRow = null;
try {
    if (trim((string)($data['section_id'] ?? '')) !== '') {
        $q = $pdo->prepare('SELECT id, department_id FROM sections WHERE id = :v');
        $q->execute([':v' => (int)$data['section_id']]);
    } else {
        $q = $pdo->prepare('SELECT id, department_id FROM sections WHERE lower(name) = lower(:v)');
        $q->execute([':v' => trim((string)($data['section_name'] ?? ($data['section'] ?? '')))]);
    }
    $sectionRow = $q->fetch(PDO::FETCH_ASSOC) ?: null;
} catch (PDOException $e) {
    $sectionRow = null;
}
if ($sectionRow) {
    $data['section_id'] = (int)$sectionRow['id'];
} elseif (trim((string)($data['section_name'] ?? ($data['section'] ?? $data['section_id'] ?? ''))) !== '') {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'That section does not exist.']);
    exit;
}

/*
|--------------------------------------------------------------------------
| Required Fields
|--------------------------------------------------------------------------
*/

$requiredFields = [
    'first_name',
    'last_name',
    'student_id',
    'username',
    'email',
    'contact_number',
    'password',
    'section_id'
];

foreach ($requiredFields as $field) {

    if (
        !isset($data[$field]) ||
        trim((string)$data[$field]) === ''
    ) {

        http_response_code(400);

        echo json_encode([
            'success' => false,
            'message' => ucwords(
                str_replace('_', ' ', $field)
            ) . ' is required.'
        ]);

        exit;
    }
}


/*
|--------------------------------------------------------------------------
| Validate Email
|--------------------------------------------------------------------------
*/

if (!filter_var($data['email'], FILTER_VALIDATE_EMAIL)) {

    http_response_code(400);

    echo json_encode([
        'success' => false,
        'message' => 'Invalid email address.'
    ]);

    exit;
}


/*
|--------------------------------------------------------------------------
| Validate Password
|--------------------------------------------------------------------------
*/

if (strlen($data['password']) < 8
    || !preg_match('/[A-Za-z]/', $data['password'])
    || !preg_match('/[0-9]/', $data['password'])) {

    http_response_code(400);

    echo json_encode([
        'success' => false,
        'message' => 'Password must be at least 8 characters with letters and numbers.'
    ]);

    exit;
}


/*
|--------------------------------------------------------------------------
| Database Operations
|--------------------------------------------------------------------------
*/

try {

    /*
    |----------------------------------------------------------------------
    | Check Duplicate Username, Email, or Student ID
    |----------------------------------------------------------------------
    */

    $check = $pdo->prepare("
        SELECT
            username,
            email,
            student_id
        FROM users
        WHERE
            LOWER(username) = LOWER(:username)
            OR LOWER(email) = LOWER(:email)
            OR student_id = :student_id
        LIMIT 1
    ");

    $check->execute([
        ':username' => trim($data['username']),
        ':email' => trim($data['email']),
        ':student_id' => trim($data['student_id'])
    ]);

    $existingUser = $check->fetch(PDO::FETCH_ASSOC);


    /*
    |----------------------------------------------------------------------
    | If User Already Exists
    |----------------------------------------------------------------------
    */

    if ($existingUser) {

        if (
            strcasecmp(
                $existingUser['username'],
                trim($data['username'])
            ) === 0
        ) {

            $message = 'Username already exists.';

        } elseif (
            strcasecmp(
                $existingUser['email'],
                trim($data['email'])
            ) === 0
        ) {

            $message = 'Email address already exists.';

        } else {

            $message = 'Student ID already exists.';
        }


        http_response_code(409);

        echo json_encode([
            'success' => false,
            'message' => $message
        ]);

        exit;
    }


    /*
    |----------------------------------------------------------------------
    | Insert Student
    |----------------------------------------------------------------------
    */

    $insert = $pdo->prepare("
        INSERT INTO users (
            first_name,
            last_name,
            student_id,
            username,
            email,
            contact_number,
            password_hash,
            section_id,
            department_id,
            role
        )
        VALUES (
            :first_name,
            :last_name,
            :student_id,
            :username,
            :email,
            :contact_number,
            :password_hash,
            :section_id,
            :department_id,
            'student'
        )
        RETURNING id
    ");


    $insert->execute([

        ':first_name' =>
            trim($data['first_name']),

        ':last_name' =>
            trim($data['last_name']),

        ':student_id' =>
            trim($data['student_id']),

        ':username' =>
            trim($data['username']),

        ':email' =>
            trim($data['email']),

        ':contact_number' =>
            trim($data['contact_number']),

        ':password_hash' =>
            password_hash(
                $data['password'],
                PASSWORD_DEFAULT
            ),

        ':section_id' =>
            (int)$data['section_id'],

        ':department_id' =>
            $sectionRow ? (int)$sectionRow['department_id'] : null
    ]);


    /*
    |----------------------------------------------------------------------
    | Get New User ID
    |----------------------------------------------------------------------
    */

    $userId = $insert->fetchColumn();


    /*
    |----------------------------------------------------------------------
    | Registration Successful
    |----------------------------------------------------------------------
    */

    echo json_encode([
        'success' => true,
        'message' => 'Registration successful.',
        'user_id' => $userId
    ]);


} catch (PDOException $e) {

    /*
    |----------------------------------------------------------------------
    | Database Error
    |----------------------------------------------------------------------
    */

    http_response_code(500);

    echo json_encode([
        'success' => false,
        'message' => 'Registration failed. Please try again.'
    ]);
}

?>