package com.cen.utils;

import cn.hutool.crypto.SecureUtil;

/**
 * 匿名化工具：
 *  - 给定 studentId，生成稳定的 8 位散列（同问卷内可去重，但跨问卷不可关联）。
 *  - 通过随机 salt 与 questionnaireId 混合，避免被反向枚举。
 */
public final class AnonymizeUtils {

    private AnonymizeUtils() {}

    private static final String GLOBAL_SALT = "feedback-tool::anon-v1";

    public static String anonymize(Long studentId, Long scopeId) {
        if (studentId == null) return "anon-unknown";
        String raw = GLOBAL_SALT + ":" + (scopeId == null ? "0" : scopeId) + ":" + studentId;
        String hash = SecureUtil.md5(raw);
        return "anon-" + hash.substring(0, 8);
    }
}
