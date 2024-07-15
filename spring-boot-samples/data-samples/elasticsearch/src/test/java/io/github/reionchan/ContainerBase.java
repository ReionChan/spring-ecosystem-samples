package io.github.reionchan;

import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Reion
 * @date 2024-07-15
 **/
@Testcontainers
public class ContainerBase {

    static DockerImageName myImage = DockerImageName.parse("elasticsearch:8.8.0");
    static ElasticsearchContainer esContainer = new ElasticsearchContainer(myImage);

    static {
        esContainer.start();
    }
}
