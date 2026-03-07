-- Create database and a dedicated user (edit credentials as needed).
CREATE DATABASE IF NOT EXISTS markdown_editor
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'md_user'@'%' IDENTIFIED BY 'md_pass';
GRANT ALL PRIVILEGES ON markdown_editor.* TO 'md_user'@'%';
FLUSH PRIVILEGES;

