package io.github.reionchan.config.work;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.util.StopWatch;

/**
 * @author Reion
 * @date 2024-07-04
 **/
@RabbitListener(queues = "hello")
public class WorkerReceiver {

    private String name;

    public WorkerReceiver(String name) {
        this.name = name;
    }

    @RabbitHandler
    public void receive(String in) throws InterruptedException {
        StopWatch watch = new StopWatch();
        watch.start();
        StringBuilder builder = new StringBuilder("\n [x] " + name + " Received '" + in + "'");
        doWork(in);
        watch.stop();
        builder.append('\n');
        builder.append("\t [x] " + name + " Done in " +  String.format("%.2f", watch.getTotalTimeSeconds()) + "s");
        System.out.println(builder);
    }

    private void doWork(String in) throws InterruptedException {
        for(char ch : in.toCharArray()) {
            if (ch == '.') {
                Thread.sleep(500);
            }
        }
    }
}
