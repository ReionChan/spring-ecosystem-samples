package io.github.reionchan;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Reion
 * @date 2023-11-20
 **/
public class Junit5AssertionsTest {
    @Test
    public void test1() {
        // obj equals
        assertEquals("headStr", "headStr");
        // 1. equals 2. string.matches() 3. fast forward
        assertLinesMatch(List.of("^head.*$"), List.of("headStr"));
        // obj equals，not support regx
        assertArrayEquals(List.of("headStr").toArray(), List.of("headStr").toArray());
    }
}
