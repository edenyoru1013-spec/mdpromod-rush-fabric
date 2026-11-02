package net.mdpromod;

/**
 * 决斗规则模式。Rush 使用独立的 3 格场地与通常召唤规则，
 * 但仍然由同一个 ocgcore 对局引擎执行。
 *
 * <p>这个构建是 Rush Duel 独立版：整个模组只有 Rush 一种玩法，
 * {@link #MASTER} 这个值保留下来只是为了兼容旧存档里写进去的
 * {@code RuleMode} 整数（老存档可能是 0），
 * 但 {@link #byOrdinal(int)} 一律折算成 {@link #RUSH}，
 * 也就是说无论实体数据里存的是什么，跑起来的规则永远是 Rush。
 */
enum DuelRuleMode {
    MASTER("master", "大师规则", "五格场地，标准 OCG / Master Duel 规则"),
    RUSH("rush", "Rush Duel", "三格场地，无准备阶段，通常召唤不限次数");

    /** 旧存档里 RuleMode 可能是 0（大师），靠 {@link #byOrdinal(int)} 折算。 */
    static final DuelRuleMode[] CYCLE = {MASTER, RUSH};

    private final String hostKey;
    private final String label;
    private final String detail;

    DuelRuleMode(String hostKey, String label, String detail) {
        this.hostKey = hostKey;
        this.label = label;
        this.detail = detail;
    }

    String hostKey() {
        return hostKey;
    }

    String label() {
        return label;
    }

    String detail() {
        return detail;
    }

    String buttonText() {
        // Rush 独立版没有规则切换按钮，这个方法只给可能残留的调用点一个稳定的说法
        return "规则：Rush";
    }

    DuelRuleMode next() {
        // 没有第二种规则可切了，循环到自己
        return RUSH;
    }

    static DuelRuleMode byHostKey(String key) {
        if (key != null && key.trim().equalsIgnoreCase(RUSH.hostKey)) return RUSH;
        // 认不出来的 hostKey（含旧版的 "master"）一律按 Rush 处理
        return RUSH;
    }

    /**
     * 唯一的 NBT 整数 → 枚举入口。这里是整棵规则树的根：
     * 让它恒返回 {@link #RUSH}，下游所有 {@code mode == RUSH} 分支自然恒真，
     * 旧存档里存成 0（大师）的场地也会自动按 Rush 跑。
     */
    static DuelRuleMode byOrdinal(int value) {
        return RUSH;
    }
}
