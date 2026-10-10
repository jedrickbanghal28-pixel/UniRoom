<?php

header("Content-Type: application/json");

require_once __DIR__ . '/_lib/db.php';

/** @var PDO $pdo */

$data = json_decode(
    file_get_contents("php://input"),
    true
);

$data = is_array($data) ? $data : [];

$sectionId = (int) ($data["section_id"] ?? 0);
$roomId = (int) ($data["room_id"] ?? 0);

if ($sectionId <= 0 || $roomId <= 0) {

    http_response_code(400);

    echo json_encode([
        "success" => false,
        "message" => "Section and room are required."
    ]);

    exit;
}

try {

    $checkSection = $pdo->prepare("
        SELECT id
        FROM sections
        WHERE id = :section_id
    ");

    $checkSection->execute([
        ":section_id" => $sectionId
    ]);

    if (!$checkSection->fetch()) {

        http_response_code(404);

        echo json_encode([
            "success" => false,
            "message" => "Section not found."
        ]);

        exit;
    }


    $checkRoom = $pdo->prepare("
        SELECT id
        FROM rooms
        WHERE id = :room_id
    ");

    $checkRoom->execute([
        ":room_id" => $roomId
    ]);

    if (!$checkRoom->fetch()) {

        http_response_code(404);

        echo json_encode([
            "success" => false,
            "message" => "Room not found."
        ]);

        exit;
    }


    $stmt = $pdo->prepare("
        INSERT INTO section_rooms (
            section_id,
            room_id
        )
        VALUES (
            :section_id,
            :room_id
        )
        ON CONFLICT (section_id)
        DO UPDATE SET
            room_id = EXCLUDED.room_id
    ");

    $stmt->execute([
        ":section_id" => $sectionId,
        ":room_id" => $roomId
    ]);


    echo json_encode([
        "success" => true,
        "message" => "Room assigned successfully."
    ]);

} catch (PDOException $e) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Database error."
    ]);

}

?>