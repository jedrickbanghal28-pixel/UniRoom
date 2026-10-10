<?php
/*
 * Bulk account creation (admin only).
 *
 * The website reads the CSV / Excel file and sends the rows here in small
 * batches (so each request finishes quickly on Vercel).
 *
 * POST {
 *   rows: [ { row_number, first_name, last_name, email, gender, age, role,
 *             department, section, student_id, employee_id, contact_number }, ... ],
 *   password_mode: "link" | "temporary",
 *   validate_only: true   → only check the rows, create nothing
 * }
 *
 * → { results: [ { row_number, status: "valid"|"created"|"invalid"|"failed",
 *                  errors: [...], username?, email_sent?, email_error? } ],
 *     summary: { total, created, invalid, failed, emails_sent } }
 */
require_once __DIR__ . '/_lib/db.php';
require_once __DIR__ . '/_lib/accounts.php';

const MAX_ROWS_PER_REQUEST = 50;

$admin = require_role(['admin']);
$data = body();
$rows = $data['rows'] ?? [];
$mode = ($data['password_mode'] ?? 'link') === 'temporary' ? 'temporary' : 'link';
$validateOnly = !empty($data['validate_only']);

if (!is_array($rows) || count($rows) === 0) {
    respond(400, ['success' => false, 'message' => 'The file has no account rows.']);
}
if (count($rows) > MAX_ROWS_PER_REQUEST) {
    respond(400, ['success' => false, 'message' => 'Send at most ' . MAX_ROWS_PER_REQUEST . ' rows per request.']);
}

try {
    $lookup = load_lookup($pdo);
    $mailer = null;
    $results = [];
    $seenEmails = [];
    $seenStudentIds = [];
    $summary = ['total' => count($rows), 'valid' => 0, 'created' => 0, 'invalid' => 0, 'failed' => 0, 'emails_sent' => 0];

    foreach ($rows as $i => $raw) {
        $rowNumber = (int) ($raw['row_number'] ?? ($i + 2));
        [$account, $errors] = validate_account(is_array($raw) ? $raw : [], $lookup);

        // Duplicates inside this batch (the website also checks the whole file)
        if ($account['email'] !== '') {
            if (isset($seenEmails[$account['email']])) {
                $errors[] = ['field' => 'email', 'message' => 'Duplicate email – also used in row ' . $seenEmails[$account['email']] . '.'];
            } else {
                $seenEmails[$account['email']] = $rowNumber;
            }
        }
        if ($account['student_id'] !== '') {
            if (isset($seenStudentIds[$account['student_id']])) {
                $errors[] = ['field' => 'student_id', 'message' => 'Duplicate student ID – also used in row ' . $seenStudentIds[$account['student_id']] . '.'];
            } else {
                $seenStudentIds[$account['student_id']] = $rowNumber;
            }
        }

        // Already in the database?
        $errors = array_merge($errors, find_duplicates($pdo, $account));

        if ($errors) {
            $results[] = ['row_number' => $rowNumber, 'status' => 'invalid', 'errors' => $errors];
            $summary['invalid']++;
            continue;
        }

        if ($validateOnly) {
            $results[] = ['row_number' => $rowNumber, 'status' => 'valid', 'errors' => []];
            $summary['valid']++;
            continue;
        }

        try {
            $created = create_account($pdo, $account, $mode, (int) $admin['uid']);
        } catch (PDOException $e) {
            $msg = $e->getCode() === '23505' ? 'Email or student ID is already registered.' : 'Database error while saving.';
            $results[] = ['row_number' => $rowNumber, 'status' => 'failed', 'errors' => [['field' => '', 'message' => $msg]]];
            $summary['failed']++;
            continue;
        }

        if ($mailer === null) $mailer = new Mailer($config['smtp']);   // one connection for the whole batch
        [$sent, $mailError] = send_account_email($mailer, $created['user'], $lookup, $created['temporary_password'], $created['setup_link']);

        $row = [
            'row_number' => $rowNumber,
            'status' => 'created',
            'errors' => [],
            'username' => $created['user']['username'],
            'email' => $created['user']['email'],
            'email_sent' => $sent,
        ];
        if (!$sent) {
            $row['email_error'] = $mailError;
            if ($created['temporary_password']) $row['temporary_password'] = $created['temporary_password'];
            if ($created['setup_link']) $row['setup_link'] = $created['setup_link'];
        }
        $results[] = $row;
        $summary['created']++;
        if ($sent) $summary['emails_sent']++;
    }

    if ($mailer) $mailer->close();

    echo json_encode(['success' => true, 'results' => $results, 'summary' => $summary]);

} catch (PDOException $e) {
    respond(500, ['success' => false, 'message' => 'Database error.']);
}
