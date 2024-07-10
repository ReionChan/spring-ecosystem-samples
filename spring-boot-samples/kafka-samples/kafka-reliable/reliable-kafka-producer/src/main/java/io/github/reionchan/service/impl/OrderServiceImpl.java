package io.github.reionchan.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.reionchan.dto.OrderDto;
import io.github.reionchan.service.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import static io.github.reionchan.consts.KafkaMQConst.ORDER_TOPIC;

/**
 * @author Reion
 * @date 2024-07-05
 **/
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private KafkaTemplate<String, OrderDto> kafkaTemplate;

    @Override
    @Scheduled(fixedDelay = 3000, initialDelay = 1000)
    public String createOrder() {

        // 调度任务模拟生产订单
        OrderDto orderDto = new OrderDto();
        orderDto.setId(IdUtil.simpleUUID());
        BigDecimal price = NumberUtil.round(NumberUtil.generateRandomNumber(1000, 5000, 1)[0] / 3.0d, 2);
        orderDto.setPrice(price);
        orderDto.setCreateTime(new Date());

        CompletableFuture<SendResult<String, OrderDto>> send = kafkaTemplate.send(ORDER_TOPIC, orderDto.getId(), orderDto);
        // 模拟超时请放开注释，防止发送缓存
        //kafkaTemplate.flush();
        send.whenComplete((result, e) -> {
            if (ObjectUtil.isNotEmpty(e)) {
                log.error("订单生成失败", e);
                return;
            }
            log.info("生成订单：{}", result.getProducerRecord().key());
        });

        return orderDto.getId();
    }
}
