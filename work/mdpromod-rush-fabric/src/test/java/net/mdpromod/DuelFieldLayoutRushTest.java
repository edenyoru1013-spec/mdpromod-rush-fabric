package net.mdpromod;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Rush 落点映射的回归网。
 *
 * <p>这个映射坏过一次，症状是「最左一列永远绿不了 / 点不动 / 卡往右挪一格」。
 * 根因是代码以为规则核心会把三列重新编号成 0/1/2 并额外做了 ±1 偏移；
 * 实际核心保留 5 个槽位（sequence 0..4），由 RDRule.lua 禁掉最外侧的 0 和 4，
 * 所以<b>引擎序号与物理列号是恒等的</b>。这里把恒等关系钉死，谁再加偏移都会红。
 */
class DuelFieldLayoutRushTest {

    /** 引擎给的怪兽区 / 魔陷区 location。 */
    private static final int MZONE = 4;
    private static final int SZONE = 8;

    @Test
    void rushColumnsAreOneTwoThreeOnly() {
        for (int column = 0; column <= 4; column++) {
            boolean selectable = DuelFieldLayout.isSelectableColumn(column, DuelRuleMode.RUSH);
            assertEquals(column >= 1 && column <= 3, selectable, "列 " + column);
        }
        // 越界列同样不可点
        assertTrue(!DuelFieldLayout.isSelectableColumn(5, DuelRuleMode.RUSH));
        assertTrue(!DuelFieldLayout.isSelectableColumn(-1, DuelRuleMode.RUSH));
    }

    @Test
    void masterKeepsEveryColumn() {
        // 大师规则不做列裁剪（裁剪只发生在 Rush）
        for (int column = 0; column <= 4; column++) {
            assertTrue(DuelFieldLayout.isSelectableColumn(column, DuelRuleMode.MASTER), "列 " + column);
        }
    }

    @Test
    void zoneIdForIsIdentityForAllFiveSequences() {
        // 关键：0 和 4 虽然被规则禁用、渲染层遮暗，映射表里必须还在，
        // 否则落到那两列上的卡会画不出来（zoneIdFor 返回 null）。
        for (int controller = 0; controller <= 1; controller++) {
            String prefix = "p" + (controller + 1) + "_";
            for (int sequence = 0; sequence <= 4; sequence++) {
                assertEquals(prefix + "mzone_" + sequence,
                        DuelFieldLayout.zoneIdFor(controller, MZONE, sequence, DuelRuleMode.RUSH));
                assertEquals(prefix + "szone_" + sequence,
                        DuelFieldLayout.zoneIdFor(controller, SZONE, sequence, DuelRuleMode.RUSH));
            }
        }
    }

    @Test
    void engineAddressUsesTheSameNumberAsThePhysicalColumn() {
        // 实机日志里核心问的是 (0,4,1)(0,4,2)(0,4,3)
        assertArrayEquals(new int[]{0, MZONE, 1}, DuelFieldLayout.engineAddress("p1_mzone_1", 0));
        assertArrayEquals(new int[]{0, MZONE, 2}, DuelFieldLayout.engineAddress("p1_mzone_2", 0));
        assertArrayEquals(new int[]{0, MZONE, 3}, DuelFieldLayout.engineAddress("p1_mzone_3", 0));
        assertArrayEquals(new int[]{1, MZONE, 2}, DuelFieldLayout.engineAddress("p2_mzone_2", 1));
        assertArrayEquals(new int[]{0, SZONE, 1}, DuelFieldLayout.engineAddress("p1_szone_1", 0));
    }

    @Test
    void dimmedColumnsNeverReachTheEngine() {
        assertNull(DuelFieldLayout.engineAddress("p1_mzone_0", 0), "最左列被 RDRule 禁掉，不该送地址");
        assertNull(DuelFieldLayout.engineAddress("p1_mzone_4", 0), "最右列被 RDRule 禁掉，不该送地址");
        assertNull(DuelFieldLayout.engineAddress("p2_mzone_0", 1));
        assertNull(DuelFieldLayout.engineAddress("p2_mzone_4", 1));
    }

