package io.github.reionchan.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 提供基于 Spring JPA 实现自定义用户查询服务参与 Spring Security 认证
 *
 * <pre>
 *  1. 引入 spring-boot-starter-data-jpa 与 h2 内存数据库
 *  2. 数据源配置改用 SpringBoot 标注外置配置 application.yaml 设置 hikari 池化数据源
 *  3. 建立数据库初始化 DDL、DML 脚本，位置在 application.yaml 指定
 *      classpath:META-INF/sql/schema.sql （建立 USERS、ROLES 表结构）
 *      classpath:META-INF/sql/data.sql (插入 user、admin 用户，密码见脚本注释)
 *  4. JPA 关闭自动创建 ddl-auto、开启 SQL 打印 show-sql
 *  5. 配置 classpath:application.yaml 文件，错误页面设置、H2 网页控制台开启
 *  6. Security 配置使用 DAO 数据库用户 {@link io.github.reionchan.config.DaoSecurityConfiguration}
 *  7. 新建用于权限控制测试的控制器 {@link io.github.reionchan.controller.UserController}
 *  8. 自定义继承 Security 用户查询服务接口 UserDetailsService 的服务类及实现
 *      {@link io.github.reionchan.service.IUserService}
 *      {@link io.github.reionchan.service.impl.UserServiceImpl}
 *  9. 使用继承 JpaRepository 接口的 DAO 数据库实体查询
 *      {@link io.github.reionchan.dao.IUserDao}
 *      {@link io.github.reionchan.dao.IRoleDao}
 *  10. 定义数据库表实体类
 *      {@link io.github.reionchan.entity.User}
 *      {@link io.github.reionchan.entity.Role}
 *  11. 启动类 {@link DaoWebSecurityBootstrap} 开启 JpaRepository 并指定组件扫描
 *      {@link EnableJpaRepositories}
 *  12. 指定实体组件扫描路径，两种途径均可
 *      {@link AutoConfigurationPackage}
 *      {@link org.springframework.boot.autoconfigure.domain.EntityScan}
 * </pre>
 *
 * @author Reion
 * @date 2023-04-25
 **/
@SpringBootApplication(scanBasePackages = "io.github.reionchan")
// 配置扫描标记为 @Repository 的接口组件
@EnableJpaRepositories("io.github.reionchan.dao")
// DaoWebSecurityBootstrap 所在的包下不存在 @Entity 持久化实体，使用此注解强制指定扫描器上层包目录
// 即可扫描到 reionchan 的子包 entity 配置的 @Entity
// 除此方法外，还可以再此标注 @EntityScan，设置包名
@AutoConfigurationPackage(basePackages = "io.github.reionchan")
// @EntityScan("io.github.reionchan.entity")
public class DaoWebSecurityBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(DaoWebSecurityBootstrap.class, args);
    }
}
