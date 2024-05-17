package com.github.paicoding.forum.core.util;

/**
 * @author YiHui
 * @date 2022/8/31
 */
public class NumUtil {

    /**
     * num 为空或者等于 0
     *
     * @param num
     * @return
     */
    public static boolean nullOrZero(Long num) {
        return num == null || num == 0L;
    }

    /**
     * num 为空或者等于 0
     *
     * @param num
     * @return
     */
    public static boolean nullOrZero(Integer num) {
        return num == null || num == 0;
    }

    /**
     * num 不为空且大于 0
     *
     * @param num
     * @return
     */
    public static boolean upZero(Long num) {
        return num != null && num > 0;
    }

    /**
     * num 不为空且大于 0
     *
     * @param num
     * @return
     */
    public static boolean upZero(Integer num) {
        return num != null && num > 0;
    }
}
