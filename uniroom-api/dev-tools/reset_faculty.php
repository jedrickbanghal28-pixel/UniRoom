<?php

require_once "db.php";

$password = "123456";

$hash = password_hash(
    $password,
    PASSWORD_DEFAULT
);

$stmt = $pdo->prepare("
    UPDATE users
    SET password_hash = :password_hash
    WHERE username = 'faculty001'
");

$stmt->execute([
    ":password_hash" => $hash
]);

echo "Faculty password reset successfully.";

?>