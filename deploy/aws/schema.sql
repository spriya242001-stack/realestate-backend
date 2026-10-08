CREATE DATABASE IF NOT EXISTS realestate_backend_db;
USE realestate_backend_db;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    role ENUM('ROLE_ADMIN', 'ROLE_CUSTOMER', 'ROLE_USER') NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS properties (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    description TEXT,
    price DOUBLE,
    type ENUM('RENT', 'SALE'),
    location VARCHAR(255),
    image_url VARCHAR(255),
    approved BIT NOT NULL,
    owner_id BIGINT NOT NULL,
    date_listed DATETIME(6),
    CONSTRAINT fk_properties_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);
