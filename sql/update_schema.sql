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
