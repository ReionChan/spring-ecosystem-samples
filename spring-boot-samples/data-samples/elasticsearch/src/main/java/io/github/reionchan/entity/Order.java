package io.github.reionchan.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.util.Date;


/**
 * @author Reion
 * @date 2024-07-05
 **/
@Data
@ToString
@Document(indexName = "orderindex")
@EqualsAndHashCode(of = "id")
public class Order implements Serializable {
    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Double)
    private Double price;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Field(type = FieldType.Date)
    private Date createTime;
}
