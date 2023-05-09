package io.github.reionchan.dao;

import io.github.reionchan.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 角色数据访问类
 *
 * @author Reion
 * @date 2023-04-25
 **/
@Repository
public interface IRoleDao extends JpaRepository<Role, Long> {

}
