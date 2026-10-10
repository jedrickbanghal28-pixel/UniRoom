<?php
/*
 * Departments and sections for the Account Creation form (admin only).
 * GET → { departments: [...], sections: [...], email_configured: bool }
 */
require_once __DIR__ . '/_lib/db.php';
require_once __DIR__ . '/_lib/accounts.php';

require_role(['admin']);

try {
    $lookup = load_lookup($pdo);
    $departments = array_map(fn($d) => [
        'id' => (int) $d['id'],
        'code' => $d['code'],
        'name' => $d['name'] ?? null,
        'label' => department_label($d),
    ], $lookup['departments']);

    $mailer = new Mailer($config['smtp']);

    echo json_encode([
        'success' => true,
        'departments' => $departments,
        'sections' => array_map(fn($s) => [
            'id' => (int) $s['id'],
            'name' => $s['name'],
            'department_id' => (int) $s['department_id'],
        ], $lookup['sections']),
        'genders' => GENDERS,
        'age_range' => [MIN_AGE, MAX_AGE],
        'email_configured' => $mailer->isConfigured(),
    ]);
} catch (PDOException $e) {
    respond(500, ['success' => false, 'message' => 'Database error.']);
}
