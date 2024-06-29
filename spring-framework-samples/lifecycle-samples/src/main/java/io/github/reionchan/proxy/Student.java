package io.github.reionchan.proxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 学生，具备人类通用行为外，还具备学习的属性
 */
@Component
//@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS) // 此处可以单独指定 Bean 动态代理方式
public class Student implements Human {

    private String name;

     @Autowired
    // 若注释掉无参构造方法，生成单例 Bean 时，指定默认名字为 马六
    public Student(@Value(value = "马六") String name) {
        this.name = name;
    }

    @Override
    public void eat() {
        System.out.println(name + " eat...");
    }

    public void study() {
        System.out.println(name + " study...");
    }

    public String giveASpeech() {
        System.out.println(name + ": 大家好！我今天演讲的主题是...");
        int time = 0;
        try {
//            time = 120/0;
            time = 120/60;
        } catch (Exception ex) {
            throw new RuntimeException("演讲被迫中断！");
        }
        return "演讲历经 " + time + " 小时后圆满结束！";
    }

    @Override
    public String getName() {
        System.out.println(name + " getName...");
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
