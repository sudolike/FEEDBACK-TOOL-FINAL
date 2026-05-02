package com.cen.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * 启动后打印所有已注册的 RequestMapping，便于核对 controller 方法是否被 Spring 扫到。
 *
 * 触发时机：ApplicationReadyEvent —— 此时 RequestMappingHandlerMapping 已完成所有注册。
 *
 * 输出形式：
 *   [MappingDump] total=NN controller methods registered
 *   [MappingDump] {GET /ai/status} -> AliAliController.status
 *   [MappingDump] {POST /ai/aliTyqw} -> AliAliController.send
 *   ...
 *
 * 之所以保留这个看似"诊断专用"的工具：实际部署中曾遇到 mvn 增量编译/类加载导致部分新方法没有注册，
 * 而 Spring 又不会主动报错的情况。把它常驻在启动日志里能立刻看到漏注册。
 */
@Component
public class MappingDumpRunner {

    private static final Logger log = LoggerFactory.getLogger(MappingDumpRunner.class);

    private final RequestMappingHandlerMapping mapping;

    public MappingDumpRunner(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mapping) {
        this.mapping = mapping;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void dump() {
        try {
            Map<RequestMappingInfo, HandlerMethod> all = mapping.getHandlerMethods();
            log.info("[MappingDump] total={} controller methods registered", all.size());
            Map<String, String> sorted = new TreeMap<>(Comparator.naturalOrder());
            all.forEach((info, hm) -> {
                String key = info.toString();
                String value = hm.getBeanType().getSimpleName() + "." + hm.getMethod().getName();
                sorted.put(key, value);
            });
            sorted.forEach((info, target) -> log.info("[MappingDump] {} -> {}", info, target));
        } catch (Exception e) {
            log.warn("[MappingDump] failed to dump request mappings", e);
        }
    }
}
