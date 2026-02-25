-- 1. 在用户表 sys_user 中添加 community_id 字段
ALTER TABLE sys_user ADD COLUMN community_id BIGINT COMMENT '归属社区ID';

-- 2. 创建社区表 sys_community
CREATE TABLE IF NOT EXISTS sys_community (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name VARCHAR(100) NOT NULL COMMENT '社区名称',
    address VARCHAR(255) COMMENT '地址',
    contact VARCHAR(50) COMMENT '联系人',
    phone VARCHAR(20) COMMENT '联系电话',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='社区表';

-- 3. 在报修表 biz_repair 中添加 community_id 字段
-- 注意：这里假设报修表名为 biz_repair，如果实际是 sys_repair 请自行调整
ALTER TABLE biz_repair ADD COLUMN community_id BIGINT COMMENT '归属社区ID';

-- 4. 初始化一些社区数据（可选）
INSERT INTO sys_community (name, address, contact, phone) VALUES 
('幸福小区', '幸福路1号', '张三', '13800000001'),
('阳光小区', '阳光路88号', '李四', '13800000002');

-- 5. 更新现有用户的 community_id（示例）
-- UPDATE sys_user SET community_id = 1 WHERE role = 'admin';

-- 6. 更新现有报修记录的 community_id（示例，关联房屋表）
-- UPDATE biz_repair r 
-- INNER JOIN sys_house h ON r.house_id = h.id 
-- INNER JOIN sys_community c ON h.community_name = c.name 
-- SET r.community_id = c.id;

-- 7. 在停车订单表 biz_parking_order 中添加 community_id 字段并建立索引
ALTER TABLE biz_parking_order 
    ADD COLUMN community_id BIGINT NULL COMMENT '归属社区ID' AFTER id,
    ADD INDEX idx_parking_order_community (community_id);

-- 8. 通过车位表回填停车订单的 community_id（如果你的车位表已设置 community_id）
-- UPDATE biz_parking_order o
-- JOIN biz_parking_space s ON o.space_id = s.id
-- SET o.community_id = s.community_id
-- WHERE o.space_id IS NOT NULL;

-- 9. 创建访客表
CREATE TABLE IF NOT EXISTS sys_visitor (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  community_id BIGINT,
  visitor_name VARCHAR(50),
  visitor_phone VARCHAR(30),
  reason VARCHAR(255),
  visit_time DATETIME,
  car_no VARCHAR(20),
  status VARCHAR(20),
  audit_remark VARCHAR(255),
  create_time DATETIME,
  update_time DATETIME
) COMMENT='访客预约';

-- 10. 创建投诉表
CREATE TABLE IF NOT EXISTS sys_complaint (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  community_id BIGINT,
  type VARCHAR(50),
  content TEXT,
  images TEXT,
  status VARCHAR(20),
  result TEXT,
  create_time DATETIME,
  handle_time DATETIME
) COMMENT='投诉建议';

-- 11. 创建活动表与报名表
CREATE TABLE IF NOT EXISTS sys_activity (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  community_id BIGINT,
  title VARCHAR(100),
  content TEXT,
  start_time DATETIME,
  location VARCHAR(100),
  max_count INT,
  signup_count INT DEFAULT 0,
  status VARCHAR(20),
  cover_url VARCHAR(255),
  create_time DATETIME
) COMMENT='社区活动';

CREATE TABLE IF NOT EXISTS sys_activity_signup (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  activity_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  signup_time DATETIME,
  UNIQUE KEY uk_activity_user(activity_id, user_id)
) COMMENT='活动报名';
