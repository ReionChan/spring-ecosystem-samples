package io.github.reionchan;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Reion
 * @date 2023-11-20
 **/
public class MockitoVerifyTest {
    class PojoBean {
        private String firstName;
        private String lastName;
        public PojoBean(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }

    @Test
    public void test() {
        // ====== 准备 =======
        List mockList = Mockito.mock(List.class);
        PojoBean spyBean = Mockito.spy(new PojoBean("Reion", "Chan"));
        when(spyBean.getFirstName()).thenReturn("_Reion_");

        // ====== 使用 =======
        mockList.add(1);
        mockList.add(2);
        assertEquals("_Reion_", spyBean.getFirstName());
        assertEquals("Chan", spyBean.getLastName());

        // ====== 验证 =======
        // 验证被 mock 对象执行过指定方法，且参数需匹配指定值
        verify(mockList).add(1);
        // 参数校验（只要出现过，不管匹配多少次都正确）
        verify(mockList).add(eq(1));
        verify(mockList).add(eq(1));
        verify(mockList).add(eq(2));
        verify(mockList).add(eq(2));
        // 匹配指定方法执行次数
        // add 1 执行 1 次
        verify(mockList, times(1)).add(1);
        // add 2 执行 1 次
        verify(mockList, times(1)).add(2);
        // add 被调用 2次
        verify(mockList, times(2)).add(anyInt());
        // add 至少被调用 2 次
        verify(mockList, atLeast(2)).add(anyInt());

        // 验证被 spy 对象执行过指定方法
        verify(spyBean, times(1)).getFirstName();
    }
}
