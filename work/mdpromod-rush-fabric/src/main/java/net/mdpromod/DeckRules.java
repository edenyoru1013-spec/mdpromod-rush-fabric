package net.mdpromod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 卡组合法性校验：张数（主 40..60 / 额外 15 / 副 15）＋ 当前生效的禁卡表。
 *
 * <p>禁卡表来自运行时根目录的 {@code lflist.conf} + {@code banlist.conf}
 * （见 {@link Banlists} 和 {@link BanlistFile}）：配置里打开哪几张就按哪几张算，
 * 同一张卡取最严的那条；一张都没打开 = 只校验张数。
 * 卡号相同的不同版本（{@code alias}）算同一张卡，额度一起数——和 MD 一样。
 */
public final class DeckRules {
    public static final int MAIN_MIN = 40;
    public static final int MAIN_MAX = 60;
    public static final int EXTRA_MAX = 15;
    public static final int SIDE_MAX = 15;

    private DeckRules() {
    }

    /**
     * Rush 独立版不读禁卡表（原 {@code lflist.conf} / {@code banlist.conf} 逻辑连同
     * {@code Banlists} / {@code BanlistFile} 一起删掉了）。Rush 的构筑规则本身就很简单：
     * 同名卡（含不同版本的 alias）统一 3 张，外加传说卡的种类限制。
     * 这句标题给卡盒界面用，保持和以前一样的形状。
     */
    public static String banlistStatus() {
        return "规则：Rush Duel";
    }

    /** 这张卡能放几张。Rush 固定 3 张，没有禁限制卡概念。 */
    public static int limitOf(int code) {
        return 3;
    }

    public static int limitOf(int code, DuelRuleMode mode) {
        return 3;
    }

    /** Rush 下没有 Mode 可分，卡池里只有 Rush 卡。 */
    public static DuelRuleMode detectMode(DeckData deck) {
        return deck == null ? DuelRuleMode.RUSH : deck.ruleMode();
    }

    /** Rush 下只有一种上限，账面上的说法统一成「限制 3」。 */
    public static String limitText(int code) {
        return "限制 3";
    }

    /** 没有禁卡表可引用，这个后缀恒为空串。 */
    public static String limitSuffix(int code) {
        return "";
    }

    public static String displayName(int code) {
        return CardRegistry.get(code).map(CardInfo::name).orElse("#" + code);
    }

    /**
     * 卡组里这张卡占了几张——按 alias 分组数：同一张卡的不同版本共用额度，
     * 所以拿别的版本凑张数也算超。
     */
    public static int copies(DeckData deck, int code) {
        int wanted = key(code);
        int total = 0;
        for (int value : deck.mainDeck()) if (key(value) == wanted) total++;
        for (int value : deck.extraDeck()) if (key(value) == wanted) total++;
        for (int value : deck.sideDeck()) if (key(value) == wanted) total++;
        return total;
    }

    /** 卡组里和这张卡同组（不同卡号但共用额度）的卡号；没有就返回空表。 */
    public static List<Integer> groupMates(DeckData deck, int code) {
        int wanted = key(code);
        Set<Integer> mates = new LinkedHashSet<>();
        for (int value : cards(deck)) {
            if (value != code && key(value) == wanted) mates.add(value);
        }
        return new ArrayList<>(mates);
    }

    /** 「，和「青眼白龙」算同一张卡」这样的解释；没有同组卡就返回空串。 */
    public static String groupMateText(DeckData deck, int code) {
        List<Integer> mates = groupMates(deck, code);
        if (mates.isEmpty()) return "";
        StringBuilder text = new StringBuilder("，和");
        for (int i = 0; i < mates.size() && i < 2; i++) {
            if (i > 0) text.append("、");
            text.append("「").append(displayName(mates.get(i))).append("」");
        }
        if (mates.size() > 2) text.append(" 等 ").append(mates.size()).append(" 种");
        return text.append("算同一张卡").toString();
    }

    public static List<String> problems(DeckData deck) {
        return problems(deck, DuelRuleMode.RUSH);
    }

