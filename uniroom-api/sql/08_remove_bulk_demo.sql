-- =====================================================================
-- UNIROOM – remove the accounts created from the demo bulk files
-- (uniroom-admin/templates/demo_bulk_accounts.csv / .xlsx), so the bulk
-- upload can be shown again from the start. Run in Supabase → SQL Editor.
-- The demo accounts from 06 and 07 (demo.*) are NOT removed.
-- =====================================================================

DELETE FROM faculty
WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'uniroomsystem+%@gmail.com');

DELETE FROM users
WHERE email LIKE 'uniroomsystem+%@gmail.com';

SELECT COUNT(*) AS demo_bulk_accounts_left FROM users WHERE email LIKE 'uniroomsystem+%@gmail.com';
