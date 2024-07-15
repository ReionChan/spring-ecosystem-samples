package io.github.reionchan.repository;

import io.github.reionchan.entity.Order;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Reion
 * @date 2024-07-12
 **/
@Repository
public interface OrderRepository extends ElasticsearchRepository<Order, String> {
    List<Order> findByPrice(Double price);

    List<Order> findByPriceIsLessThan(Double price);
    List<Order> findByPriceIsGreaterThanOrderByPriceDesc(Double price);
}
