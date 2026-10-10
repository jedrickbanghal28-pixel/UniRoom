<?php
/*
 * Quick check that the API is deployed and can reach Supabase.
 * Open https://<your-api>.vercel.app/ or /api/health.php in a browser.
 */
require_once __DIR__ . '/_lib/db.php';
require_once __DIR__ . '/_lib/mailer.php';

$pdo->query('SELECT 1');
$mailer = new Mailer($config['smtp']);

echo json_encode([
    'success' => true,
    'message' => 'UNIROOM API is running and connected to the database.',
    'email_configured' => $mailer->isConfigured(),
]);
