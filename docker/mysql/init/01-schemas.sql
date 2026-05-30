-- One MySQL instance, one schema (database) per microservice
CREATE DATABASE IF NOT EXISTS mealbridge_user
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS mealbridge_donation
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS mealbridge_matching
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS mealbridge_ai
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON mealbridge_user.* TO 'mealbridge'@'%';
GRANT ALL PRIVILEGES ON mealbridge_donation.* TO 'mealbridge'@'%';
GRANT ALL PRIVILEGES ON mealbridge_matching.* TO 'mealbridge'@'%';
GRANT ALL PRIVILEGES ON mealbridge_ai.* TO 'mealbridge'@'%';
FLUSH PRIVILEGES;
