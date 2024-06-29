package io.github.reionchan.proxy;

import org.springframework.stereotype.Component;

/**
 * 人类
 */
public interface Human {
    /**
     * Human eat
     */
    void eat();

    /**
     * 获得 Human 名字
     */
    String getName();

    /**
     * 设置 Human 名字
     */
    void setName(String name);
}
