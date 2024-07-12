package io.github.reionchan;

import com.mongodb.client.result.UpdateResult;
import io.github.reionchan.entity.Order;
import io.github.reionchan.repository.OrderRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author Reion
 * @date 2024-07-12
 **/
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(classes = MongodbBootstrap.class)
@ActiveProfiles("test")
public class TestMongoBootstrap extends MongoContainer {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private OrderRepository orderRepository;

    @Test
    @org.junit.jupiter.api.Order(1)
    public void insert() {
        log.info("--- insert ---");
        Order order = new Order();
        order.setPrice(new BigDecimal(10.0));
        order.setCreateTime(new Date());
        Order dbOrder = mongoTemplate.insert(order);
        log.info("id: {} price: {}  date: {}", dbOrder.getId(), dbOrder.getPrice(), dbOrder.getCreateTime());

        List<Order> orderList = new ArrayList<>();
        for (int i=0; i<10; i++) {
            Order o = new Order();
            o.setPrice(new BigDecimal(10.0 + i));
            o.setCreateTime(new Date());
            orderList.add(o);
        }

        Collection<Order> collection = mongoTemplate.insert(orderList, Order.class);
        for (Order o : collection) {
            log.info("id: {} price: {}  date: {}", o.getId(), o.getPrice(), o.getCreateTime());
        }
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    public void update() {
        log.info("--- update ---");
        Query query = Query.query(Criteria.where("price").is(new BigDecimal(10.0)));
        Update udt = Update.update("price", new BigDecimal(100.00));
        UpdateResult result = mongoTemplate.updateMulti(query, udt, Order.class);
        log.info("updated {} records", result.getModifiedCount());
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    public void get() {

        log.info("--- find by Query ---");
        Query query = Query.query(Criteria.where("price").gt(new BigDecimal(15.00))).with(Sort.by(Sort.Order.desc("price")));
        List<Order> orderList = mongoTemplate.find(query, Order.class);
        String one = null;
        if (orderList.size() > 0) {
            for(Order o : orderList) {
                log.info("id: {} price: {}  date: {}", o.getId(), o.getPrice(), o.getCreateTime());
                one = o.getId();
            }
        }

        log.info("--- findById ---");
        Order order = mongoTemplate.findById(one, Order.class);
        log.info("id: {} price: {}  date: {}", order.getId(), order.getPrice(), order.getCreateTime());

        log.info("--- findAll ---");
        orderList = mongoTemplate.findAll(Order.class);
        if (orderList.size() > 0) {
            for(Order o : orderList) {
                log.info("id: {} price: {}  date: {}", o.getId(), o.getPrice(), o.getCreateTime());
            }
        }
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    public void delete() {
        log.info("--- delete ---");
        mongoTemplate.dropCollection(Order.class);
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    public void insertByRepository() {
        log.info("--- insert ---");
        Order order = new Order();
        order.setPrice(new BigDecimal(10.0));
        order.setCreateTime(new Date());

        Order dbOrder = orderRepository.insert(order);
        log.info("id: {} price: {}  date: {}", dbOrder.getId(), dbOrder.getPrice(), dbOrder.getCreateTime());

        List<Order> orderList = new ArrayList<>();
        for (int i=0; i<10; i++) {
            Order o = new Order();
            o.setPrice(new BigDecimal(10.0 + i));
            o.setCreateTime(new Date());
            orderList.add(o);
        }

        Collection<Order> collection = orderRepository.saveAll(orderList);
        for (Order o : collection) {
            log.info("id: {} price: {}  date: {}", o.getId(), o.getPrice(), o.getCreateTime());
        }
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    public void updateByRepository() {
        log.info("--- update ---");
        List<Order> toUpdate = orderRepository.findByPrice(new BigDecimal(10.0));
        toUpdate.forEach(order -> order.setPrice(new BigDecimal(100.0)));
        int count = orderRepository.saveAll(toUpdate).size();
        log.info("updated {} records", count);
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    public void getByRepository() {

        log.info("--- find By price ---");
        List<Order> orderList = orderRepository.findByPriceIsGreaterThanOrderByPriceDesc(new BigDecimal(15.0));
        String one = null;
        if (orderList.size() > 0) {
            for(Order o : orderList) {
                log.info("id: {} price: {}  date: {}", o.getId(), o.getPrice(), o.getCreateTime());
                one = o.getId();
            }
        }

        log.info("--- findById ---");
        Optional<Order> orderOpt = orderRepository.findById(one);
        orderOpt.ifPresent(order -> {
            log.info("id: {} price: {}  date: {}", order.getId(), order.getPrice(), order.getCreateTime());
        });

        log.info("--- findAll ---");
        orderList = orderRepository.findAll();
        if (orderList.size() > 0) {
            for(Order o : orderList) {
                log.info("id: {} price: {}  date: {}", o.getId(), o.getPrice(), o.getCreateTime());
            }
        }
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    public void deleteByRepository() {
        log.info("--- delete ---");
        orderRepository.deleteAll();
    }

}
