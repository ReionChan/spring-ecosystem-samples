insert into roles(role_name, status, create_time, update_time) values ('USER', 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
insert into roles(role_name, status, create_time, update_time) values ('ADMIN', 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
-- 用户 wukong 密码 wukong
insert into users(user_name, password, email, avatar, phone, status, create_time, update_time, roles) values (
  'wukong', '$2a$10$ulmJv3HxV1dnExVX5ubCZurRvozEAUkGyE2X8GwKOYSZlAV08Ga.e', 'wukong@auth-server.org', 'http://auth-server:9090/avatar/wukong.jpg', '+86 18012345678', 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), '1'
);
-- 用户 wuneng 密码 wuneng
insert into users(user_name, password, email, avatar, phone, status, create_time, update_time, roles) values (
  'wuneng', '$2a$10$Ck6CekRqlDSs5P07W4Nk4.nQLUMWGtuOla1UKCum2i5zuzEA7LWN6', 'wuneng@auth-server.org', 'http://auth-server:9090/avatar/wuneng.jpg', '+86 18087654321', 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), '1'
);
-- 用户 wujing 密码 wujing
insert into users(user_name, password, email, avatar, phone, status, create_time, update_time, roles) values (
  'wujing', '$2a$10$f1Nq.a4KmBhyMBdVuZJWw.GIHl9eEOi4ef7OTzvhlWolIj7ecAjuG', 'wujing@auth-server.org', 'http://auth-server:9090/avatar/wujing.jpg', '+86 18018273645', 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), '1'
);
-- 管理员 sanzang 密码 sanzang
insert into users(user_name, password, email, avatar, phone, status, create_time, update_time, roles) values (
  'sanzang', '$2a$10$ydApyz0rhw4/PQpkNv0wfuRSlsrphzT.Zhqlt7LucZ8y.r.HsNBrG', 'sanzang@auth-server.org', 'http://auth-server:9090/avatar/sanzang.jpg', '+86 18000808800', 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), '1,2'
);