    public static List<String> problems(DeckData deck, DuelRuleMode mode) {
        DuelRuleMode ruleMode = mode == null ? DuelRuleMode.RUSH : mode;
        Set<String> issues = new LinkedHashSet<>();
        int main = deck.mainDeck().size();
        int extra = deck.extraDeck().size();
        int side = deck.sideDeck().size();
        if (main < MAIN_MIN) issues.add("主卡组还差 " + (MAIN_MIN - main) + " 张（当前 " + main + "/60）");
        if (main > MAIN_MAX) issues.add("主卡组超过 60 张（当前 " + main + " 张）");
        if (extra > EXTRA_MAX) issues.add("额外卡组超过 15 张（当前 " + extra + " 张）");
        if (side > SIDE_MAX) issues.add("副卡组超过 15 张（当前 " + side + " 张）");

        Map<Integer, int[]> groups = groupCounts(deck);
        for (Map.Entry<Integer, int[]> entry : groups.entrySet()) {
            int group = entry.getKey(), total = entry.getValue()[0], sample = entry.getValue()[1];
            int limit = 3;                       // Rush 没有禁限制表，同名卡一律 3 张
            if (total <= limit) continue;
            String name = displayName(sample);
            issues.add(name + " 最多 " + limit + " 张，当前 " + total + " 张");
        }

        for (int code : cards(deck)) {
            boolean rushCard = CardRegistry.isRush(code);
            if (ruleMode == DuelRuleMode.RUSH && !rushCard) {
                issues.add(displayName(code) + " 不是 Rush Duel 卡，不能用于 Rush 卡组");
            }
        }

        if (ruleMode == DuelRuleMode.RUSH) {
            int[] legendByType = new int[3];
            Set<Integer> checkedLegends = new LinkedHashSet<>();
            for (int code : cards(deck)) {
                CardInfo info = CardRegistry.get(code).orElse(null);
                if (info == null || info.desc() == null || !info.desc().contains("<传说卡>")) continue;
                if (!checkedLegends.add(code)) continue;
                int copies = copies(deck, code);
                if (copies > 1) {
                    issues.add("传说卡「" + info.name() + "」最多投入 1 张，当前 " + copies + " 张");
                }
                if (info.isMonster()) legendByType[0]++;
                else if (info.isSpell()) legendByType[1]++;
                else if (info.isTrap()) legendByType[2]++;
            }
            String[] names = {"传说怪兽卡", "传说魔法卡", "传说陷阱卡"};
            for (int i = 0; i < legendByType.length; i++) {
                if (legendByType[i] > 1) {
                    issues.add(names[i] + "最多投入 1 种，当前 " + legendByType[i] + " 种");
                }
            }
        }

        // Rush 的融合怪兽同样放在额外卡组；不能按普通主卡组卡处理。
        for (int code : deck.mainDeck()) {
            if (CardRegistry.get(code).map(CardInfo::isExtraDeckInRush).orElse(false)) {
                issues.add(displayName(code) + " 是 Rush 额外卡组卡，不能放进主卡组");
            }
        }
        for (int code : deck.extraDeck()) {
            CardInfo info = CardRegistry.get(code).orElse(null);
            if (info != null && !info.isExtraDeckInRush()) {
                issues.add(displayName(code) + " 应放在主卡组，不必放进额外卡组");
            }
        }
        return new ArrayList<>(issues);
    }

    /** 分组统计：别名组 → {合计张数, 组里的第一个卡号}。 */
    private static Map<Integer, int[]> groupCounts(DeckData deck) {
        Map<Integer, int[]> groups = new HashMap<>();
        for (int value : cards(deck)) {
            int group = key(value);
            int[] counts = groups.get(group);
            if (counts == null) groups.put(group, new int[]{1, value});
            else counts[0]++;
        }
        return groups;
    }

    private static List<Integer> cards(DeckData deck) {
        List<Integer> all = new ArrayList<>(deck.mainDeck().size() + deck.extraDeck().size()
                + deck.sideDeck().size());
        all.addAll(deck.mainDeck());
        all.addAll(deck.extraDeck());
        all.addAll(deck.sideDeck());
        return all;
    }

    /** 禁卡表按 alias 记账：有 alias 的卡用 alias，没有就用自己。 */
    private static int key(int code) {
        CardInfo info = CardRegistry.get(code).orElse(null);
        return info == null || info.alias() == 0 ? code : info.alias();
    }
}
