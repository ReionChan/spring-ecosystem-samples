package io.github.reionchan.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import static org.springframework.data.mongodb.core.mapping.FieldType.DECIMAL128;

/**
 * @author Reion
 * @date 2024-07-05
 **/
@Data
@Document
@ToString
@EqualsAndHashCode(of = "id")
public class Order implements Serializable {
    @Id
    private String id;

    @Field(targetType = DECIMAL128)
    private BigDecimal price;

    @Field
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
