<?php

header("Content-Type: application/json");

require_once __DIR__ . '/_lib/db.php';

/** @var PDO $pdo */

try {

    $sections = $pdo->query("
        SELECT id, name
        FROM sections
        ORDER BY name
    ")->fetchAll(PDO::FETCH_ASSOC);


    $subjects = $pdo->query("
        SELECT id, code, name
        FROM subjects
        ORDER BY code
    ")->fetchAll(PDO::FETCH_ASSOC);


    $faculty = $pdo->query("
        SELECT id, first_name, last_name
        FROM faculty
        ORDER BY last_name, first_name
    ")->fetchAll(PDO::FETCH_ASSOC);


    $rooms = $pdo->query("
        SELECT id, room_name, building, floor
        FROM rooms
        ORDER BY room_name
    ")->fetchAll(PDO::FETCH_ASSOC);


    echo json_encode([
        "success" => true,
        "sections" => $sections,
        "subjects" => $subjects,
        "faculty" => $faculty,
        "rooms" => $rooms
    ]);

} catch (PDOException $e) {

    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Database error."
    ]);

}

?>