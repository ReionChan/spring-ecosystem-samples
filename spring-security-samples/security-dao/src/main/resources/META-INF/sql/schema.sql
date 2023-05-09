-- 角色表
create table roles (id bigint generated by default as identity, create_time timestamp(6), role_name varchar(255), status integer, update_time timestamp(6), primary key (id));
-- 权限表
create table users (id bigint generated by default as identity, create_time timestamp(6), password varchar(255), roles varchar(255), status integer, update_time timestamp(6), user_name varchar(255), primary key (id));
-- 创建索引
create unique index ix_users_user_name on users (user_name);
create unique index ix_roles_role_name on roles (role_name);