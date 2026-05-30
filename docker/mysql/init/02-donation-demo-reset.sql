-- Reset MealBridge donation demo seed (local dev).
-- Run against mealbridge_donation after stopping donation-service, then restart it.
-- Example:
--   mysql -h 127.0.0.1 -P 13306 -u mealbridge -pmealbridge mealbridge_donation < 02-donation-demo-reset.sql

USE mealbridge_donation;

DELETE FROM tracking_events
WHERE session_id IN (
  SELECT id FROM meal_sessions WHERE notes LIKE 'DUMMY%'
);

DELETE FROM meal_line_items
WHERE session_id IN (
  SELECT id FROM meal_sessions WHERE notes LIKE 'DUMMY%'
);

DELETE FROM meal_sessions
WHERE notes LIKE 'DUMMY%';

DELETE FROM meal_plans
WHERE notes LIKE 'DUMMY%';

-- Optional: wipe all donation demo tables for a completely clean slate
-- TRUNCATE TABLE tracking_events;
-- TRUNCATE TABLE meal_line_items;
-- TRUNCATE TABLE meal_sessions;
-- TRUNCATE TABLE meal_plans;
