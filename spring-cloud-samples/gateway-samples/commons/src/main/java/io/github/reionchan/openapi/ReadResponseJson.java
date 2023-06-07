package io.github.reionchan.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 读取 JSON 格式的通用响应
 *
 * @author Reion
 * @date 2023-06-06
 **/
@Component
public class ReadResponseJson {

    @Autowired
    private ObjectMapper objectMapper;

    private static final String RESPONSE_JSON_FILE = "openapi/response.json";
    public Map<String, Object> read() throws IOException {
        String content = new String(getClass().getClassLoader().getResourceAsStream(RESPONSE_JSON_FILE).readAllBytes());
        return objectMapper.readValue(content, Map.class);
    }
}
