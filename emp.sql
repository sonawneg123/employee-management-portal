-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: emp_portal
-- ------------------------------------------------------
-- Server version	8.0.17

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `attendance`
--

DROP TABLE IF EXISTS `attendance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `employee_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `attendance_date` date NOT NULL,
  `check_in_time` time DEFAULT NULL,
  `check_out_time` time DEFAULT NULL,
  `status` enum('PRESENT','ABSENT','HALF_DAY','WORK_FROM_HOME','ON_LEAVE') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PRESENT',
  `notes` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `created_by` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_by` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_attendance_emp_date` (`employee_id`,`attendance_date`),
  KEY `idx_attendance_employee_id` (`employee_id`),
  KEY `idx_attendance_date_status` (`attendance_date`,`status`),
  CONSTRAINT `fk_att_employee` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance`
--

LOCK TABLES `attendance` WRITE;
/*!40000 ALTER TABLE `attendance` DISABLE KEYS */;
INSERT INTO `attendance` VALUES ('54167e57-38cf-4ba4-8e3a-fcd1f1a89a35','a5899e6e-9718-11f1-bd28-84a93e3ff303','2026-08-13','22:42:33','22:42:35','WORK_FROM_HOME',NULL,'2026-08-13 17:12:32.988344','2026-08-13 17:16:58.975460','sonawneg123@gmail.com','admin@company.com'),('79266631-7ade-4a47-b05c-92315b155580','a5899e6e-9718-11f1-bd28-84a93e3ff303','2026-08-14','00:18:35','11:59:27','ON_LEAVE',NULL,'2026-08-13 18:48:35.238108','2026-08-14 08:21:16.193427','sonawneg123@gmail.com','undareswati87@gmail.com'),('819e74ca-6de7-43e4-9bc9-aa74a1b74a5f','811547bf-d652-47a0-8a46-c25885aee20e','2026-08-13','23:07:51',NULL,'PRESENT',NULL,'2026-08-13 17:37:50.504371','2026-08-13 17:37:50.504371','undareswati87@gmail.com','undareswati87@gmail.com');
/*!40000 ALTER TABLE `attendance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `departments`
--

DROP TABLE IF EXISTS `departments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `departments` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `created_by` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_by` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_dept_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `departments`
--

