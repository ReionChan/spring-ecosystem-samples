package io.github.reionchan.config.topic;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.util.StopWatch;

/**
 * @author Reion
 * @date 2024-07-04
 **/
public class TopicReceiver {

    @RabbitListener(queues = "#{autoDeleteQueue1.name}")
    public void receive1(String in) throws InterruptedException {
        receive(in, "routing-1");
    }

    @RabbitListener(queues = "#{autoDeleteQueue2.name}")
    public void receive2(String in) throws InterruptedException {
        receive(in, "routing-2");
    }

    public void receive(String in, String name) throws InterruptedException {
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
        for (char ch : in.toCharArray()) {
            if (ch == '.') {
                Thread.sleep(500);
            }
        }
    }
}
