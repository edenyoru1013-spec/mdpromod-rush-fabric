package net.mdpromod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

/** 卡组规则默认值、旧存档迁移和 Rush 卡池可用性的回归测试。 */
class DeckRushMigrationTest {

    @Test
    void newDecksStartInRushMode() {
        DeckData deck = new DeckData();
        assertEquals(DuelRuleMode.RUSH, deck.ruleMode());
        assertEquals("rush", deck.save().getString("ruleMode"));
    }

    @Test
    void legacyDecksMigrateToRushOnLoad() {
        CompoundTag legacy = new CompoundTag();
        legacy.putString("name", "old");
        legacy.putString("ruleMode", "master");
        legacy.putIntArray("main", new int[]{1, 2});

        DeckData migrated = DeckData.load(legacy);
        assertEquals(DuelRuleMode.RUSH, migrated.ruleMode());
        assertEquals("rush", migrated.save().getString("ruleMode"));

        CompoundTag oldest = new CompoundTag();
        oldest.putInt("RuleMode", DuelRuleMode.MASTER.ordinal());
        assertEquals(DuelRuleMode.RUSH, DeckData.load(oldest).ruleMode());
    }

    @Test
    void rushCardsNoLongerProduceMasterRuleErrors() {
        CardRegistry.load();
        assertTrue(CardRegistry.rushSize() > 0, "测试资源里应有 Rush 卡");
        int rushCode = CardRegistry.all().iterator().next().code();

        DeckData deck = new DeckData();
        deck.mainDeck().add(rushCode);
        List<String> problems = DeckRules.problems(deck, DeckRules.detectMode(deck));
        assertFalse(problems.stream().anyMatch(problem -> problem.contains("大师规则")),
                "Rush 卡不能再次被反向判成大师卡组卡");
    }

    @Test
    void cardPoolDefaultsToRushAndDoesNotRejectRushCards() {
        CardFilter filter = new CardFilter();
        assertEquals(CardFilter.SOURCE_RUSH, filter.source);
        CardInfo rushCard = new CardInfo(12345678, "test", "", 0, 0,
                0x1 | 0x2000, 1000, 1000, 4, 0x1, 0x1);
        assertTrue(filter.matches(rushCard));
        assertEquals("Rush Duel", CardFilter.sourceName(filter.source));
    }

    @Test
    void realRushDemoDecksAreLegalAfterMigration() throws IOException {
        DeckData plain = readRealDeck(Path.of("..", "rush-invest", "RDRushDemo.ydk"), 40, 0);
        assertTrue(DeckRules.problems(plain).isEmpty(), String.join("；", DeckRules.problems(plain)));

        DeckData fusion = readRealDeck(Path.of("..", "rush-invest", "RDRushFusionDemo.ydk"), 40, 3);
        List<String> fusionProblems = DeckRules.problems(fusion);
        assertFalse(fusionProblems.stream().anyMatch(problem -> problem.contains("大师规则")));
        assertFalse(fusionProblems.stream().anyMatch(problem -> problem.contains("不能放进主卡组")));
        assertTrue(fusionProblems.stream().allMatch(problem -> problem.contains("传说卡")),
                "演示卡组自身只应保留已知的 3 张传说卡问题：" + String.join("；", fusionProblems));
    }

    private static DeckData readRealDeck(Path path, int main, int extra) throws IOException {
        assumeTrue(Files.isRegularFile(path), "演示卡组不在当前检出中：" + path);
        DeckData deck = DeckData.fromYdk(path);
        assertEquals(DuelRuleMode.RUSH, deck.ruleMode());
        assertEquals(main, deck.mainDeck().size());
        assertEquals(extra, deck.extraDeck().size());
        return deck;
    }
}