LOCK TABLES `departments` WRITE;
/*!40000 ALTER TABLE `departments` DISABLE KEYS */;
INSERT INTO `departments` VALUES ('19e19a9b-d304-4cde-8965-19d2d868ebba','R&D','401','2026-08-13 18:42:16.875492','2026-08-13 18:42:16.875492','hr@company.com','hr@company.com'),('75d11684-e291-4120-ab45-5d7250b57813','HR','1111','2026-08-13 07:35:13.282142','2026-08-13 07:35:13.282142','admin@company.com','admin@company.com'),('7cebfdab-5ef9-4d89-a58a-fe21ed21a3d3','sales','1234','2026-08-12 16:40:17.192409','2026-08-12 16:40:17.192409','admin@company.com','admin@company.com'),('cf3ede10-96fd-11f1-bd28-84a93e3ff303','General','GEN','2026-08-13 15:30:27.000000','2026-08-13 15:30:27.000000',NULL,NULL);
/*!40000 ALTER TABLE `departments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `employees`
--

DROP TABLE IF EXISTS `employees`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employees` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `first_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `employee_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `department_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `job_title` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `date_of_joining` date NOT NULL,
  `salary` decimal(15,2) NOT NULL DEFAULT '0.00',
  `status` enum('ACTIVE','INACTIVE','ON_LEAVE','TERMINATED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `created_by` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_by` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_emp_code` (`employee_code`),
  UNIQUE KEY `uq_emp_user` (`user_id`),
  KEY `idx_employees_user_id` (`user_id`),
  KEY `idx_employees_department_id` (`department_id`),
  KEY `idx_employees_status` (`status`),
  KEY `idx_employees_dept_status` (`department_id`,`status`),
  CONSTRAINT `fk_emp_dept` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_emp_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `employees`
--

LOCK TABLES `employees` WRITE;
/*!40000 ALTER TABLE `employees` DISABLE KEYS */;
INSERT INTO `employees` VALUES ('1c7ea740-7b32-432e-b4f2-683e4cf97072',NULL,'nikhil','Sonawane','34533665','7cebfdab-5ef9-4d89-a58a-fe21ed21a3d3','Software Engineer','08530574106','tal newasa dist ahmednagar','2026-07-29',1312423523.00,'ON_LEAVE','2026-08-13 17:21:06.448139','2026-08-13 17:21:06.448139','admin@company.com','admin@company.com'),('811547bf-d652-47a0-8a46-c25885aee20e','5ae0f1c6-2e02-42f1-9bea-ed8c1810e59a','swati','undare','REG-0000025552','cf3ede10-96fd-11f1-bd28-84a93e3ff303','Employee',NULL,NULL,'2026-08-13',0.00,'ACTIVE','2026-08-13 17:37:30.470248','2026-08-13 17:37:30.470248','SYSTEM','SYSTEM'),('a5899a24-9718-11f1-bd28-84a93e3ff303','26e5f5a5-3310-41c9-8482-64b310c848d1','kk','kk','REG-0842425653','cf3ede10-96fd-11f1-bd28-84a93e3ff303','Employee',NULL,NULL,'2026-08-13',0.00,'ACTIVE','2026-08-13 18:42:33.000000','2026-08-13 18:42:33.000000',NULL,NULL),('a5899bdb-9718-11f1-bd28-84a93e3ff303','3240f0df-b16f-489e-8abe-d3add531ea36','ram','w','REG-0858928176','cf3ede10-96fd-11f1-bd28-84a93e3ff303','Employee',NULL,NULL,'2026-08-13',0.00,'ACTIVE','2026-08-13 18:42:33.000000','2026-08-13 18:42:33.000000',NULL,NULL),('a5899cc1-9718-11f1-bd28-84a93e3ff303','81a017b7-732e-4254-90f4-ead5f77ccf6c','hh','hh','REG-0942760240','cf3ede10-96fd-11f1-bd28-84a93e3ff303','Employee',NULL,NULL,'2026-08-13',0.00,'ACTIVE','2026-08-13 18:42:33.000000','2026-08-13 18:42:33.000000',NULL,NULL),('a5899d90-9718-11f1-bd28-84a93e3ff303','93db21a8-c53a-42fa-8de6-42d2fe1553d0','Gaurav','Sonawane','REG-0959669346','cf3ede10-96fd-11f1-bd28-84a93e3ff303','Employee',NULL,NULL,'2026-08-13',0.00,'ACTIVE','2026-08-13 18:42:33.000000','2026-08-13 18:42:33.000000',NULL,NULL),('a5899e6e-9718-11f1-bd28-84a93e3ff303','b2facd26-cf7a-40f0-90ef-c7af3b89d275','kashif','ali','REG-1647470177','cf3ede10-96fd-11f1-bd28-84a93e3ff303','Employee','08530574105','at navin chandgaon post usthal dumala ta newasa dist ahilyanagar latur','2026-08-13',0.00,'ACTIVE','2026-08-13 18:42:33.000000','2026-08-14 08:02:03.983880',NULL,'sonawneg123@gmail.com'),('a5899f14-9718-11f1-bd28-84a93e3ff303','e818fce5-f62a-4434-9e06-6473a1e13345','Gaurav','Sonawane','REG-1698181432','cf3ede10-96fd-11f1-bd28-84a93e3ff303','Employee',NULL,NULL,'2026-08-13',0.00,'ACTIVE','2026-08-13 18:42:33.000000','2026-08-13 18:42:33.000000',NULL,NULL),('cf4283fa-96fd-11f1-bd28-84a93e3ff303','10000000-0000-0000-0000-000000000004','Demo','Employee','EMP-0001','cf3ede10-96fd-11f1-bd28-84a93e3ff303','Software Engineer','08530574106','at navin chandgaon post usthal dumala ta newasa dist ahilyanagar','2024-01-01',121212.00,'ACTIVE','2026-08-13 15:30:27.000000','2026-08-13 11:54:13.413157',NULL,'hr@company.com'),('d58b58f8-35ea-4ca1-be19-4f4ce86107a5',NULL,'priya','Sonawane','EMP-0001433','cf3ede10-96fd-11f1-bd28-84a93e3ff303','Web Developer Intern','08530574106','tal newasa dist ahmednagar','2026-08-04',213323.00,'ACTIVE','2026-08-14 09:44:00.818998','2026-08-14 09:44:00.818998','admin@company.com','admin@company.com'),('e563bc5c-8727-4295-9206-62c2a8e2f9cc',NULL,'kartik','udmale','EMP-000143','cf3ede10-96fd-11f1-bd28-84a93e3ff303','java developer','9370220056','kanadgaon','2026-08-04',300000.00,'ACTIVE','2026-08-14 08:05:05.170353','2026-08-14 08:05:05.170353','hr@company.com','hr@company.com');
/*!40000 ALTER TABLE `employees` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int(11) NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int(11) DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int(11) NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'1','init schema','SQL','V1__init_schema.sql',-899911478,'root','2026-08-10 08:45:50',588,1),(2,'2','add roles audit columns and indexes','SQL','V2__add_roles_audit_columns_and_indexes.sql',-1893736153,'root','2026-08-10 08:45:50',175,1),(3,'3','fix performance review rating type','SQL','V3__fix_performance_review_rating_type.sql',1220842101,'root','2026-08-10 09:06:45',103,1),(4,'4','fix performance review reviewer id type','SQL','V4__fix_performance_review_reviewer_id_type.sql',94398395,'root','2026-08-10 09:14:14',99,1),(5,'5','add missing columns and indexes','SQL','V5__add_missing_columns_and_indexes.sql',-1495841128,'root','2026-08-10 14:23:42',979,1),(6,'6','fix reviewer id type and add review index','SQL','V6__fix_reviewer_id_type_and_add_review_index.sql',-148872426,'root','2026-08-10 17:44:33',76,1),(7,'7','seed default users','SQL','V7__seed_default_users.sql',1314225677,'emp_user','2026-08-11 11:53:54',115,1),(8,'8','fix seed user role assignments','SQL','V8__fix_seed_user_role_assignments.sql',-556417794,'emp_user','2026-08-11 14:09:53',29,1),(9,'9','seed employee account','SQL','V9__seed_employee_account.sql',184524612,'root','2026-08-13 10:00:27',74,1),(10,'10','backfill employee records','SQL','V10__backfill_employee_records.sql',1570580860,'root','2026-08-13 13:12:33',37,1),(11,'11','ensure gen department and fix employee records','SQL','V11__ensure_gen_department_and_fix_employee_records.sql',488111665,'root','2026-08-13 13:12:33',6,1),(12,'12','add employee name columns','SQL','V12__add_employee_name_columns.sql',-203904675,'root','2026-08-13 15:43:30',196,0),(13,'12','add employee name columns','JDBC','db.migration.V12__add_employee_name_columns',NULL,'emp_user','2026-08-13 16:44:15',33,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `leave_requests`
--

DROP TABLE IF EXISTS `leave_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `leave_requests` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `employee_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `leave_type` enum('ANNUAL','SICK','MATERNITY','PATERNITY','UNPAID','OTHER','EMERGENCY','STUDY') COLLATE utf8mb4_unicode_ci NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `rejection_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `attachment_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('PENDING','APPROVED','REJECTED','CANCELLED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `reviewed_by` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `created_by` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_by` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_leave_requests_employee_id` (`employee_id`),
  KEY `idx_leave_requests_status` (`status`),
  KEY `idx_leave_requests_dates` (`start_date`,`end_date`),
  KEY `idx_leave_requests_emp_status` (`employee_id`,`status`),
  CONSTRAINT `fk_lr_employee` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `leave_requests`
--

LOCK TABLES `leave_requests` WRITE;
/*!40000 ALTER TABLE `leave_requests` DISABLE KEYS */;
INSERT INTO `leave_requests` VALUES ('6497b6b9-b9fa-4902-a2d3-41b7eea48fe7','811547bf-d652-47a0-8a46-c25885aee20e','MATERNITY','2026-08-29','2026-12-10','',NULL,NULL,'APPROVED','10000000-0000-0000-0000-000000000001','2026-08-13 17:40:48.265332','2026-08-13 17:38:25.633784','2026-08-13 17:40:48.533087','undareswati87@gmail.com','admin@company.com'),('c83b09cd-54dd-4922-a460-f2c831a7a6fb','a5899e6e-9718-11f1-bd28-84a93e3ff303','SICK','2026-08-16','2026-08-28','t6tu','you are chutiya',NULL,'REJECTED','10000000-0000-0000-0000-000000000001','2026-08-13 13:23:29.878438','2026-08-13 13:16:20.998646','2026-08-13 13:23:29.895679','sonawneg123@gmail.com','admin@company.com'),('d39b57c1-431b-47c7-a727-9405d404f1cc','a5899e6e-9718-11f1-bd28-84a93e3ff303','STUDY','2026-08-16','2026-08-26','dsfsfd',NULL,NULL,'APPROVED','10000000-0000-0000-0000-000000000001','2026-08-13 13:34:36.298728','2026-08-13 13:33:53.023598','2026-08-13 13:34:36.308703','sonawneg123@gmail.com','admin@company.com'),('fdc6facd-4fbc-43f3-b8aa-9ea1024a166e','a5899e6e-9718-11f1-bd28-84a93e3ff303','PATERNITY','2026-08-14','2026-08-31','ygyhuji',NULL,NULL,'APPROVED','10000000-0000-0000-0000-000000000001','2026-08-13 13:22:25.317911','2026-08-13 13:19:01.235574','2026-08-13 13:22:25.332871','sonawneg123@gmail.com','admin@company.com');
/*!40000 ALTER TABLE `leave_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `performance_reviews`
--

DROP TABLE IF EXISTS `performance_reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `performance_reviews` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `employee_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reviewer_id` char(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `review_period` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rating` int(11) NOT NULL,
  `comments` text COLLATE utf8mb4_unicode_ci,
  `goals` text COLLATE utf8mb4_unicode_ci,
  `review_date` date NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `created_by` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_by` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_performance_reviews_employee_id` (`employee_id`),
  KEY `idx_performance_reviews_review_date` (`review_date`),
  CONSTRAINT `fk_pr_employee` FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_rating` CHECK ((`rating` between 1 and 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `performance_reviews`
--

LOCK TABLES `performance_reviews` WRITE;
/*!40000 ALTER TABLE `performance_reviews` DISABLE KEYS */;
INSERT INTO `performance_reviews` VALUES ('04f43eb8-391c-40d6-ac0f-239586032a8a','811547bf-d652-47a0-8a46-c25885aee20e','10000000-0000-0000-0000-000000000001','1',1,'ewrrwer','werwrewr','2026-08-13','2026-08-13 17:41:17.298498','2026-08-13 17:41:17.298498','admin@company.com','admin@company.com'),('2fae48b6-1d80-4adb-9563-ba5d38303295','1c7ea740-7b32-432e-b4f2-683e4cf97072','10000000-0000-0000-0000-000000000002','12345',1,'','','2026-08-14','2026-08-14 09:56:40.434103','2026-08-14 09:56:40.434103','hr@company.com','hr@company.com'),('77763cf5-f8c9-46a1-8551-b5a5bf6dec48','a5899d90-9718-11f1-bd28-84a93e3ff303','d01db6f8-80a5-4cf7-b871-6c2875d88d88','12345',1,'erwerwer','','2026-08-13','2026-08-13 17:23:48.329869','2026-08-13 17:23:48.329869','ruchitabhurri@gmail.com','ruchitabhurri@gmail.com'),('b335ed62-d35e-4bb4-a3e3-2a4b0da87319','a5899e6e-9718-11f1-bd28-84a93e3ff303','10000000-0000-0000-0000-000000000001','12345',5,'3rwerqwd','1224335','2026-08-13','2026-08-13 17:17:27.179048','2026-08-13 17:17:27.179048','admin@company.com','admin@company.com'),('ccc1944c-b8a6-474b-8638-a8d8ed2b6345','811547bf-d652-47a0-8a46-c25885aee20e','10000000-0000-0000-0000-000000000002','fgg',5,'','','2026-08-13','2026-08-13 17:42:19.921071','2026-08-13 17:42:19.921071','hr@company.com','hr@company.com');
/*!40000 ALTER TABLE `performance_reviews` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `refresh_tokens`
--

DROP TABLE IF EXISTS `refresh_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refresh_tokens` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expiry_date` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_rt_token` (`token`),
  KEY `fk_rt_user` (`user_id`),
  CONSTRAINT `fk_rt_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `refresh_tokens`
--

LOCK TABLES `refresh_tokens` WRITE;
/*!40000 ALTER TABLE `refresh_tokens` DISABLE KEYS */;
/*!40000 ALTER TABLE `refresh_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `created_by` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_by` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_roles_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES ('e3910260-9497-11f1-b520-84a93e3ff303','ROLE_ADMIN',NULL,NULL,NULL,NULL),('e3912558-9497-11f1-b520-84a93e3ff303','ROLE_HR',NULL,NULL,NULL,NULL),('e39129b1-9497-11f1-b520-84a93e3ff303','ROLE_MANAGER',NULL,NULL,NULL,NULL),('e39136ea-9497-11f1-b520-84a93e3ff303','ROLE_EMPLOYEE',NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `user_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role_id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `fk_ur_role` (`role_id`),
  CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_roles`
--

LOCK TABLES `user_roles` WRITE;
/*!40000 ALTER TABLE `user_roles` DISABLE KEYS */;
INSERT INTO `user_roles` VALUES ('10000000-0000-0000-0000-000000000001','e3910260-9497-11f1-b520-84a93e3ff303'),('10000000-0000-0000-0000-000000000002','e3912558-9497-11f1-b520-84a93e3ff303'),('10000000-0000-0000-0000-000000000004','e3912558-9497-11f1-b520-84a93e3ff303'),('5ae0f1c6-2e02-42f1-9bea-ed8c1810e59a','e3912558-9497-11f1-b520-84a93e3ff303'),('9d42529d-d737-4dd7-889a-a1df24c67fc0','e3912558-9497-11f1-b520-84a93e3ff303'),('d01db6f8-80a5-4cf7-b871-6c2875d88d88','e3912558-9497-11f1-b520-84a93e3ff303'),('10000000-0000-0000-0000-000000000003','e39129b1-9497-11f1-b520-84a93e3ff303'),('accec7b1-78ab-4742-9a31-5fb621bef539','e39129b1-9497-11f1-b520-84a93e3ff303'),('26e5f5a5-3310-41c9-8482-64b310c848d1','e39136ea-9497-11f1-b520-84a93e3ff303'),('3240f0df-b16f-489e-8abe-d3add531ea36','e39136ea-9497-11f1-b520-84a93e3ff303'),('81a017b7-732e-4254-90f4-ead5f77ccf6c','e39136ea-9497-11f1-b520-84a93e3ff303'),('93db21a8-c53a-42fa-8de6-42d2fe1553d0','e39136ea-9497-11f1-b520-84a93e3ff303'),('b2facd26-cf7a-40f0-90ef-c7af3b89d275','e39136ea-9497-11f1-b520-84a93e3ff303'),('e818fce5-f62a-4434-9e06-6473a1e13345','e39136ea-9497-11f1-b520-84a93e3ff303');
/*!40000 ALTER TABLE `user_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` char(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `first_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `last_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `is_locked` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `created_by` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_by` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES ('10000000-0000-0000-0000-000000000001','admin@company.com','$2a$12$FENBHO7C7cWBwBY7W/dDY.6LrZB42ysyLBhMKcRIvbKE5Zkz4FD2S','System','Admin',1,0,'2026-08-11 17:23:54.000000','2026-08-11 17:23:54.000000',NULL,NULL),('10000000-0000-0000-0000-000000000002','hr@company.com','$2a$12$2.7NlzJ79UTLaiTSxtYZ..T679UuA6k4QFp.Gg7JvMRwhI.8JHnIu','HRsa','Manager',1,0,'2026-08-11 17:23:54.000000','2026-08-14 09:57:33.120738',NULL,'hr@company.com'),('10000000-0000-0000-0000-000000000003','manager@company.com','$2a$12$oj8TdCoKzeVJrb3fk5fXp.jRgHlLfoRTjGzpa4GD.WTYAwGZvX6uq','Team','Manager',1,0,'2026-08-11 17:23:54.000000','2026-08-11 17:23:54.000000',NULL,NULL),('10000000-0000-0000-0000-000000000004','employee@company.com','$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LPVyXMGYm9G','Demo','Employee',1,0,'2026-08-13 15:30:27.000000','2026-08-13 15:30:27.000000',NULL,NULL),('26e5f5a5-3310-41c9-8482-64b310c848d1','sonawneg123@yahoo.com','$2a$12$0ih2yWDBglWnBymXhiXXhedt50s4yzPER/P8UBLx4CGNlDdXNi2Eu','kk','kk',1,0,'2026-08-12 10:16:01.206776','2026-08-12 10:16:01.206776','SYSTEM','SYSTEM'),('3240f0df-b16f-489e-8abe-d3add531ea36','ramwagh440@gmail.com','$2a$12$WrhEePe9ErCQhpD86MOk7.xpP5710VhLc/3kEy0kRE1kweUVd/6JO','ram','w',1,0,'2026-08-10 18:03:49.880749','2026-08-10 18:03:49.880749','SYSTEM','SYSTEM'),('5ae0f1c6-2e02-42f1-9bea-ed8c1810e59a','undareswati87@gmail.com','$2a$12$hADTRYCzbQkqOpbi5fjbIehjKUCPHMzw0oTZrsA3Sh1XPWCwDyCkS','swati','undare',1,0,'2026-08-13 17:37:30.407415','2026-08-14 08:09:13.237101','SYSTEM','admin@company.com'),('81a017b7-732e-4254-90f4-ead5f77ccf6c','chematerama123@mail.com','$2a$12$pd3hsRn.GH12sNSgBoEPB.lovOeTGoqqeghx9.3b8ymyBP9cWMqKi','hh','hh',1,0,'2026-08-12 11:52:58.704753','2026-08-12 11:52:58.704753','SYSTEM','SYSTEM'),('93db21a8-c53a-42fa-8de6-42d2fe1553d0','sonawaneg623@yahoo.com','$2a$12$OZuolLdByF5DgUIo2mD1zOBi9QiwtUkMFKByZ962.R6weKTQ0eMbK','Gaurav','Sonawane',1,0,'2026-08-12 15:39:23.609590','2026-08-13 10:49:48.080742','SYSTEM','admin@company.com'),('9d42529d-d737-4dd7-889a-a1df24c67fc0','testemployee1@test.com','$2a$12$xTotCIMyTUv9Z9/UfzNhcugx6r7RIOI/3rdN1nBM3vj/XmWdXFzES','Alice','Test',1,0,'2026-08-13 17:07:41.588416','2026-08-13 17:20:00.751339','SYSTEM','admin@company.com'),('accec7b1-78ab-4742-9a31-5fb621bef539','sonawneg123@rediff.com','$2a$12$62MPfchG.mGaLDRSKB8Zh.V9fl4vy3E.TmpEfIULfIVeUj27Z1Itm','Gaurav','Sonawane',1,0,'2026-08-13 07:39:15.075235','2026-08-13 07:39:15.075235','SYSTEM','SYSTEM'),('b2facd26-cf7a-40f0-90ef-c7af3b89d275','sonawneg123@gmail.com','$2a$12$.up6f0jljecteXCQzWAuX.4IsIg687vU3dc.SeoLb.jlDvPiSi8Qq','kashif','ali',1,0,'2026-08-10 12:47:44.325112','2026-08-12 15:45:52.554928','SYSTEM','sonawneg123@gmail.com'),('d01db6f8-80a5-4cf7-b871-6c2875d88d88','ruchitabhurri@gmail.com','$2a$12$itCbcOeuxpHpKXiB6Ucb1ei6KKFamZ4ENP2mw8vQ7jxDXqijSrMq2','ruchita','bhurri',1,0,'2026-08-13 17:23:22.590336','2026-08-13 17:23:22.590336','SYSTEM','SYSTEM'),('e818fce5-f62a-4434-9e06-6473a1e13345','sonawng123@yahoo.com','$2a$12$NIp60ZJOHFwCZanIN4v3LOlHHijCH/EwUC0kTz40O8Idz/AaZg5QK','Gaurav','Sonawane',1,0,'2026-08-11 14:17:58.894952','2026-08-11 14:17:58.894952','SYSTEM','SYSTEM');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-14 16:25:14
