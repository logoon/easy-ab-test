-- ============================================
-- ABTest配置平台数据库初始化脚本
-- 数据库: easy_ab_test
-- 字符集: utf8mb4
-- ============================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS easy_ab_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE easy_ab_test;

-- ============================================
-- 1. 用户表
-- ============================================
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密后）',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    role VARCHAR(20) DEFAULT 'USER' COMMENT '角色',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 2. 服务表
-- ============================================
DROP TABLE IF EXISTS services;
CREATE TABLE services (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL COMMENT '服务名称',
    service_code VARCHAR(100) NOT NULL UNIQUE COMMENT '服务编码（不可修改）',
    description VARCHAR(500) COMMENT '服务描述',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_service_code (service_code),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务表';

-- ============================================
-- 3. 实验表
-- ============================================
DROP TABLE IF EXISTS experiments;
CREATE TABLE experiments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_name VARCHAR(200) NOT NULL COMMENT '实验名称',
    version VARCHAR(50) NOT NULL COMMENT '版本号',
    effective_time DATETIME COMMENT '生效时间',
    expire_time DATETIME COMMENT '过期时间',
    split_strategy VARCHAR(20) DEFAULT 'PERCENTAGE' COMMENT '分流策略：PERCENTAGE-百分比, USER_ATTRIBUTE-用户属性',
    percentage INT DEFAULT 100 COMMENT '分流百分比（0-100）',
    user_attribute VARCHAR(100) COMMENT '用户属性名',
    attribute_values VARCHAR(500) COMMENT '属性值列表（逗号分隔）',
    service_id BIGINT NOT NULL COMMENT '关联服务ID',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿, RUNNING-运行中, PAUSED-已暂停, FINISHED-已结束',
    return_value_type VARCHAR(20) COMMENT '返回值类型：STRING, INT, BOOLEAN, DECIMAL, JSON',
    default_value_json TEXT COMMENT '默认返回值JSON',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_service_id (service_id),
    INDEX idx_status (status),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验表';

-- ============================================
-- 4. 实验规则表
-- ============================================
DROP TABLE IF EXISTS experiment_rules;
CREATE TABLE experiment_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    experiment_id BIGINT NOT NULL COMMENT '实验ID',
    priority INT DEFAULT 0 COMMENT '优先级（数字越小优先级越高）',
    conditions_json TEXT COMMENT '规则条件JSON',
    return_value_json TEXT COMMENT '返回值定义JSON',
    INDEX idx_experiment_id (experiment_id),
    FOREIGN KEY (experiment_id) REFERENCES experiments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验规则表';