    @Test
    void roundTripThroughEverySelectableAddress() {
        for (int controller = 0; controller <= 1; controller++) {
            for (int location : new int[]{MZONE, SZONE}) {
                for (int sequence = 1; sequence <= 3; sequence++) {
                    String id = DuelFieldLayout.zoneIdFor(controller, location, sequence, DuelRuleMode.RUSH);
                    int[] address = DuelFieldLayout.engineAddress(id, controller, DuelRuleMode.RUSH);
                    assertEquals(controller, address[0]);
                    assertEquals(location, address[1]);
                    assertEquals(sequence, address[2], "序号必须原样往返，不能偏移");
                }
            }
        }
    }

    @Test
    void everySelectableAddressMapsToAPhysicalZone() {
        // 「整块场地一格绿都没有」的兜底：映射不到物理格子时 zoneIndexFor 返回 -1。
        for (int controller = 0; controller <= 1; controller++) {
            for (int location : new int[]{MZONE, SZONE}) {
                for (int sequence = 1; sequence <= 3; sequence++) {
                    int index = DuelFieldLayout.zoneIndexFor(controller, location, sequence,
                            DuelRuleMode.RUSH);
                    assertTrue(index >= 0 && index < DuelFieldLayout.zoneCount(),
                            "落点 (" + controller + "," + location + "," + sequence + ") 映射不到格子");
                }
            }
        }
    }

    @Test
    void lastRemainingColumnStillProducesANonZeroMask() {
        // 旧代码在这里算出来是 0（只剩 mzone_3 时整块场地不亮），是当初那个 bug 的直接证据。
        int onlyThird = 1 << DuelFieldLayout.zoneIndexFor(0, MZONE, 3, DuelRuleMode.RUSH);
        assertNotEquals(0, onlyThird);
        int threeColumns = 0;
        for (int sequence = 1; sequence <= 3; sequence++) {
            threeColumns |= 1 << DuelFieldLayout.zoneIndexFor(0, MZONE, sequence, DuelRuleMode.RUSH);
        }
        // 三列各自的位必须互不相同，否则掩码会"少一列"
        assertEquals(3, Integer.bitCount(threeColumns));
    }

    @Test
    void rushHasNoExtraMonsterZone() {
        assertNull(DuelFieldLayout.engineAddress("shared_emz_left", 0, DuelRuleMode.RUSH));
        assertNull(DuelFieldLayout.engineAddress("shared_emz_right", 1, DuelRuleMode.RUSH));
        assertNull(DuelFieldLayout.zoneIdFor(0, MZONE, 5, DuelRuleMode.RUSH));
        assertNull(DuelFieldLayout.zoneIdFor(0, MZONE, 6, DuelRuleMode.RUSH));
        // 大师规则下额外怪兽区照旧
        assertArrayEquals(new int[]{0, MZONE, 5},
                DuelFieldLayout.engineAddress("shared_emz_left", 0, DuelRuleMode.MASTER));
        assertArrayEquals(new int[]{1, MZONE, 6},
                DuelFieldLayout.engineAddress("shared_emz_left", 1, DuelRuleMode.MASTER));
    }

    @Test
    void pilesMapToTheirZone() {
        assertEquals("p1_graveyard", DuelFieldLayout.zoneIdFor(0, 16, 0, DuelRuleMode.RUSH));
        assertEquals("p2_banished", DuelFieldLayout.zoneIdFor(1, 32, 0, DuelRuleMode.RUSH));
        assertEquals("p1_extra", DuelFieldLayout.zoneIdFor(0, 64, 0, DuelRuleMode.RUSH));
        // 主卡组（location 1）刻意不做映射：它是里侧的，本来就不该出现在落点/高亮里
        assertNull(DuelFieldLayout.zoneIdFor(1, 1, 0, DuelRuleMode.RUSH));
    }

    @Test
    void layoutResourceActuallyLoaded() {
        assertEquals(32, DuelFieldLayout.zoneCount(), "场地应该是 32 个格子");
        assertTrue(DuelFieldLayout.byId("p1_mzone_1").isPresent());
        assertTrue(DuelFieldLayout.byId("p2_szone_3").isPresent());
    }
}
