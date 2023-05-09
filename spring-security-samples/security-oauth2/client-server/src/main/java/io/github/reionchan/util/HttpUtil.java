package io.github.reionchan.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * Http 工具类
 *
 * @author Reion
 * @date 2023-05-03 14:29
 **/
public class HttpUtil {

    public static HttpHeaders getJwtTokenRequestHeaders(String jwt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));
        headers.setBearerAuth(jwt);
        return headers;
    }

    public static HttpRequest.Builder builder(String url) {
        URI uri = URI.create(url);
        return HttpRequest.newBuilder(uri);
    }

    public static HttpRequest.BodyPublisher ofForm(Map<Object, Object> data) {

        StringBuilder body = new StringBuilder();

        for (Object dataKey : data.keySet()) {

            if (body.length() > 0) {
                body.append("&");
            }

            body.append(encode(dataKey))
                    .append("=")
                    .append(encode(data.get(dataKey)));
        }

        return HttpRequest.BodyPublishers.ofString(body.toString());
    }

    private static String encode(Object obj) {
        return URLEncoder.encode(obj.toString(), StandardCharsets.UTF_8);
    }
}
