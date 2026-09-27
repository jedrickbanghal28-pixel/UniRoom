<?php

header("Content-Type: application/json");

require_once "db.php";

$userId = (int)($_GET["user_id"] ?? 0);

if ($userId <= 0) {
    http_response_code(400);
    echo json_encode([
        "success" => false,
        "message" => "User ID is required."
    ]);
    exit;
}

try {

    $stmt = $pdo->prepare("
        SELECT
            s.id AS section_id,
            s.name AS section_name,
            r.id AS room_id,
            r.room_name,
            r.building,
            r.floor,
            r.capacity,
            r.status
        FROM users u
        JOIN sections s
            ON s.id = u.section_id
        LEFT JOIN section_rooms sr
            ON sr.section_id = s.id
        LEFT JOIN rooms r
            ON r.id = sr.room_id
        WHERE u.id = :user_id
          AND u.role = 'student'
        LIMIT 1
    ");

    $stmt->execute([
        ":user_id" => $userId
    ]);

    $room = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$room) {
        http_response_code(404);
        echo json_encode([
            "success" => false,
            "message" => "Student account not found."
        ]);
        exit;
    }

    if ($room["room_id"] === null) {
        echo json_encode([
            "success" => false,
            "message" => "No room has been assigned to your section yet."
        ]);
        exit;
    }

    echo json_encode([
        "success" => true,
        "message" => "Assigned room loaded.",
        "room" => $room
    ]);

} catch (PDOException $e) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Database error."
    ]);
}

?>
