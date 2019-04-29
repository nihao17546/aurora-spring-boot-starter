package com.appcnd.aurora.spring.boot.autoconfigure.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author nihao 2019/4/29
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface MysqlListener {
    String groupId();
}
