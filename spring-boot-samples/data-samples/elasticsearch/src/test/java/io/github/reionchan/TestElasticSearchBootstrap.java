package io.github.reionchan;

import co.elastic.clients.json.JsonData;
import io.github.reionchan.entity.Order;
import io.github.reionchan.repository.OrderRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author Reion
 * @date 2024-07-12
 **/
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(classes = {ElasticSearchBootstrap.class})
@ActiveProfiles("test")
public class TestElasticSearchBootstrap {

    @Resource
    private ElasticsearchTemplate esTemplate;

    @Resource
    private OrderRepository orderRepository;

    @Test
    @org.junit.jupiter.api.Order(1)
    public void insert() {
        log.info("--- insert ---");
        Order order = new Order();
        order.setPrice(10.0);
        order.setName(UUID.randomUUID().toString());
        order.setCreateTime(new Date());
        // ES 数据写入到能搜索默认有 1 秒的延迟：内存 Buffer -> OS Cache 默认 1 秒刷新
        // 设置下面刷新策略后将立即刷新，但影响性能（生产环境需评估）
        esTemplate.setRefreshPolicy(RefreshPolicy.IMMEDIATE);
        Order dbOrder = esTemplate.save(order);
        log.info("id: {} price: {}  date: {}", dbOrder.getId(), dbOrder.getPrice(), dbOrder.getCreateTime());

        List<Order> orderList = new ArrayList<>();
        for (int i=0; i<10; i++) {
            Order o = new Order();
            o.setPrice(10.0 + i);
            o.setName(UUID.randomUUID().toString());
            o.setCreateTime(new Date());
            orderList.add(o);
        }

        Iterable<Order> collection = esTemplate.save(orderList);
        for (Order o : collection) {
            log.info("id: {} price: {}  date: {}", o.getId(), o.getPrice(), o.getCreateTime());
        }
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    public void update() {
        // ES 数据写入到能搜索默认有 1 秒的延迟：内存 Buffer -> OS Cache 默认 1 秒刷新
        // Thread.sleep(1000);
        log.info("--- update ---");
        Criteria criteria = new Criteria("price").lessThan(11.00);
        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);
        SearchHits<Order> hits = esTemplate.search(criteriaQuery, Order.class);
        for (SearchHit<Order> hit : hits) {
            hit.getContent().setPrice(100.0);
            log.info("{}", hit.getContent());
            UpdateResponse update = esTemplate.update(hit.getContent());
            log.info("{}", update.getResult());
        }
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    public void get() {
//        Thread.sleep(1000);
        log.info("--- find by CriteriaQuery ---");
        Criteria criteria = new Criteria("price").greaterThan(15.00);
        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);
        SearchHits<Order> hits = esTemplate.search(criteriaQuery, Order.class);
        String one = null;
        if (hits.hasSearchHits()) {
            for(SearchHit<Order> hit : hits) {
                Order o = hit.getContent();
                log.info("id: {} name: {} price: {}  date: {}", o.getId(), o.getName(), o.getPrice(), o.getCreateTime());
                one = o.getId();
            }
        }

        log.info("--- find by StringQuery ---");
        String queryStr = """
            {
                "range": {
                    "price": {
                        "gt": 15.00
                    }
                }
            }
            """;
        StringQuery stringQuery = new StringQuery(queryStr);
        hits = esTemplate.search(stringQuery, Order.class);
        if (hits.hasSearchHits()) {
            for(SearchHit<Order> hit : hits) {
                Order o = hit.getContent();
                log.info("id: {} name: {} price: {}  date: {}", o.getId(), o.getName(), o.getPrice(), o.getCreateTime());
            }
        }

        log.info("--- find by NativeQuery ---");

        Query nativeQuery = NativeQuery.builder()
                .withQuery(q -> q.range(m -> m.field("price").gt(JsonData.of(15.00)))).build();
        hits = esTemplate.search(nativeQuery, Order.class);
        if (hits.hasSearchHits()) {
            for(SearchHit<Order> hit : hits) {
                Order o = hit.getContent();
                log.info("id: {} name: {} price: {}  date: {}", o.getId(), o.getName(), o.getPrice(), o.getCreateTime());
            }
        }

        log.info("--- findById ---");
        if (one != null) {
            Order order = esTemplate.get(one, Order.class);
            log.info("id: {} price: {} name: {} date: {}", order.getId(), order.getName(), order.getPrice(), order.getCreateTime());
        }

        log.info("--- findAll ---");
        Pageable pageable = PageRequest.of(0, 20);
        Query all = Query.findAll();
        all.setPageable(pageable);
        hits = esTemplate.search(all, Order.class);
        if (hits.hasSearchHits()) {
            for(SearchHit<Order> hit : hits) {
                Order o = hit.getContent();
                log.info("id: {} name: {} price: {}  date: {}", o.getId(), o.getName(), o.getPrice(), o.getCreateTime());
            }
        }
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    public void delete() {
        log.info("--- delete ---");
        boolean orderindex = esTemplate.indexOps(IndexCoordinates.of("orderindex")).delete();
        log.info("Delete all {}", orderindex);
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    public void insertByRepository() {
        log.info("--- insert ---");
        Order order = new Order();
        order.setPrice(10.0);
        order.setName(UUID.randomUUID().toString());
        order.setCreateTime(new Date());
        Order dbOrder = orderRepository.save(order);
        log.info("id: {} price: {}  date: {}", dbOrder.getId(), dbOrder.getPrice(), dbOrder.getCreateTime());

        List<Order> orderList = new ArrayList<>();
        for (int i=0; i<10; i++) {
            Order o = new Order();
            o.setPrice(10.0 + i);
            o.setName(UUID.randomUUID().toString());
            o.setCreateTime(new Date());
            orderList.add(o);
        }

        Iterable<Order> collection = orderRepository.saveAll(orderList);
        for (Order o : collection) {
            log.info("id: {} price: {}  date: {}", o.getId(), o.getPrice(), o.getCreateTime());
        }
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    public void updateByRepository() {
//        Thread.sleep(1000);
        log.info("--- update ---");
        List<Order> hits = orderRepository.findByPriceIsLessThan(11.00);
        for (Order hit : hits) {
            hit.setPrice(100.0);
            Order order = orderRepository.save(hit);
            log.info("update price to {}", order.getPrice());
        }
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    public void getByRepository() {
//        Thread.sleep(1000);
        log.info("--- find by Query ---");
        List<Order> hits = orderRepository.findByPriceIsGreaterThanOrderByPriceDesc(15.0);
        String one = null;
        if (hits.size() > 0) {
            for(Order o : hits) {
                log.info("id: {} name: {} price: {}  date: {}", o.getId(), o.getName(), o.getPrice(), o.getCreateTime());
                one = o.getId();
            }
        }

        log.info("--- findById ---");
        if (one != null) {
            Order order = esTemplate.get(one, Order.class);
            log.info("id: {} price: {} name: {} date: {}", order.getId(), order.getName(), order.getPrice(), order.getCreateTime());
        }

        log.info("--- findAll ---");
        Pageable pageable = PageRequest.of(0, 20);
        Page<Order> orderPage = orderRepository.findAll(pageable);
        orderPage.stream().forEach(o -> {
            log.info("id: {} name: {} price: {}  date: {}", o.getId(), o.getName(), o.getPrice(), o.getCreateTime());
        });
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    public void deleteByRepository() {
        log.info("--- delete ---");
        orderRepository.deleteAll();
    }

}
