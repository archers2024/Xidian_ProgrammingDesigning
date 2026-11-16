-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: campus_activity
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `campus_activity`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `campus_activity` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `campus_activity`;

--
-- Table structure for table `t_activity`
--

DROP TABLE IF EXISTS `t_activity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_activity` (
  `activity_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `activity_no` varchar(32) NOT NULL COMMENT '活动编号（业务可见）',
  `signup_guid` varchar(32) NOT NULL COMMENT '报名条随机串，用于链接与二维码',
  `title` varchar(200) NOT NULL COMMENT '活动名称',
  `type_code` varchar(20) NOT NULL COMMENT '活动类型，关联字典表',
  `introduction` text COMMENT '活动简介',
  `location` varchar(200) NOT NULL COMMENT '活动地点',
  `lng_lat` varchar(50) DEFAULT NULL COMMENT '地点经纬度，格式 lng,lat（准确签到校验用）',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `signup_deadline` datetime NOT NULL COMMENT '报名截止时间（必须早于开始时间）',
  `quota` int NOT NULL COMMENT '人数上限',
  `signed_count` int NOT NULL DEFAULT '0' COMMENT '已报名人数（受控冗余，事务内维护）',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/OPEN/CLOSED/ONGOING/FINISHED/CANCELED',
  `checkin_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否开启签到',
  `checkin_secret` varchar(64) DEFAULT NULL COMMENT '动态签到码密钥',
  `checkin_radius` int NOT NULL DEFAULT '500' COMMENT '签到允许半径（米）',
  `live_url` varchar(500) DEFAULT NULL COMMENT '直播链接（可选）',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面图地址',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `created_by` bigint NOT NULL COMMENT '创建人 admin_id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  PRIMARY KEY (`activity_id`),
  UNIQUE KEY `uk_activity_no` (`activity_no`),
  UNIQUE KEY `uk_signup_guid` (`signup_guid`),
  KEY `idx_activity_status` (`status`),
  KEY `idx_activity_type` (`type_code`),
  KEY `idx_activity_start` (`start_time`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='活动表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_activity`
--

LOCK TABLES `t_activity` WRITE;
/*!40000 ALTER TABLE `t_activity` DISABLE KEYS */;
INSERT INTO `t_activity` VALUES (1,'ACT1794829499908','0d116af514a14e0dab2eaf4d','test1','CAREER','','123','','2026-11-16 00:00:00','2026-11-17 00:00:00','2026-11-10 00:00:00',100,0,'ONGOING',0,'f538094cfa904d9c8514144e3e043efa',500,'','','2026-11-16 19:45:03',1,'2026-11-16 19:45:00','2026-11-16 19:45:38',0),(4,'ACT1794833524996','77d15c25734e447390a6f7d1','E2E-UI-自动化活动','LECTURE','由自动化脚本创建的测试活动，用于验证全流程。','教学楼A101','108.9,34.2','2026-11-16 20:42:27','2026-11-16 23:52:27','2026-11-16 20:22:27',50,1,'ONGOING',1,'dda0dbbc54a84aa6844626abfbab4f3e',500,'','','2026-11-16 20:52:08',1,'2026-11-16 20:52:05','2026-11-16 20:52:27',0);
/*!40000 ALTER TABLE `t_activity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_activity_type`
--

DROP TABLE IF EXISTS `t_activity_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_activity_type` (
  `type_code` varchar(20) NOT NULL COMMENT '类型编码',
  `type_name` varchar(50) NOT NULL COMMENT '类型名称',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '1启用 0停用',
  PRIMARY KEY (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='活动类型字典表（消除类型名称的传递依赖）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_activity_type`
--

LOCK TABLES `t_activity_type` WRITE;
/*!40000 ALTER TABLE `t_activity_type` DISABLE KEYS */;
INSERT INTO `t_activity_type` VALUES ('CAREER','就业指导会',3,1),('LECTURE','讲座',1,1),('OTHER','其他',9,1),('SEMINAR','宣讲会',2,1),('TRAINING','培训',4,1);
/*!40000 ALTER TABLE `t_activity_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_admin`
--

DROP TABLE IF EXISTS `t_admin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_admin` (
  `admin_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password_hash` varchar(100) NOT NULL COMMENT 'BCrypt 密文，禁止明文',
  `real_name` varchar(50) NOT NULL COMMENT '姓名',
  `dept` varchar(100) DEFAULT NULL COMMENT '所属部门',
  `role_code` varchar(20) NOT NULL DEFAULT 'ADMIN' COMMENT 'ADMIN/SUPER',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1启用 0停用',
  `token_version` int NOT NULL DEFAULT '0' COMMENT '令牌版本：停用/重置密码/改角色时自增，旧令牌即刻失效',
  `last_login_time` datetime DEFAULT NULL COMMENT '最近登录时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`admin_id`),
  UNIQUE KEY `uk_admin_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_admin`
--

LOCK TABLES `t_admin` WRITE;
/*!40000 ALTER TABLE `t_admin` DISABLE KEYS */;
INSERT INTO `t_admin` VALUES (1,'admin','$2a$10$Pv1tKjO9WiBkfLheeio2h.r69Ntnvl6wStdtXRwRq8rlQ.TdsT5hS','系统管理员','信息化办公室','SUPER',1,0,'2026-11-16 22:26:44','2026-11-16 19:33:58'),(2,'TYS','$2a$10$Di7A8II.lka66OBo1B1lw.dx1gS5F.WX7qB8ZO2R/SQTyIViTfNIO','管理员','TEST','SUPER',1,0,NULL,'2026-11-16 19:44:10');
/*!40000 ALTER TABLE `t_admin` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_attachment`
--

DROP TABLE IF EXISTS `t_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_attachment` (
  `att_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `file_name` varchar(200) NOT NULL COMMENT '文件名',
  `file_url` varchar(500) NOT NULL COMMENT '文件地址',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`att_id`),
  KEY `idx_att_activity` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='活动附件表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_attachment`
--

LOCK TABLES `t_attachment` WRITE;
/*!40000 ALTER TABLE `t_attachment` DISABLE KEYS */;
/*!40000 ALTER TABLE `t_attachment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_checkin`
--

DROP TABLE IF EXISTS `t_checkin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_checkin` (
  `checkin_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `reg_id` bigint NOT NULL COMMENT '对应报名记录',
  `activity_id` bigint NOT NULL COMMENT '活动ID（随报名确定，便于统计）',
  `checkin_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签到时间',
  `method` varchar(20) NOT NULL COMMENT 'BASIC基本 / ACCURATE准确',
  `lng_lat` varchar(50) DEFAULT NULL COMMENT '签到时上报坐标',
  `device_fingerprint` varchar(64) DEFAULT NULL COMMENT '设备指纹，防换机代签',
  PRIMARY KEY (`checkin_id`),
  UNIQUE KEY `uk_checkin_reg` (`reg_id`) COMMENT '一人一次签到',
  KEY `idx_checkin_activity` (`activity_id`),
  KEY `idx_checkin_time` (`checkin_time`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='签到记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_checkin`
--

LOCK TABLES `t_checkin` WRITE;
/*!40000 ALTER TABLE `t_checkin` DISABLE KEYS */;
INSERT INTO `t_checkin` VALUES (2,2,4,'2026-11-16 20:52:30','BASIC','','cb65f1e52d328753');
/*!40000 ALTER TABLE `t_checkin` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_college`
--

DROP TABLE IF EXISTS `t_college`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_college` (
  `college_code` varchar(20) NOT NULL COMMENT '学院代码',
  `college_name` varchar(100) NOT NULL COMMENT '学院名称',
  `sort_order` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`college_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学院字典表（消除学院名称的传递依赖）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_college`
--

LOCK TABLES `t_college` WRITE;
/*!40000 ALTER TABLE `t_college` DISABLE KEYS */;
INSERT INTO `t_college` VALUES ('ART','人文学院',6),('BIZ','经济与管理学院',5),('CS','计算机科学与技术学院',1),('EE','电子工程学院',3),('ME','机电工程学院',4),('OTHER','其他',99),('SE','软件学院',2);
/*!40000 ALTER TABLE `t_college` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_operation_log`
--

DROP TABLE IF EXISTS `t_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_operation_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `admin_id` bigint DEFAULT NULL COMMENT '操作人',
  `op_type` varchar(40) DEFAULT NULL COMMENT '操作类型',
  `target_id` varchar(64) DEFAULT NULL COMMENT '操作对象ID',
  `op_detail` varchar(500) DEFAULT NULL COMMENT '操作详情',
  `ip` varchar(50) DEFAULT NULL COMMENT '来源IP',
  `op_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_log_admin` (`admin_id`),
  KEY `idx_log_time` (`op_time`)
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_operation_log`
--

LOCK TABLES `t_operation_log` WRITE;
/*!40000 ALTER TABLE `t_operation_log` DISABLE KEYS */;
INSERT INTO `t_operation_log` VALUES (1,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 19:43:18'),(2,1,'ADMIN_CREATE','2','新增管理员',NULL,'2026-11-16 19:44:10'),(3,1,'ACTIVITY_CREATE','1','创建活动：test1',NULL,'2026-11-16 19:45:00'),(4,1,'ACTIVITY_PUBLISH','1','发布活动：test1',NULL,'2026-11-16 19:45:03'),(5,NULL,'ACTIVITY_STATUS_AUTO','1','状态自动流转：OPEN → FINISHED',NULL,'2026-11-16 19:45:03'),(6,1,'ACTIVITY_UPDATE','1','编辑活动：test1',NULL,'2026-11-16 19:45:30'),(7,NULL,'ACTIVITY_STATUS_AUTO','1','状态自动流转：FINISHED → ONGOING',NULL,'2026-11-16 19:45:38'),(8,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 19:50:00'),(9,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 19:50:00'),(10,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 19:50:56'),(11,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 19:50:56'),(12,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 20:45:53'),(13,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 20:46:10'),(14,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 20:47:27'),(15,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 20:48:16'),(16,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 20:48:55'),(17,1,'ACTIVITY_CREATE','2','创建活动：E2E-UI-自动化活动',NULL,'2026-11-16 20:49:09'),(18,1,'ACTIVITY_PUBLISH','2','发布活动：E2E-UI-自动化活动',NULL,'2026-11-16 20:49:12'),(19,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 20:49:36'),(20,1,'ACTIVITY_CREATE','3','创建活动：E2E-UI-自动化活动',NULL,'2026-11-16 20:49:50'),(21,1,'ACTIVITY_PUBLISH','3','发布活动：E2E-UI-自动化活动',NULL,'2026-11-16 20:49:53'),(22,1,'ACTIVITY_CHECKIN_SWITCH','3','开启签到：E2E-UI-自动化活动',NULL,'2026-11-16 20:50:10'),(23,1,'ACTIVITY_UPDATE','3','编辑活动：E2E-UI-自动化活动',NULL,'2026-11-16 20:50:10'),(24,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 20:51:51'),(25,1,'ACTIVITY_CREATE','4','创建活动：E2E-UI-自动化活动',NULL,'2026-11-16 20:52:05'),(26,1,'ACTIVITY_PUBLISH','4','发布活动：E2E-UI-自动化活动',NULL,'2026-11-16 20:52:08'),(27,1,'ACTIVITY_CHECKIN_SWITCH','4','开启签到：E2E-UI-自动化活动',NULL,'2026-11-16 20:52:24'),(28,1,'ACTIVITY_UPDATE','4','编辑活动：E2E-UI-自动化活动',NULL,'2026-11-16 20:52:25'),(29,1,'LOGIN','1','登录成功','127.0.0.1','2026-11-16 21:23:43'),(30,1,'LOGIN','1','登录成功','127.0.0.1','2026-11-16 21:23:43'),(31,1,'LOGIN','1','登录成功','127.0.0.1','2026-11-16 21:23:53'),(32,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 21:23:57'),(33,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 21:29:26'),(34,1,'LOGIN','1','登录成功','127.0.0.1','2026-11-16 21:32:59'),(35,1,'LOGIN','1','登录成功','127.0.0.1','2026-11-16 21:33:26'),(36,1,'LOGIN','1','登录成功','127.0.0.1','2026-11-16 21:40:00'),(37,1,'LOGIN','1','登录成功','127.0.0.1','2026-11-16 21:41:02'),(38,1,'LOGIN','1','登录成功','127.0.0.1','2026-11-16 22:01:08'),(39,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 22:22:07'),(40,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 22:22:18'),(41,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 22:22:27'),(42,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 22:22:48'),(43,1,'LOGIN','1','登录成功','127.0.0.1','2026-11-16 22:24:55'),(44,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 22:25:03'),(45,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 22:25:29'),(46,1,'LOGIN','1','登录成功','0:0:0:0:0:0:0:1','2026-11-16 22:26:44');
/*!40000 ALTER TABLE `t_operation_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `t_registration`
--

DROP TABLE IF EXISTS `t_registration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_registration` (
  `reg_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `name` varchar(50) NOT NULL COMMENT '姓名',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `college_code` varchar(20) NOT NULL COMMENT '学院代码，关联字典表',
  `source_channel` varchar(20) DEFAULT NULL COMMENT '来源渠道 SCAN/LINK',
  `signup_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  `status` varchar(20) NOT NULL DEFAULT 'VALID' COMMENT 'VALID/CANCELED',
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`reg_id`),
  UNIQUE KEY `uk_activity_phone` (`activity_id`,`phone`) COMMENT '防止同一人重复报名',
  KEY `idx_reg_activity` (`activity_id`),
  KEY `idx_reg_college` (`college_code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报名记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `t_registration`
--

LOCK TABLES `t_registration` WRITE;
/*!40000 ALTER TABLE `t_registration` DISABLE KEYS */;
INSERT INTO `t_registration` VALUES (2,4,'张三丰','13900001234','ART','SCAN','2026-11-16 20:52:17','VALID',0);
/*!40000 ALTER TABLE `t_registration` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-16 22:26:59
