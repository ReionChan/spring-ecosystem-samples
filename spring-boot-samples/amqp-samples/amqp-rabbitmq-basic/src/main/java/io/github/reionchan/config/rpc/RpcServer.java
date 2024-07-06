package io.github.reionchan.config.rpc;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import static io.github.reionchan.config.rpc.RpcConfig.QUEUE_NAME;

/**
 * @author Reion
 * @date 2024-07-04
 **/
public class RpcServer {

    @RabbitListener(queues = QUEUE_NAME)
    public int fibonacci(int n) {
        System.out.println(" [x] Received request for " + n);
        int result = fib(n);
        System.out.println(" [.] Returned " + result);
        return result;
    }

    public int fib(int n) {
        return n == 0 ? 0 : n == 1 ? 1 : (fib(n - 1) + fib(n - 2));
    }
}
