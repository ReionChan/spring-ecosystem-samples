package io.github.reionchan.repository;

import io.github.reionchan.entity.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Reion
 * @date 2024-07-12
 **/
@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByPrice(BigDecimal decimal);
    List<Order> findByPriceIsGreaterThanOrderByPriceDesc(BigDecimal decimal);
}
