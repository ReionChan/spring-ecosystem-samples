package io.github.reionchan.proxy;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

/**
 * 用来打印日志的切面
 *
 * 不同通知类型的执行顺序：
 *      Around 通知 - 执行前逻辑
 *      Before 通知
 *      Around 通知 - 执行后逻辑 (异常时不执行)
 *      After 最终通知 (正常、异常都执行)
 *      AfterReturning 通知（正常返回）/ AfterThrowing 通知 （异常返回）
 *
 * @see <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop-advice">Spring AOP 通知</>
 */
@Component
@Aspect
public class LoggerAspect {

    /**
     * 定义切入点
     *
     * <pre>
     * 利用 {@code within} 限制连接点为 thinking 包下的 proxy 子包内的类
     * 利用 {@code execution} 限制类的方法不能以 get、set 开头
     * </pre>
     *
     * @see <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop-pointcuts">Spring AOP 切入点</>
     */
    @Pointcut("within(thinking..proxy.*) && " +
            "!(execution(* thinking..proxy.*.set*(..)) || execution(* thinking..proxy.*.get*(..)))")
    public void pointcut() {
    }

    /**
     * 方法执行前通知
     *
     * Spring 会把 ProceedingJoinPoint 暴露给通知方法的参数，如果不需此参数可以不在方法参数中声明 (下面所有的通知类似)
     * 如果想要暴露自定义参数，那么在切入点表达式中使用 {@code args} 明确指定参数名称，例如：args(foo, bar)
     */
    @Before("pointcut()")
    public void beforeAdvice() {
        println("Before 通知");
    }

    /**
     * 方法正常执行后返回通知，发生异常时不会执行此通知
     */
    @AfterReturning(value = "pointcut()", returning = "returnValue")
    public void afterReturningAdvice(Object returnValue) {
        println("AfterRetuning 通知，返回结果：" + returnValue);
    }

    /**
     * 方法异常执行后通知，方法正常时不会执行此通知
     */
    @AfterThrowing(value = "pointcut()", throwing = "ex")
    public void afterReturningAdvice(Throwable ex) {
        println("AfterThrowing 通知，异常：" + ex.getMessage());
    }

    /**
     * 方法执行后最终通知，方法正常或异常执行都将执行此通知
     */
    @After("pointcut()")
    public void afterAdvice() {
        println("After 最终通知");
    }

    /**
     * 方法执行前后环绕通知，发生异常时，不会执行原方法执行之后的逻辑
     *
     *  由于需要控制原方法执行时机，故此方法首个方法参数声明为 ProceedingJoinPoint 类型
     *  此外回调原方法后可能要处理执行结果，故方法最好返回参数声明为 Object 类型
     */
    @Around("pointcut()")
    public Object aroundAdvice(ProceedingJoinPoint pjp) throws Throwable {
        println("Around 环绕通知，执行前");

        // 控制原始方法在此执行
        Object result = pjp.proceed();

        println("Around 环绕通知，执行后");

        return result;
    }

    /**
     * 日志打印方法
     * @param msg 日志消息
     */
    private static void println(String msg) {
        System.out.println(msg);
    }
}
