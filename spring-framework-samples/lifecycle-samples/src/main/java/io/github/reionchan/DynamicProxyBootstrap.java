/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.reionchan;

import io.github.reionchan.proxy.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.cglib.proxy.CallbackFilter;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


/**
 * {@link DynamicProxyBootstrap} 引导类
 *
 * @since 1.0.0
 */
@Component
@ComponentScan(basePackageClasses = LoggerAspect.class)
// proxyTargetClass 默认为 false，
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class DynamicProxyBootstrap {

    public static void main(String[] args) {
        // JDK 动态代理
        jdkDynamicProxy();

        // CGLIB 动态代理
        cglibDynamicProxy();

        // Spring AOP
        springAOP();
    }

    /**
     * JDK 动态代理
     */
    private static void jdkDynamicProxy() {

        /**
         * 三要素
         * --------------------------------------
         * ClassLoader          负责加载所需代理接口的类加载器
         * Class[]              代理接口类型数组
         * InvocationHandler    方法调用处理器，对代理方法的调用都会被定向到此处理器的 invoke 方法
         */
        ClassLoader loader = DynamicProxyBootstrap.class.getClassLoader();
        Class[] interfaces = of(Human.class);
        InvocationHandler handler = new InvocationHandler() {

            // 被代理类，也可以使用构造参数由外部指定具体被代理对象
            private Human human = new Student("张三");

            /**
             * @param proxy     代理对象
             *                      1. 提供反射获取代理对象的一些字节码信息
             *                      2. 作为 invoke 方法返回值时，可以对代理对象进行链式调用，例如：proxy.foo().bar()
             * @param method    当前被调用的方法
             * @param args      当前被调用的方法参数
             */
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                System.out.println("---方法执行前---");

                // 此处反射调用，被调用的对象传入被代理的对象，如果传递代理对象 proxy 将会导致循环调用
                // 语义：代理对象【额外操作】后，交给被代理对象执行【具体业务】
                Object result = method.invoke(human, args);

                System.out.println("---方法执行后---");
                return result;
            }
        };

        Human human = (Human) Proxy.newProxyInstance(loader, interfaces, handler);
        human.eat();
    }

    /**
     * CGLIB 动态代理
     *
     * <pre>
     * {@code
     * 注意：
     *      此处用的 CGLIB API 为 spring-core 内置重新打包的版本
     *      Spring 单纯修改了包名，和原始版本并无差别
     *      如果想获取原始版本，请依赖如下 GAV
     *      ------------------------------------------------
     *      <!-- https://mvnrepository.com/artifact/cglib/cglib -->
     *      <dependency>
     *          <groupId>cglib</groupId>
     *          <artifactId>cglib</artifactId>
     *          <version>3.3.0</version>
     *      </dependency>
     * }
     * </pre>
     */
    private static void cglibDynamicProxy() {
        /**
         * Enhancer             增强器
         * InvocationHandler    调用处理器，对代理方法的调用都会被定向到此处理器的 invoke 方法 （与 JDK 动态代理类似）
         * MethodInterceptor    方法拦截器，
         */
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Student.class);
        enhancer.setInterfaces(of(Flyable.class));

        // 第一种调用处理器 InvocationHandler
        class MyInvocationHandler implements org.springframework.cglib.proxy.InvocationHandler {


            // 被代理类，与 JDK 动态代理类似，需要依赖一个具体的被代理类实例，此处使用构造参数由外部指定具体被代理对象
            private Student student;

            public MyInvocationHandler(Student student) {
                this.student = student;
            }

            /**
             *  InvocationHandler 类型的方法调用处理器，与 JDK 动态代理使用方式一致
             *
             *  @param proxy    代理对象
             *                      1. 提供反射获取代理对象的一些字节码信息
             *                      2. 作为 invoke 方法返回值时，可以对代理对象进行链式调用，例如：proxy.foo().bar()
             * @param method    当前被调用的方法
             * @param objects   当前被调用的方法参数
             */
            @Override
            public Object invoke(Object proxy, Method method, Object[] objects) throws Throwable {
                System.out.println("\n---方法执行前---");

                // 此处反射调用，被调用的对象传入被代理的对象，如果传递代理对象 proxy 将会导致循环调用
                // 语义：代理对象【额外操作】后，交给被代理对象执行【具体业务】
                Object result = method.invoke(student, objects);

                System.out.println("---方法执行后---");
                return result;
            }
        };

        // 第二种方法调用拦截器 MethodInterceptor
        class MyMethodInterceptor implements MethodInterceptor {

            // 被代理类，MethodInterceptor 方式的实现其实不必强依赖一个被代理实例
            // 当需要对父类原始方法的调用时，intercept 方法暴露了类似 super() 的方法引用 methodProxy
            // 此处只是单纯为了实现与第一种方式达到一致效果，而保存了一个被代理实例
            private Student student;

            public MyMethodInterceptor(Student student) {
                this.student = student;
            }

            /**
             *
             * @param proxy         代理对象，被代理类的子类实例
             * @param method        当前被调用的方法
             * @param objects       当前被调用的方法参数
             * @param methodProxy   当前被调用的方法对应的子类覆盖方法
             */
            @Override
            public Object intercept(Object proxy, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                System.out.println("\n---方法执行前---");

                Object result = null;

                // 检查方法来源于 Flyable 接口
                if (method.getDeclaringClass().isAssignableFrom(Flyable.class)) {
                    // 不依赖代理对象的调用，注意第一个参数传的是代理对象 proxy
                    // 优点：
                    //      不依赖代理对象，可以引入任意其他接口，并在此处分发给具备接口行为能力的其他类处理
                    // Plane 具备飞行能力，将飞行行为交给飞机处理
                    Flyable c919 = new Plane("C919", student);
                    result = methodProxy.invoke(c919, objects);
                } else if (method.getDeclaringClass().isAssignableFrom(Student.class)) {
                    // 依赖代理对象的调用，注意第一个参数传的是被代理对象 student
                    // 此中调用模拟 【第一种 InvocationHandler 方式】、【JDK 动态代理方式】类似
                    // 语义：将方法调用直接委托给传入的 张三 执行
                    result = method.invoke(student, objects);
                    // 不依赖代理对象的调用，注意第一个参数传的是代理对象 proxy
                    // 语义：通过代理类 proxy 直接调用父类 Student 的方法，故不会造成嵌套死循环调用
                    result = methodProxy.invokeSuper(proxy, objects);
                }

                System.out.println("---方法执行后---");
                return result;
            }
        };

        Student student = new Student("张三");
        // 添加两个回调
        enhancer.setCallbacks(of(new MyInvocationHandler(student), new MyMethodInterceptor(student)));
        // 设置每种回调具体类型
        enhancer.setCallbackTypes(of(MyInvocationHandler.class, MyMethodInterceptor.class));

        // 定义回调过滤器，通过此过滤器来指定哪个方法使用哪个回调进行处理
        CallbackFilter filter = new CallbackFilter() {

            // 回调方法索引，要保持和 setCallbacks 数组中的 Callback 顺序、索引最大长度一致
            // 索引 0，指定回调方法 MyInvocationHandler，交给被代理类执行
            public static final int TARGET = 0;
            // 索引 1，指定回调方法 MyMethodInterceptor，交给代理类执行
            public static final int PROXY = 1;

            /**
             * @param method    当前被调用的方法
             */
            @Override
            public int accept(Method method) {
                String methodName = method.getName();

                // eat、equals、hashCode 使用构造方法中传入的【被代理类】执行，也就是张三
                if (methodName.equals("eat")
                        || methodName.equals("equals")
                        || methodName.equals("hashCode")) {
                    return TARGET;
                } else {
                    // study 以及其他方法 使用【代理类】执行，也就是李四
                    return PROXY;
                }
            }
        };

        // 设置回调过滤器，可以通过此过滤器来指定哪个方法使用哪个回调进行处理
        enhancer.setCallbackFilter(filter);
        Student studentProxy = (Student) enhancer.create(of(String.class), of("李四"));
        studentProxy.eat();
        studentProxy.study();
        // 动态代理使学生类具备飞行的行为，很有趣！
        // Spring AOP 中把这种运行时为某个类对象增加额外功能的操作称为引入 Introduction。
        //  下文讲解 Spring AOP 时将会使用 Spring AOP API 形式实现目前 CGLIB API 的引入操作。
        // （织入作用的是连接点方法，引入作用的是类）
        ((Flyable) studentProxy).fly();
        System.out.println(studentProxy.getName());
    }

    /**
     * Spring AOP
     *
     * <pre>
     * {@code
     *      Spring 基于 JDK 动态代理、CGLIB 动态代理两种技术来实现 AOP 框架，
     *      Spring AOP 被用来实现 Spring 框架的一些特性，为用户提供声明式的企业级服务、及 AOP 能力。
     *      例如：Spring 声明式事务管理、Spring IoC 容器对中间件的整合能力。
     *
     *      Spring AOP 属于基于动态代理技术实现的 AOP 框架，功能简单但有局限性。
     *      AspectJ 则是基于静态编译技术实现的 AOP 框架，功能强大但有一定的复杂性。
     *
     * AOP 术语
     *
     *  Join Point （连接点）：
     *      能够被切入点表达式匹配的关注点。
     *      Spring AOP 支持 Method Execution（方法执行）的连接点。
     *
     *      AspectJ 则更强大，它还支持：
     *          Method Call（方法调用）、Constructor Call (构造方法调用)、
     *          Constructor Execution (构造方法执行)、
     *          Field Get（字段获取）、Field Set（字段设置）、
     *          Static Initializer Execution（静态初始化执行）等一系列的连接点。
     *
     *  Pointcut （切入点）：
     *      选取符合匹配条件的连接点的规则断言，一般使用正则表达式来描述此断言。
     *      凡是与此切入点匹配的连接点，都会执行该切入点关联的通知。
     *
     *  Advice （通知）：
     *          在切入点所执行的具体操作逻辑方法。
     *
     *  Target （目标对象）：
     *          符合切入点匹配表达式的连接点所属的对象，该目标对象的连接点将被织入通知。
     *          Spring AOP 目标对象是运行时的类的对象实例
     *          AspectJ 目标对象是类的字节码文件、二进制 Jar 文件等
     *
     *  Weaving （织入）：
     *      将目标对象符合切入点表达式的连接点上追加通知的过程。
     *
     *      Spring AOP 织入是将运行时对象符合切入点的连接点增加通知，生成动态代理对象的过程。
     *      AspectJ 织入是将目标字节码符合切入点的连接点上增加通知，编译生成最终字节码的过程。
     *
     * Introduction（引入）：
     *      运行时为某个类对象增加额外功能的操作称为引入 Introduction。
     *      引入作用范围是类，织入作用范围是方法。
     *
     *  Aspect （切面）：
     *      汇聚许多切入点及切入点关联的多个通知，从而完成特定功能的域对象，被称作切面。
     *
     * }
     * </pre>
     *
     * @see <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop-pointcuts-examples">Spring AOP 切点表达式</>
     */
    private static void springAOP() {
        // 注册当前引导类作为 Configuration Class
        ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(DynamicProxyBootstrap.class);
        // 注意：
        //   Student 实现了 Human 接口，默认使用 JDK 动态代理生成的代理对象类型只能为 Human，强制转换将会报 ClassCastException 异常
        // 解决办法：
        //   将 @EnableAspectJAutoProxy 注解 proxyTargetClass 设置为 true，强制使用 CGLIB 动态代理
        Student student = (Student) context.getBean("student");
        student.giveASpeech();

        System.out.println("\n-----Spring AOP 实现引入，让 Student 具备飞行能力-------\n");

        /*
         * 此处本可以直接使用 new 对象，不用容器中的 student、plane 单例 Bean
         * 但为了验证经过 CGLIB 代理过后的对象是否还能继续被 CGLIB 动态代理，故注释。
         * 结果：
         *      经过 CGLIB 提升的实例，是可以继续被 CGLIB、JDK 动态代理
         */
        //Student stu = new Student("王五");
        //Plane plane = new Plane("B737", stu);

        // 使用容器中的 CGLIB 代理的 Plane
        Plane plane = (Plane) context.getBean("plane");
        // 使用容器中的 CGLIB 代理的 Student
        ProxyFactory proxyFactory = new ProxyFactory(student);

        // 指定 CGLIB 动态代理，否则会由于 Student 有接口走 JDK 动态代理，使得代理对象无法转换成 Student 类型
        proxyFactory.setProxyTargetClass(true);
        // 动态引入 Flyable 飞行能力
        proxyFactory.addInterface(Flyable.class);
        // 添加通知，此处使用代理引入拦截器通知类型
        proxyFactory.addAdvice(new DelegatingIntroductionInterceptor(plane));
        Human studentProxy = (Human) proxyFactory.getProxy();
        // 由于被引入的对象 plane 是容器中的 Advised 被通知对象，故此处执 fly 方法会执行所有切入点关联的通知
        ((Flyable) studentProxy).fly();

        context.close();
    }

    /**
     * 获取数组方法
     */
    private static <T> T[] of(T... objs) {
        return (T[]) objs;
    }
}



