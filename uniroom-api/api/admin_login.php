<?php

header("Content-Type: application/json");

require_once __DIR__ . '/_lib/db.php';
require_once __DIR__ . '/_lib/auth.php';

/*
 * Read JSON request
 */
$data = json_decode(
    file_get_contents("php://input"),
    true
);

$email = trim($data["email"] ?? "");
$password = $data["password"] ?? "";

/*
 * Check required fields
 */
if ($email === "" || $password === "") {

    http_response_code(400);

    echo json_encode([
        "success" => false,
        "message" => "Please enter your email and password."
    ]);

    exit;
}

try {

    /*
     * Find the user by email
     */
    $stmt = $pdo->prepare("
        SELECT
            id,
            first_name,
            last_name,
            student_id,
            username,
            email,
            contact_number,
            password_hash,
            role,
            section_id,
            must_change_password
        FROM users
        WHERE lower(email) = lower(:email)
        LIMIT 1
    ");

    $stmt->execute([
        ":email" => $email
    ]);

    $user = $stmt->fetch(PDO::FETCH_ASSOC);

    /*
     * User not found
     */
    if (!$user) {

        http_response_code(401);

        echo json_encode([
            "success" => false,
            "message" => "Invalid email or password."
        ]);

        exit;
    }

    /*
     * Verify password
     */
    if (!password_verify($password, $user["password_hash"])) {

        http_response_code(401);

        echo json_encode([
            "success" => false,
            "message" => "Invalid email or password."
        ]);

        exit;
    }

    /*
     * Only Admin and Faculty can access
     * the admin/faculty website.
     */
    if (
        $user["role"] !== "admin" &&
        $user["role"] !== "faculty"
    ) {

        http_response_code(403);

        echo json_encode([
            "success" => false,
            "message" => "You do not have permission to access this page."
        ]);

        exit;
    }

    /*
     * Do not send the password hash
     * back to the website.
     */
    unset($user["password_hash"]);

    $user["must_change_password"] = (bool) $user["must_change_password"];

    /*
     * Login successful.
     * "token" must be sent back in the Authorization header for admin pages.
     * If must_change_password is true the website asks for a new password first.
     */
    echo json_encode([
        "success" => true,
        "message" => "Login successful.",
        "user" => $user,
        "token" => make_token($user),
        "must_change_password" => $user["must_change_password"]
    ]);

} catch (PDOException $e) {

    /*
     * Database error
     */
    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Database error."
    ]);

}

?>