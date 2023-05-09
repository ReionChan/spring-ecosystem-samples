insert into roles(role_name, status, create_time, update_time) values ('USER', 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
insert into roles(role_name, status, create_time, update_time) values ('ADMIN', 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
-- 用户 user 密码 user
insert into users(user_name, password, status, create_time, update_time, roles) values (
  'user', '$2a$10$B1SEasfkonpH5Z5aWGGeGuG0aIeaQA6IG3awVctamwKSWkL.AM8iu', 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), '1'
);
-- 管理员 admin 密码 admin
insert into users(user_name, password, status, create_time, update_time, roles) values (
  'admin', '$2a$10$q4Uwx.lq1WJL7CCeDeYnseah84d4OJxO7wogJKoxcktnG5bNVhFJ6', 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), '1,2'
);