-- MySQL 데이터베이스 및 사용자 생성 스크립트
-- MySQL에 root로 접속한 후 실행하세요
-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS streaming_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 사용자 생성 및 권한 부여
CREATE USER IF NOT EXISTS 'admin' @'localhost' IDENTIFIED BY '1234';
GRANT ALL PRIVILEGES ON streaming_db.* TO 'admin' @'localhost';
FLUSH PRIVILEGES;
-- 데이터베이스 선택
USE streaming_db;
-- 테이블이 자동으로 생성되므로 별도 테이블 생성은 필요하지 않습니다.
-- Spring Boot JPA가 자동으로 테이블을 생성합니다.