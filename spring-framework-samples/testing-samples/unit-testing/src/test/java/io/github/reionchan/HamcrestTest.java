package io.github.reionchan;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * @author Reion
 * @date 2023-11-20
 **/
public class HamcrestTest {
    @Test
    public void test() {
        // 给定实际值，使用后面的匹配器进行断言
        assertThat("hello", startsWith("he"));
        assertThat("isPack", is(equalTo("isPack")));
        assertThat(Map.of("a", 1), hasKey("a"));
    }
}
