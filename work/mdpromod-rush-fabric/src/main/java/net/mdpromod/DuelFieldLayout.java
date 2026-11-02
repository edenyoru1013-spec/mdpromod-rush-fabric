package net.mdpromod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DuelFieldLayout {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<Zone> ZONES = new ArrayList<>();
    private static final Map<String, Zone> BY_ID = new HashMap<>();
    private static final Map<String, Integer> INDEX_BY_ID = new HashMap<>();
    private static final List<Control> CONTROLS = new ArrayList<>();
    private static final Map<String, Control> CONTROL_BY_ID = new HashMap<>();
    private static boolean loaded;
    private static double widthBlocks = 16.0;
    private static double depthBlocks = 14.0;

    private DuelFieldLayout() {
    }

    public record Zone(String id, String owner, String kind,
                       double x, double y, double z, int rotationY) {
    }
    public record Control(String id, String owner, String kind, String phase, String label,
                          double x, double y, double z, double width, double depth, int rotationY) {}

    public static synchronized void load() {
        if (loaded) return;
        try (InputStream input = DuelFieldLayout.class.getResourceAsStream(
                "/data/mdpromod/duel_field_layout.json")) {
            if (input == null) {
                LOGGER.error("Duel field layout resource is missing");
                return;
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("size_blocks")) {
                JsonArray size = root.getAsJsonArray("size_blocks");
                widthBlocks = size.get(0).getAsDouble();
                depthBlocks = size.get(2).getAsDouble();
            }
            JsonArray zones = root.getAsJsonArray("zones");
            for (JsonElement element : zones) {
                JsonObject zone = element.getAsJsonObject();
                JsonArray center = zone.getAsJsonArray("center");
                Zone value = new Zone(
                        zone.get("id").getAsString(),
                        zone.get("owner").getAsString(),
                        zone.get("kind").getAsString(),
                        center.get(0).getAsDouble(),
                        center.get(1).getAsDouble(),
                        center.get(2).getAsDouble(),
                        zone.has("rotation_y") ? zone.get("rotation_y").getAsInt() : 0);
                ZONES.add(value);
                BY_ID.put(value.id(), value);
                INDEX_BY_ID.put(value.id(), ZONES.size() - 1);
            }
            JsonArray controls = root.has("controls") ? root.getAsJsonArray("controls") : new JsonArray();
            for (JsonElement element : controls) {
                JsonObject control = element.getAsJsonObject();
                JsonArray center = control.getAsJsonArray("center");
                JsonArray size = control.getAsJsonArray("size");
                String phase = control.has("phase") ? control.get("phase").getAsString() : "";
                Control value = new Control(control.get("id").getAsString(),
                        control.get("owner").getAsString(), control.get("kind").getAsString(), phase,
                        control.has("label") ? control.get("label").getAsString() : "",
                        center.get(0).getAsDouble(), center.get(1).getAsDouble(), center.get(2).getAsDouble(),
                        size.get(0).getAsDouble(), size.get(1).getAsDouble(),
                        control.has("rotation_y") ? control.get("rotation_y").getAsInt() : 0);
                CONTROLS.add(value);
                CONTROL_BY_ID.put(value.id(), value);
            }
            loaded = true;
            LOGGER.info("Loaded {} duel field zones", ZONES.size());
        } catch (Exception ex) {
            LOGGER.error("Failed to load duel field layout", ex);
        }
    }

    public static List<Zone> zones() {
        load();
        return Collections.unmodifiableList(ZONES);
    }

    public static int zoneCount() {
        load();
        return ZONES.size();
    }

    public static double widthBlocks() { load(); return widthBlocks; }
    public static double depthBlocks() { load(); return depthBlocks; }

    public static Optional<Zone> byId(String id) {
        load();
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static List<Zone> ofKind(String kind) {
        load();
        List<Zone> result = new ArrayList<>();
        for (Zone zone : ZONES) {
            if (zone.kind().equals(kind)) result.add(zone);
        }
        return result;
    }

    public static List<Control> controls() { load(); return Collections.unmodifiableList(CONTROLS); }
    public static Optional<Control> controlById(String id) { load(); return Optional.ofNullable(CONTROL_BY_ID.get(id)); }

    /**
     * Rush 模式下真正能放卡的物理列号范围。
     *
     * <p>规则核心在 Rush 里仍然给足 5 个 MZONE / SZONE 槽位（sequence 0..4），
     * 由外层的 RDRule.lua 把最外侧两列禁掉，所以核心问落点时给的是 (0,4,1)(0,4,2)(0,4,3)，
     * 也就是物理上的 mzone_1 / mzone_2 / mzone_3 —— 引擎序号和物理列号是一一对应的。
     * 这里绝对不能再做 -1 / +1 的偏移：一旦偏移，最左的 mzone_1 永远绿不了、点不到，
     * 而只剩 sequence 3 时整块场地一格提示都没有。
     */
    private static final int RUSH_FIRST_COLUMN = 1;
    private static final int RUSH_LAST_COLUMN = 3;

    /**
     * 场上物理格子 → 规则引擎地址 {controller, location, sequence}。
     * 额外怪兽区是共享的：P1 的 MZONE5 与 P2 的 MZONE6 是同一个物理格子，
     * 所以要看"是谁在选"。
     *
     * <p>Rush 下引擎序号与物理列号恒等；被规则禁用、渲染层也遮暗的最外侧两列
     * 返回 null —— 点它们不该再往核心送一个注定被拒的地址。
     */
    public static int[] engineAddress(String zoneId, int side) {
        return engineAddress(zoneId, side, DuelRuleMode.RUSH);
    }

    public static int[] engineAddress(String zoneId, int side, DuelRuleMode mode) {
        if (zoneId == null || zoneId.length() < 4) return null;
        int controller = zoneId.startsWith("p2_") ? 1 : 0;
        String rest = zoneId.substring(3);
        if (rest.startsWith("mzone_")) {
            try {
                int physical = Integer.parseInt(rest.substring(6));
                if (!isSelectableColumn(physical, mode)) return null;
                return new int[]{controller, 4, physical};
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        if (rest.startsWith("szone_")) {
            try {
                int physical = Integer.parseInt(rest.substring(6));
                if (!isSelectableColumn(physical, mode)) return null;
                return new int[]{controller, 8, physical};
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        if (rest.equals("field")) return new int[]{controller, 8, 5};
        if (rest.equals("extra")) return new int[]{controller, 64, 0};
        if (rest.equals("deck")) return new int[]{controller, 1, 0};
        if (rest.equals("graveyard")) return new int[]{controller, 16, 0};
        if (rest.equals("banished")) return new int[]{controller, 32, 0};
        if (zoneId.startsWith("shared_emz_")) {
            if (mode == DuelRuleMode.RUSH) return null;
            boolean left = zoneId.endsWith("left");
            return side == 0 ? new int[]{0, 4, left ? 5 : 6} : new int[]{1, 4, left ? 6 : 5};
        }
        return null;
    }

    /** 这个物理列在当前规则下能不能点（Rush 下最外侧两列被规则禁用）。 */
    static boolean isSelectableColumn(int physicalSequence, DuelRuleMode mode) {
        if (mode != DuelRuleMode.RUSH) return physicalSequence >= 0;
        return physicalSequence >= RUSH_FIRST_COLUMN && physicalSequence <= RUSH_LAST_COLUMN;
    }

    /** 规则引擎地址 → 物理格子 id（engineAddress 的逆）；映射不到返回 null。 */
    public static String zoneIdFor(int controller, int location, int sequence) {
        return zoneIdFor(controller, location, sequence, DuelRuleMode.RUSH);
    }

    public static String zoneIdFor(int controller, int location, int sequence, DuelRuleMode mode) {
        String prefix = "p" + (controller + 1) + "_";
        // 卡堆（额外 / 墓地 / 除外）也要能映射到格子：卡堆的高亮、粒子、发光标记都靠它
        if (location == 16) return prefix + "graveyard";
        if (location == 32) return prefix + "banished";
        if (location == 64) return prefix + "extra";
        if (location == 4) {
            // sequence 0..4 与物理列号恒等，Rush / Master 一致（Rush 的最外侧两列由渲染层遮暗，
            // 这里必须保留完整映射，否则落在那两列上的卡会画不出来）。
            if (sequence <= 4) return prefix + "mzone_" + sequence;
            // Rush 没有额外怪兽区
            if (mode == DuelRuleMode.RUSH) return null;
            boolean left = controller == 0 ? sequence == 5 : sequence == 6;
            return left ? "shared_emz_left" : "shared_emz_right";
        }
        if (location == 8) {
            if (sequence <= 4) return prefix + "szone_" + sequence;
            if (sequence == 5) return prefix + "field";
        }
        return null;
    }

    /** Synced place-hint bit index for an engine address, or -1 if it is not a physical zone. */
    public static int zoneIndexFor(int controller, int location, int sequence) {
        return zoneIndexFor(controller, location, sequence, DuelRuleMode.RUSH);
    }

    public static int zoneIndexFor(int controller, int location, int sequence, DuelRuleMode mode) {
        String id = zoneIdFor(controller, location, sequence, mode);
        return id == null ? -1 : INDEX_BY_ID.getOrDefault(id, -1);
    }
}
