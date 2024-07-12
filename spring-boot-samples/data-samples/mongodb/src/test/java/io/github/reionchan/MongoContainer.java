package io.github.reionchan;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Reion
 * @date 2024-07-12
 **/
@Testcontainers
public class MongoContainer {

    static DockerImageName myImage = DockerImageName.parse("mongodb/mongodb-community-server:6.0.3-ubi8")
            .asCompatibleSubstituteFor("mongo");
    static MongoDBContainer mongoDB = new MongoDBContainer(myImage);

    static {
        mongoDB.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDB::getConnectionString);
        registry.add("spring.data.mongodb.database", () -> "test");
    }
}
