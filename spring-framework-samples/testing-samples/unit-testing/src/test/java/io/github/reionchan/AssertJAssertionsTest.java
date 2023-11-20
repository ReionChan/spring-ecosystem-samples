package io.github.reionchan;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;


/**
 * @author Reion
 * @date 2023-11-20
 **/
public class AssertJAssertionsTest {
    @Test
    public void test1() {
        // 根据传递参数返回相应类型的断言实例，多态之后的断言方法
        assertThat("hello").startsWith("he");
        assertThat(123).isLessThan(128);
        assertThat(List.of(1, 2, 3));
        assertThat(Map.of("a", 1)).containsEntry("a", 1);
        assertThatException().isThrownBy(()-> {throw new RuntimeException("hello");}).withMessage("hello");
    }
}
