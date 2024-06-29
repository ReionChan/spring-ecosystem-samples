package io.github.reionchan.proxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 飞机，具备飞行能力
 */
@Component
public class Plane implements Flyable {

    // 飞机型号
    private String type;
    // 乘客
    private Human passenger;

    // 这里会使用构造方法形式的自动注入 human，先 By Type 再 By Name
    @Autowired
    public Plane(@Value("C919") String type, Human human) {
        this.type = type;
        this.passenger = human;
    }

    @Override
    public void fly() {
        System.out.println(passenger.getName() + " 搭乘飞机 " + type + " 翱翔于蓝天...");
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Human getPassenger() {
        return passenger;
    }

    public void setPassenger(Human passenger) {
        this.passenger = passenger;
    }
}
