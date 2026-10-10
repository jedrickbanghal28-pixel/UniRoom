-- =====================================================
-- 04 – Home room for each section (optional)
-- =====================================================
-- The Android app's student dashboard shows "My Room" from
-- the section_rooms table. Without a row here, students see
-- "No room has been assigned to your section yet."
--
-- Change the rooms below if your sections use other rooms,
-- or assign them later from the admin website (Assign Room).
-- Safe to run more than once.

INSERT INTO section_rooms (section_id, room_id)
SELECT s.id, r.id
FROM (VALUES ('BSIT-A1', 'TH 308'),
             ('BSMT-A1', 'TH 305'),
             ('BSMT-A2', 'TH 306')) AS v(section_name, room_name)
JOIN sections s ON s.name = v.section_name
JOIN rooms r ON r.room_name = v.room_name
ON CONFLICT (section_id) DO UPDATE SET room_id = EXCLUDED.room_id;
