package io.metersphere.commons.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class FixedCapacityUtil {
    public static Map<Long, StringBuffer> fixedCapacityCache = Collections.synchronizedMap(new LRUHashMap<>());
    public final static Map<String, Long> jmeterLogTask = new HashMap<>();

    public static StringBuffer get(Long key) {
        return fixedCapacityCache.get(key);
    }

    public static void put(Long key, StringBuffer value) {
        fixedCapacityCache.put(key, value);
    }

    public static int size() {
        return fixedCapacityCache.size();
    }


    static class LRUHashMap<K, V> extends LinkedHashMap<K, V> {
        private int capacity = 100;

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }


    public static String getJmeterLogger(String reportId, boolean isClear) {
        try {
            Long startTime = FixedCapacityUtil.jmeterLogTask.get(reportId);
            if (startTime == null) {
                startTime = FixedCapacityUtil.jmeterLogTask.get("[" + reportId + "]");
            }
            if (startTime == null) {
                startTime = System.currentTimeMillis();
            }
            Long endTime = System.currentTimeMillis();
            Long finalStartTime = startTime;
            String logMessage = FixedCapacityUtil.fixedCapacityCache.entrySet().stream()
                    .filter(map -> map.getKey() > finalStartTime && map.getKey() <= endTime)
                    .map(map -> map.getValue()).collect(Collectors.joining());

            return logMessage;
        } catch (Exception e) {
            return StringUtils.EMPTY;
        } finally {
            if (isClear && FixedCapacityUtil.jmeterLogTask.containsKey(reportId)) {
                FixedCapacityUtil.jmeterLogTask.remove(reportId);
            }
        }
    }
}
