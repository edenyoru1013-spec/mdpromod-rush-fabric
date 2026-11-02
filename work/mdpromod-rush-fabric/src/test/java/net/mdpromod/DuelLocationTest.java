package net.mdpromod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * 位置文案的回归网（曾经报错过一格）。
 *
 * <p>以前这里写死 {@code sequence + 1}（大师规则的编号习惯），会把 Rush 最左那一列
 * 叫成「怪兽区2」，和绿色高亮的位置对不上。Rush 下三列就是引擎序号 1/2/3，直接用原值。
 */
class DuelLocationTest {

    private static int pack(int controller, int location, int sequence) {
        return controller << 16 | location << 8 | sequence;
    }

    @Test
    void monsterZoneUsesTheEngineSequence() {
        assertEquals("自己怪兽区1", DuelLocation.label(pack(0, 4, 1)));
        assertEquals("自己怪兽区2", DuelLocation.label(pack(0, 4, 2)));
        assertEquals("自己怪兽区3", DuelLocation.label(pack(0, 4, 3)));
        assertEquals("对方怪兽区1", DuelLocation.label(pack(1, 4, 1)));
    }

    @Test
    void dimmedColumnsAreSpelledOut() {
        // 0 / 4 是规则禁用、渲染层遮暗的两列，文案里说清楚是第几格，别冒充合法列
        assertEquals("自己怪兽区(第 0 格)", DuelLocation.label(pack(0, 4, 0)));
        assertEquals("对方怪兽区(第 4 格)", DuelLocation.label(pack(1, 4, 4)));
    }

    @Test
    void spellTrapZoneUsesOneTwoThree() {
        assertEquals("自己魔陷区1", DuelLocation.label(pack(0, 8, 1)));
        assertEquals("自己魔陷区3", DuelLocation.label(pack(0, 8, 3)));
        assertEquals("自己魔陷区(第 0 格)", DuelLocation.label(pack(0, 8, 0)));
        assertEquals("自己魔陷区(第 4 格)", DuelLocation.label(pack(0, 8, 4)));
        // 已知的小不一致：场地魔法所在的 (8,5) 目前落进魔陷区的兜底文案，
        // 会说成「魔陷区(第 5 格)」而不是「场地」。这里先钉住现状，改文案时记得同步。
        assertEquals("自己魔陷区(第 5 格)", DuelLocation.label(pack(0, 8, 5)));
    }

    @Test
    void pilesAndHand() {
        assertEquals("自己卡组", DuelLocation.label(pack(0, 1, 0)));
        assertEquals("对方手牌", DuelLocation.label(pack(1, 2, 0)));
        assertEquals("自己墓地", DuelLocation.label(pack(0, 16, 0)));
        assertEquals("对方除外区", DuelLocation.label(pack(1, 32, 0)));
        assertEquals("自己额外卡组", DuelLocation.label(pack(0, 64, 0)));
    }

    @Test
    void tooltipKeepsTheLocationLine() {
        assertEquals("位置：自己怪兽区2", DuelLocation.tooltipLine(pack(0, 4, 2)));
    }

    @Test
    void ownerNamesAreRelativeToViewerSeat() {
        assertEquals("自己手牌", DuelLocation.label(pack(1, 2, 0), 1));
        assertEquals("对方手牌", DuelLocation.label(pack(1, 2, 0), 0));
        assertEquals("位置：自己手牌", DuelLocation.tooltipLine(pack(1, 2, 0), 1));
    }
}
