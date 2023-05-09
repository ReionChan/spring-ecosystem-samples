package io.github.reionchan.service.impl;

import io.github.reionchan.dao.IRoleDao;
import io.github.reionchan.dao.IUserDao;
import io.github.reionchan.entity.Role;
import io.github.reionchan.entity.User;
import io.github.reionchan.service.IUserService;
import jakarta.annotation.Nonnull;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.security.core.userdetails.User.withUsername;

/**
 * 用户服务实现
 *
 * @author Reion
 * @date 2023-04-25 18:32
 **/
@Service
@Data
@CommonsLog
public class UserServiceImpl implements IUserService {

    @Autowired
    private IUserDao userDao;

    @Autowired
    private IRoleDao roleDao;

    /**
     * 根据用户名查询获取用户详情，对 Security 认证查询接口 UserDetailsService 方法的实现
     *
     * @param username 用户名
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(@Nonnull String username) throws UsernameNotFoundException {

        User user = userDao.findByUserName(username);
        Optional.ofNullable(user).orElseThrow(()->new UsernameNotFoundException("Not Found user by userName :" + username));
        String roles = user.getRoles();

        List<String> roleNames = new ArrayList<>();
        if (Strings.isNotBlank(roles)) {
            Set<Long> roleIds = Arrays.stream(roles.split(",")).mapToLong(Long::parseLong).boxed().collect(Collectors.toSet());
            if (roleIds.size()>0) {
                roleNames = roleDao.findAllById(roleIds).stream().map(Role::getRoleName).collect(Collectors.toList());
            }
        }

        UserDetails userDetails = withUsername(user.getUserName()).password(user.getPassword())
                .roles(roleNames.toArray(new String[1])).build();

        return userDetails;
    }

}
