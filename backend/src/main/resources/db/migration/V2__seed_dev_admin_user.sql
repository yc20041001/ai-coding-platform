-- =====================================================
-- V2: 开发环境初始化管理员用户
-- 仅在开发环境使用，生产环境必须移除或替换此文件
-- =====================================================

-- 开发管理员用户
-- 邮箱: admin@example.com
-- 用户名: admin
-- 密码: Admin@123456 (BCrypt 加密)
INSERT INTO `user` (`id`, `username`, `email`, `password`, `status`, `create_time`, `update_time`, `deleted`, `version`) VALUES
(100001, 'admin', 'admin@example.com', '$2b$10$/UE2qFW4yJBblapZGSftLuRiGBi9fkfDj3nNpYsTzdnY/A/4Mbkhy', 'ENABLED', NOW(3), NOW(3), 0, 0)
ON DUPLICATE KEY UPDATE `username` = `username`;

-- 绑定 ADMIN 角色
INSERT INTO `user_role` (`id`, `user_id`, `role_id`, `create_time`) VALUES
(100001, 100001, 1, NOW(3))
ON DUPLICATE KEY UPDATE `user_id` = `user_id`;
