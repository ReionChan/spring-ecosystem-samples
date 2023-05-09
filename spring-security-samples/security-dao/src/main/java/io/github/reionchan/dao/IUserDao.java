package io.github.reionchan.dao;

import io.github.reionchan.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户数据访问类
 *
 * @author Reion
 * @date 2023-04-25
 **/
@Repository
public interface IUserDao extends JpaRepository<User, Long> {

    /**
     * 根据用户名查询
     *
     * @param userName 用户名
     * @return
     */
    public User findByUserName(String userName);
}
