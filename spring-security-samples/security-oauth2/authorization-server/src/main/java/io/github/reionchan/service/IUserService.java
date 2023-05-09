package io.github.reionchan.service;

import io.github.reionchan.entity.User;
import jakarta.annotation.Nonnull;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * 用户服务层
 *
 * @author Reion
 * @date 2023-04-26
 **/
public interface IUserService extends UserDetailsService {
    User findUserByUsername(@Nonnull String username) throws UsernameNotFoundException;
}
