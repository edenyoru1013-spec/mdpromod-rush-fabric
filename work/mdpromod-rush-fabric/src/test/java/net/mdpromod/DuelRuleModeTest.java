package net.mdpromod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * 规则固化方式的回归网。
 *
 * <p>RD 独立版没有逐个清理 21 个文件里的 {@code mode == RUSH} 分支，
 * 而是把 {@link DuelRuleMode#byOrdinal(int)}（NBT → 枚举的唯一入口）改成恒返回 RUSH，
 * 让下游分支自然恒真。这里钉死两点：
 * <ol>
 *   <li>任何旧存档值都折算成 RUSH；</li>
 *   <li>{@link DuelRuleMode#MASTER} 枚举值必须还在——旧存档 NBT 里写的是 0，
 *       删掉它会让 ordinal 漂移。</li>
 * </ol>
 */
class DuelRuleModeTest {

    @Test
    void anyOrdinalFoldsToRush() {
        for (int value = -1; value <= 3; value++) {
            assertEquals(DuelRuleMode.RUSH, DuelRuleMode.byOrdinal(value), "ordinal " + value);
        }
        assertEquals(DuelRuleMode.RUSH, DuelRuleMode.byOrdinal(9999));
    }

    @Test
    void anyHostKeyFoldsToRush() {
        assertEquals(DuelRuleMode.RUSH, DuelRuleMode.byHostKey("rush"));
        assertEquals(DuelRuleMode.RUSH, DuelRuleMode.byHostKey("RUSH"));
        assertEquals(DuelRuleMode.RUSH, DuelRuleMode.byHostKey("master"), "旧存档里的 master 也要折成 Rush");
        assertEquals(DuelRuleMode.RUSH, DuelRuleMode.byHostKey(null));
        assertEquals(DuelRuleMode.RUSH, DuelRuleMode.byHostKey("  "));
    }

    @Test
    void thereIsNoSecondRuleToCycleTo() {
        assertEquals(DuelRuleMode.RUSH, DuelRuleMode.RUSH.next());
    }

    @Test
    void masterConstantMustSurviveForOldSaves() {
        // 不是"还在用"，而是"不能删"：删掉会让 byOrdinal 语义随 ordinal 漂移
        assertNotNull(DuelRuleMode.MASTER);
        assertEquals(0, DuelRuleMode.MASTER.ordinal(), "MASTER 必须仍占 ordinal 0");
        assertEquals("rush", DuelRuleMode.RUSH.hostKey());
    }
}
