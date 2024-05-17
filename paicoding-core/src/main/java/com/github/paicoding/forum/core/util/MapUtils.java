package com.github.paicoding.forum.core.util;

import com.google.common.collect.Maps;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author YiHui
 * @date 2022/7/6
 */
public class MapUtils {

    /**
     * 构建 map 数组
     * @param k map 中第一个元素的键
     * @param v map 中第一个元素的值
     * @param kvs 需要放入 map 中的键值对（成对出现）
     * @return map 数组
     * @param <K>
     * @param <V>
     */
    public static <K, V> Map<K, V> create(K k, V v, Object... kvs) {
        Map<K, V> map = Maps.newHashMapWithExpectedSize(kvs.length + 1);
        map.put(k, v);
        for (int i = 0; i < kvs.length; i += 2) {
            map.put((K) kvs[i], (V) kvs[i + 1]);
        }
        return map;
    }

    public static <T, K, V> Map<K, V> toMap(Collection<T> list, Function<T, K> key, Function<T, V> val) {
        if (CollectionUtils.isEmpty(list)) {
            return Maps.newHashMapWithExpectedSize(0);
        }
        return list.stream().collect(Collectors.toMap(key, val));
    }
}
