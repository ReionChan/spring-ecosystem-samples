package io.github.reionchan.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ResourceController implements ErrorController {


    /**
     * 西游记宇宙各路神仙的法术数据库
     */
    @GetMapping("/user/magicSkill")
    @PreAuthorize("hasAuthority('SCOPE_MAGIC_SKILL')")
    @PostFilter("filterObject.getKey() == authentication.getName()")
    public Map<String, Object> magicSkill(Authentication authentication) {
        String user = authentication != null ? authentication.getName() : null;
        Map<String, Object> magicSkillDatabase = new HashMap<>();
        magicSkillDatabase.put("sanzang", List.of("紧箍咒"));
        magicSkillDatabase.put("wukong", List.of("火眼金睛", "定身术", "筋斗云", "猴毛分身术", "七十二般变化"));
        magicSkillDatabase.put("wuneng", List.of("火光盾", "钉头七箭", "三十六般变化"));
        magicSkillDatabase.put("wujing", List.of("降妖禅杖", "水遁术", "十八般变化"));
        return magicSkillDatabase;
    }

    /**
     * 西游记宇宙各路神仙的兵器数据库
     */
    @GetMapping("/user/weapon")
    @PreAuthorize("hasAuthority('SCOPE_WEAPON')")
    @PostFilter("filterObject.getKey() == authentication.getName()")
    public Map<String, Object> weapon(Authentication authentication) {
        String user = authentication != null ? authentication.getName() : null;
        Map<String, Object> weaponDatabase = new HashMap<>();
        weaponDatabase.put("sanzang", List.of("九环锡杖"));
        weaponDatabase.put("wukong", List.of("如意金箍棒"));
        weaponDatabase.put("wuneng", List.of("宝沁金耙"));
        weaponDatabase.put("wujing", List.of("降妖宝杖"));
        return weaponDatabase;
    }

    /**
     * 错误页面
     */
    @GetMapping("/errorPage")
    public String errorPage(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        Integer statusCode = null;
        HttpStatus sta = HttpStatus.OK;
        if (status != null) {
            statusCode = Integer.valueOf(status.toString());
            sta = HttpStatus.valueOf(statusCode);
        }

        return """
                 <h1>错误页 &gt; </h1>
                 <ul>
                     <li><a href="/">首页</a></li>
                     """ +
                "<li><font color='red'>状态码：" + sta.value() + "</font></li>" +
                "<li><font color='red'>异常消息：" + sta.getReasonPhrase() + "</font></li>" +
                """
                </ul>
                """;
    }
}
