package com.appcnd.aurora.spring.boot.autoconfigure.util;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author nihao 2019/4/29
 */
public class EnhanceAssert extends Assert {
    public static final boolean hasRepeat(List list) {
        if (!CollectionUtils.isEmpty(list)) {
            Set set = new HashSet(list);
            return set.size() != list.size();
        }
        return false;
    }
}
