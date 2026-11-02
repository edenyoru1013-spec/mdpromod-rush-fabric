package net.mdpromod;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CardRegistry {
    private static final Map<Integer, CardInfo> CARDS = new HashMap<>();
    private static final Set<Integer> RUSH_CODES = new HashSet<>();
    private static boolean loaded;
    /** 载入失败的原因（空串 = 没失败）；日志和离线测试都靠它说清楚。 */
    private static String loadError = "";

    private CardRegistry() {
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;                     // 失败也只试一次，别每次查询都重来一遍
        // Rush 独立版只加载 Rush 卡库：原来的 cards.json（大师规则 15,019 条）已经不再随包分发。
        // 两个卡池的卡号本来交集为空，删掉大师卡库不影响任何 Rush 卡。
        loadResource("/data/mdpromod/rush_cards.json", true);
        ModLog.info("载入 {} 张 Rush 卡的数据", CARDS.size());
    }

    private static void loadResource(String resource, boolean rush) {
        try (InputStream input = CardRegistry.class.getResourceAsStream(resource)) {
            if (input == null) {
                ModLog.warn("卡库资源缺失：{}", resource);
                loadError = "找不到资源 " + resource;
                return;
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                try {
                    int code = Integer.parseInt(entry.getKey());
                    JsonObject value = entry.getValue().getAsJsonObject();
                    CardInfo previous = CARDS.put(code, new CardInfo(
                            code,
                            value.get("name").getAsString(),
                            value.has("desc") ? value.get("desc").getAsString() : "",
                            value.has("ot") ? value.get("ot").getAsInt() : 0,
                            value.get("alias").getAsInt(),
                            value.get("type").getAsInt(),
                            value.get("atk").getAsInt(),
                            value.get("def").getAsInt(),
                            value.get("level").getAsInt(),
                            value.get("race").getAsInt(),
                            value.get("attribute").getAsInt()));
                    if (previous != null && rush) {
                        ModLog.warn("Rush 卡号 {} 与既有卡号重复，保留统一编号映射", code);
                    }
                    if (rush) RUSH_CODES.add(code);
                } catch (RuntimeException ex) {
                    ModLog.warn("跳过损坏的卡数据 {}：{}", entry.getKey(), ex.toString());
                }
            }
        } catch (Throwable ex) {
            // 离线测试的 classpath 上没有 gson，这里会走到：卡库为空，但功能照常（禁卡表按卡号算）
            loadError = ex.toString();
            ModLog.warn("卡库载入失败：{}", ex.toString());
        }
    }

    static String loadError() {
        load();
        return loadError;
    }

    public static Optional<CardInfo> get(int code) {
        load();
        return Optional.ofNullable(CARDS.get(code));
    }

    public static boolean contains(int code) {
        load();
        return CARDS.containsKey(code);
    }

    public static boolean isRush(int code) {
        load();
        return RUSH_CODES.contains(code);
    }

    public static int rushSize() {
        load();
        return RUSH_CODES.size();
    }

    public static int size() {
        load();
        return CARDS.size();
    }

    public static Collection<CardInfo> all() {
        load();
        return Collections.unmodifiableCollection(CARDS.values());
    }
}
