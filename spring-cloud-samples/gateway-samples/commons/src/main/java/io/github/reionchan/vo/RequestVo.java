package io.github.reionchan.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

/**
 * 请求值对象
 *
 * @author Reion
 * @date 2023-06-06
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"id", "userName", "age"})
@Schema(name = "RequestVo", description = "用户请求 Vo")
public class RequestVo {

    @JsonProperty("id")
    @Schema(title = "用户ID", description = "用户唯一ID")
    @Positive
    private Integer id;

    @JsonProperty("userName")
    @Schema(name = "userName", title = "用户名", description = "长度5~30，开头为字母，之后数字、字母、_",  minLength = 5, maxLength = 30)
    @Length(min = 5, max = 30, message = "用户名长度 5 ~ 30")
    @Pattern(regexp = "[a-zA-Z][0-9a-zA-Z_]+", message = "开头为字母，之后数字、字母、_")
    private String userName;

    @JsonProperty("age")
    @Min(value = 0, message = "年龄不低于 0")
    @Max(value = 120, message = "年龄不高于 120")
    @Schema(title = "年龄", description = "年龄区间[0, 120]", minimum = "0", maximum = "120")
    private Integer age;
}
