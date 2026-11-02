package net.mdpromod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 额外卡组记账的回归测试。
 *
 * <p>用例照抄实机日志：核心的额外卡组是有序表，卡片离开后序号整体前移，
 * 界面必须跟着同一套规则维护一份实时清单（序号 = 这份清单里的下标），
 * 否则会出现「召唤过的怪兽还在列表里、再点它却召唤出另一张」。
 *
 * <p>另外覆盖按席位记账：真人对真人时两边各一份，
 * 只记自己那一侧的写法会让坐在远半场的人看自己的额外卡组时看到错的东西。
 */
class ExtraDeckTrackerTest {

    private static final int EXTRA = ExtraDeckTracker.EXTRA;
    private static final int MZONE = 4;

    /** 实机那副额外卡组的前几张（同卡号有副本，规则上不可区分）。 */
    private static final List<Integer> DECK = List.of(8491961, 8491961, 8491961, 48608796, 48608796, 72971064);

    @Test
    void codeZeroIsIgnored() {
        ExtraDeckTracker tracker = new ExtraDeckTracker(0);
        tracker.reset(DECK);
        assertFalse(tracker.move(0, EXTRA, 0, MZONE, 0), "卡号 0 不是真卡，不该改清单");
        assertEquals(DECK, tracker.codes());
    }

    @Test
    void leavingRemovesTheFirstCopyAndShiftsTheRest() {
        ExtraDeckTracker tracker = new ExtraDeckTracker(0);
        tracker.reset(DECK);
        assertTrue(tracker.move(0, EXTRA, 0, MZONE, 8491961));
        assertEquals(List.of(8491961, 8491961, 48608796, 48608796, 72971064), tracker.codes(),
                "去掉最前面那个同卡号副本，后面的整体前移");
    }

    @Test
    void returningAppendsToTheEnd() {
        ExtraDeckTracker tracker = new ExtraDeckTracker(0);
        tracker.reset(DECK);
        assertTrue(tracker.move(0, MZONE, 0, EXTRA, 12345678));
        assertEquals(12345678, tracker.codes().get(tracker.codes().size() - 1), "回到额外卡组是 push_back");
    }

    @Test
    void movesWithinExtraDoNotChangeTheList() {
        ExtraDeckTracker tracker = new ExtraDeckTracker(0);
        tracker.reset(DECK);
        assertFalse(tracker.move(0, EXTRA, 0, EXTRA, 8491961));
        assertEquals(DECK, tracker.codes());
    }

    @Test
    void seatZeroTrackerIgnoresSeatOne() {
        ExtraDeckTracker tracker = new ExtraDeckTracker(0);
        tracker.reset(DECK);
        assertFalse(tracker.move(1, EXTRA, 1, MZONE, 8491961), "席位 1 的卡不该动席位 0 的清单");
        assertEquals(DECK, tracker.codes());
    }

    @Test
    void seatOneTrackerTracksSeatOne() {
        ExtraDeckTracker tracker = new ExtraDeckTracker(1);
        tracker.reset(DECK);
        assertTrue(tracker.move(1, EXTRA, 1, MZONE, 8491961));
        assertEquals(List.of(8491961, 8491961, 48608796, 48608796, 72971064), tracker.codes());
        // 反过来：席位 0 的进出不影响它
        assertFalse(tracker.move(0, EXTRA, 0, MZONE, 8491961));
    }

    @Test
    void twoSeatsStayIndependent() {
        ExtraDeckTracker mine = new ExtraDeckTracker(0);
        ExtraDeckTracker theirs = new ExtraDeckTracker(1);
        mine.reset(DECK);
        theirs.reset(DECK);
        assertTrue(mine.move(0, EXTRA, 0, MZONE, 8491961));
        assertEquals(5, mine.codes().size());
        assertEquals(6, theirs.codes().size(), "一边召唤不该影响另一边的清单");
    }

    @Test
    void crossSeatReturnIsCaughtByTheReceivingTracker() {
        // 从我方墓地回到对方额外卡组：只有席位 1 那份该记到
        ExtraDeckTracker mine = new ExtraDeckTracker(0);
        ExtraDeckTracker theirs = new ExtraDeckTracker(1);
        mine.reset(DECK);
        theirs.reset(DECK);
        assertFalse(mine.move(0, 16, 1, EXTRA, 99999999));
        assertTrue(theirs.move(0, 16, 1, EXTRA, 99999999));
    }

    @Test
    void defaultConstructorIsSeatZero() {
        ExtraDeckTracker tracker = new ExtraDeckTracker();
        tracker.reset(DECK);
        assertTrue(tracker.move(0, EXTRA, 0, MZONE, 8491961));
        assertFalse(tracker.move(1, EXTRA, 1, MZONE, 8491961));
    }

    @Test
    void notTrackingUntilReset() {
        ExtraDeckTracker tracker = new ExtraDeckTracker(0);
        assertFalse(tracker.tracking());
        tracker.reset(DECK);
        assertTrue(tracker.tracking());
        tracker.stop();
        assertFalse(tracker.tracking());
    }
}
