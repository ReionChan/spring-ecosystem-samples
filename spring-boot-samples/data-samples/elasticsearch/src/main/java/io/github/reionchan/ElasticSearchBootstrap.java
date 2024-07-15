package io.github.reionchan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * @author Reion
 * @date 2024-07-12
 **/
@SpringBootApplication
@EnableElasticsearchRepositories
public class ElasticSearchBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(ElasticSearchBootstrap.class, args);
    }
}
