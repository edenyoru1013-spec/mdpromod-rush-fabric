package net.mdpromod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 决斗盘：16 x 14 方块、按方块对齐（放置时朝向吸附到 90 度）。
 * 刚放下时只有场地本身，玩家把卡盒右键上来之后才会出现卡组模型。
 */
public class DuelFieldEntity extends Entity {
    private static final EntityDataAccessor<Boolean> DATA_P1_DECK =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_P2_DECK =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_P1_NAME =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_P2_NAME =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_P1_MAIN =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_P2_MAIN =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_P1_EXTRA =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_P2_EXTRA =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_P1_REMAIN =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_P2_REMAIN =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_P1_GRAVE =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_P2_GRAVE =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_P1_BANISH =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_P2_BANISH =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.INT);
    /** UI state replicated with the field so the client can render controls without a server lookup. */
    private static final EntityDataAccessor<Boolean> DATA_P1_READY =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_P2_READY =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GAME_STARTED =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_P1_PHASE =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_P2_PHASE =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_P1_LP =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_P2_LP =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TURN_SIDE =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.INT);
    /** 当前核心正在等谁做决定（0/1，-1 = 没有决策点），以及它允许的阶段转移。 */
    private static final EntityDataAccessor<Integer> DATA_PROMPT_SIDE =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_TO_BATTLE =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TO_END =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TO_MAIN2 =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.BOOLEAN);
    /** Bit mask of legal placement zones currently highlighted by the core. */
    private static final EntityDataAccessor<Integer> DATA_PLACE_ZONES =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.INT);
    /** 开局发牌演出进行中：控制条显示「游戏中」并锁住阶段键。 */
    private static final EntityDataAccessor<Boolean> DATA_OPENING =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.BOOLEAN);
    /** 上一局的结果：0 = 没有/进行中，1 = 本侧胜利（金色），2 = 本侧失败（灰色）。 */
    private static final EntityDataAccessor<Integer> DATA_RESULT =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.INT);
    /** 远半场放的是哪个人机：卡组键 / 人机显示名 / 人格。空 = 还没放人机卡组。 */
    private static final EntityDataAccessor<String> DATA_P2_AI =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.STRING);
    /** 时点开关（{@link DuelTimingMode} 的序号）：右下角菜单里能改，跟着决斗盘存档。 */
    private static final EntityDataAccessor<Integer> DATA_TIMING_MODE =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.INT);
    /** 规则模式（{@link DuelRuleMode} 的序号）：开局前可切换，跟着决斗盘存档。 */
    private static final EntityDataAccessor<Integer> DATA_RULE_MODE =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_P2_AI_NAME =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_P2_AI_DIALOG =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.STRING);
    /**
     * 近/远半场的"座位"：占座真人的 UUID 字符串，空串 = 这一侧还没人坐（通常是人机占位）。
     *
     * <p>以前 p1/p2 完全靠 {@link #playerSide} 的几何判定（站在桌子哪半边就算哪边），
     * 绕桌子走一圈身份就翻转。现在先看这张座位表，查不到才退回几何。
     */
    private static final EntityDataAccessor<String> DATA_P1_UUID =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_P2_UUID =
            SynchedEntityData.defineId(DuelFieldEntity.class, EntityDataSerializers.STRING);

    public DuelFieldEntity(EntityType<DuelFieldEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.blocksBuilding = false;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_P1_DECK, false);
        this.entityData.define(DATA_P2_DECK, false);
        this.entityData.define(DATA_P1_NAME, "");
        this.entityData.define(DATA_P2_NAME, "");
        this.entityData.define(DATA_P1_MAIN, 0);
        this.entityData.define(DATA_P2_MAIN, 0);
        this.entityData.define(DATA_P1_EXTRA, 0);
        this.entityData.define(DATA_P2_EXTRA, 0);
        this.entityData.define(DATA_P1_REMAIN, 0);
        this.entityData.define(DATA_P2_REMAIN, 0);
        this.entityData.define(DATA_P1_GRAVE, 0);
        this.entityData.define(DATA_P2_GRAVE, 0);
        this.entityData.define(DATA_P1_BANISH, 0);
        this.entityData.define(DATA_P2_BANISH, 0);
        this.entityData.define(DATA_P1_READY, false);
        this.entityData.define(DATA_P2_READY, false);
        this.entityData.define(DATA_GAME_STARTED, false);
        this.entityData.define(DATA_P1_PHASE, "DP");
        this.entityData.define(DATA_P2_PHASE, "DP");
        this.entityData.define(DATA_P1_LP, 8000);
        this.entityData.define(DATA_P2_LP, 8000);
        this.entityData.define(DATA_TURN_SIDE, 0);
        this.entityData.define(DATA_PROMPT_SIDE, -1);
        this.entityData.define(DATA_TO_BATTLE, false);
        this.entityData.define(DATA_TO_END, false);
        this.entityData.define(DATA_TO_MAIN2, false);
        this.entityData.define(DATA_PLACE_ZONES, 0);
        this.entityData.define(DATA_OPENING, false);
        this.entityData.define(DATA_RESULT, 0);
        this.entityData.define(DATA_P2_AI, "");
        this.entityData.define(DATA_TIMING_MODE, DuelTimingMode.DEFAULT.ordinal());
        this.entityData.define(DATA_RULE_MODE, DuelRuleMode.RUSH.ordinal());
        this.entityData.define(DATA_P2_AI_NAME, "");
        this.entityData.define(DATA_P2_AI_DIALOG, "");
        this.entityData.define(DATA_P1_UUID, "");
        this.entityData.define(DATA_P2_UUID, "");
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_P1_DECK, tag.getBoolean("P1Deck"));
        this.entityData.set(DATA_P2_DECK, tag.getBoolean("P2Deck"));
        this.entityData.set(DATA_P1_NAME, tag.getString("P1Name"));
        this.entityData.set(DATA_P2_NAME, tag.getString("P2Name"));
        this.entityData.set(DATA_P1_MAIN, tag.getInt("P1Main"));
        this.entityData.set(DATA_P2_MAIN, tag.getInt("P2Main"));
        this.entityData.set(DATA_P1_EXTRA, tag.getInt("P1Extra"));
        this.entityData.set(DATA_P2_EXTRA, tag.getInt("P2Extra"));
        this.entityData.set(DATA_P1_REMAIN, tag.getInt("P1Remain"));
        this.entityData.set(DATA_P2_REMAIN, tag.getInt("P2Remain"));
        this.entityData.set(DATA_P1_GRAVE, tag.getInt("P1Grave"));
        this.entityData.set(DATA_P2_GRAVE, tag.getInt("P2Grave"));
        this.entityData.set(DATA_P1_BANISH, tag.getInt("P1Banish"));
        this.entityData.set(DATA_P2_BANISH, tag.getInt("P2Banish"));
        this.entityData.set(DATA_P1_READY, tag.getBoolean("P1Ready"));
        this.entityData.set(DATA_P2_READY, tag.getBoolean("P2Ready"));
        this.entityData.set(DATA_GAME_STARTED, tag.getBoolean("GameStarted"));
        this.entityData.set(DATA_P1_PHASE, tag.contains("P1Phase") ? tag.getString("P1Phase") : "DP");
        this.entityData.set(DATA_P2_PHASE, tag.contains("P2Phase") ? tag.getString("P2Phase") : "DP");
        this.entityData.set(DATA_P1_LP, tag.contains("P1Lp") ? tag.getInt("P1Lp") : 8000);
        this.entityData.set(DATA_P2_LP, tag.contains("P2Lp") ? tag.getInt("P2Lp") : 8000);
        this.entityData.set(DATA_TURN_SIDE, tag.contains("TurnSide") ? tag.getInt("TurnSide") : 0);
        this.entityData.set(DATA_P2_AI, tag.getString("P2Ai"));
        this.entityData.set(DATA_P2_AI_NAME, tag.getString("P2AiName"));
        this.entityData.set(DATA_P2_AI_DIALOG, tag.getString("P2AiDialog"));
        this.entityData.set(DATA_TIMING_MODE, DuelTimingMode.byOrdinal(
                tag.contains("TimingMode") ? tag.getInt("TimingMode")
                        : DuelTimingMode.DEFAULT.ordinal()).ordinal());
        this.entityData.set(DATA_RULE_MODE, DuelRuleMode.byOrdinal(
                tag.contains("RuleMode") ? tag.getInt("RuleMode")
                        : DuelRuleMode.RUSH.ordinal()).ordinal());
        this.entityData.set(DATA_P1_UUID, tag.getString("SeatP1Uuid"));
        this.entityData.set(DATA_P2_UUID, tag.getString("SeatP2Uuid"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("P1Deck", this.entityData.get(DATA_P1_DECK));
        tag.putBoolean("P2Deck", this.entityData.get(DATA_P2_DECK));
        tag.putString("P1Name", this.entityData.get(DATA_P1_NAME));
        tag.putString("P2Name", this.entityData.get(DATA_P2_NAME));
        tag.putInt("P1Main", this.entityData.get(DATA_P1_MAIN));
        tag.putInt("P2Main", this.entityData.get(DATA_P2_MAIN));
        tag.putInt("P1Extra", this.entityData.get(DATA_P1_EXTRA));
        tag.putInt("P2Extra", this.entityData.get(DATA_P2_EXTRA));
        tag.putInt("P1Remain", this.entityData.get(DATA_P1_REMAIN));
        tag.putInt("P2Remain", this.entityData.get(DATA_P2_REMAIN));
        tag.putInt("P1Grave", this.entityData.get(DATA_P1_GRAVE));
        tag.putInt("P2Grave", this.entityData.get(DATA_P2_GRAVE));
        tag.putInt("P1Banish", this.entityData.get(DATA_P1_BANISH));
        tag.putInt("P2Banish", this.entityData.get(DATA_P2_BANISH));
        tag.putBoolean("P1Ready", this.entityData.get(DATA_P1_READY));
        tag.putBoolean("P2Ready", this.entityData.get(DATA_P2_READY));
        tag.putBoolean("GameStarted", this.entityData.get(DATA_GAME_STARTED));
        tag.putString("P1Phase", this.entityData.get(DATA_P1_PHASE));
        tag.putString("P2Phase", this.entityData.get(DATA_P2_PHASE));
        tag.putInt("P1Lp", this.entityData.get(DATA_P1_LP));
        tag.putInt("P2Lp", this.entityData.get(DATA_P2_LP));
        tag.putInt("TurnSide", this.entityData.get(DATA_TURN_SIDE));
        tag.putString("P2Ai", this.entityData.get(DATA_P2_AI));
        tag.putString("P2AiName", this.entityData.get(DATA_P2_AI_NAME));
        tag.putString("P2AiDialog", this.entityData.get(DATA_P2_AI_DIALOG));
        tag.putInt("TimingMode", this.entityData.get(DATA_TIMING_MODE));
        tag.putInt("RuleMode", this.entityData.get(DATA_RULE_MODE));
        tag.putString("SeatP1Uuid", this.entityData.get(DATA_P1_UUID));
        tag.putString("SeatP2Uuid", this.entityData.get(DATA_P2_UUID));
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256.0 * 256.0;
    }

    // ------------------------------------------------------------ 卡组

    public boolean hasDeck(String side) {
        return "p2".equals(side)
                ? this.entityData.get(DATA_P2_DECK)
                : this.entityData.get(DATA_P1_DECK);
    }

    public boolean hasAnyDeck() {
        return this.entityData.get(DATA_P1_DECK) || this.entityData.get(DATA_P2_DECK);
    }

    public String deckName(String side) {
        return "p2".equals(side) ? this.entityData.get(DATA_P2_NAME) : this.entityData.get(DATA_P1_NAME);
    }

    public int mainCount(String side) {
        return "p2".equals(side) ? this.entityData.get(DATA_P2_MAIN) : this.entityData.get(DATA_P1_MAIN);
    }

    public int extraCount(String side) {
        return "p2".equals(side) ? this.entityData.get(DATA_P2_EXTRA) : this.entityData.get(DATA_P1_EXTRA);
    }

    public int remainingCount(String side) {
        return "p2".equals(side) ? this.entityData.get(DATA_P2_REMAIN) : this.entityData.get(DATA_P1_REMAIN);
    }

    public int graveyardCount(String side) {
        return "p2".equals(side) ? this.entityData.get(DATA_P2_GRAVE) : this.entityData.get(DATA_P1_GRAVE);
    }

    public int banishedCount(String side) {
        return "p2".equals(side) ? this.entityData.get(DATA_P2_BANISH) : this.entityData.get(DATA_P1_BANISH);
    }

    private void setInt(EntityDataAccessor<Integer> key, int value) {
        if (this.entityData.get(key) != value) {
            this.entityData.set(key, value);
        }
    }

    /** 决斗主机每帧推过来的实时统计：剩余卡组 / 墓地 / 除外。 */
    public void updateLiveStats(int draw0, int draw1, int grave0, int grave1,
                                int banish0, int banish1) {
        boolean p1 = this.entityData.get(DATA_P1_DECK);
        boolean p2 = this.entityData.get(DATA_P2_DECK);
        if (p1) {
            setInt(DATA_P1_REMAIN, Math.max(0, this.entityData.get(DATA_P1_MAIN) - draw0));
        }
        if (p2) {
            setInt(DATA_P2_REMAIN, Math.max(0, this.entityData.get(DATA_P2_MAIN) - draw1));
        }
        setInt(DATA_P1_GRAVE, p1 ? Math.max(0, grave0) : 0);
        setInt(DATA_P2_GRAVE, p2 ? Math.max(0, grave1) : 0);
        setInt(DATA_P1_BANISH, p1 ? Math.max(0, banish0) : 0);
        setInt(DATA_P2_BANISH, p2 ? Math.max(0, banish1) : 0);
    }

    /** Counts include searches, returns, milling, and extra-deck movement. */
    public void updateVisualCounts(int[] main, int[] extra, int[] grave, int[] banish) {
        setInt(DATA_P1_REMAIN, Math.max(0, main[0]));
        setInt(DATA_P2_REMAIN, Math.max(0, main[1]));
        setInt(DATA_P1_EXTRA, Math.max(0, extra[0]));
        setInt(DATA_P2_EXTRA, Math.max(0, extra[1]));
        setInt(DATA_P1_GRAVE, Math.max(0, grave[0]));
        setInt(DATA_P2_GRAVE, Math.max(0, grave[1]));
        setInt(DATA_P1_BANISH, Math.max(0, banish[0]));
        setInt(DATA_P2_BANISH, Math.max(0, banish[1]));
    }

    /** 把卡盒里的卡组放到某一侧；放上之后该侧的卡组模型才会出现。 */
    public void bindDeck(String side, DeckData deck) {
        boolean p2 = "p2".equals(side);
        this.entityData.set(p2 ? DATA_P2_DECK : DATA_P1_DECK, true);
        this.entityData.set(p2 ? DATA_P2_NAME : DATA_P1_NAME, deck.name());
        this.entityData.set(p2 ? DATA_P2_MAIN : DATA_P1_MAIN, deck.mainDeck().size());
        this.entityData.set(p2 ? DATA_P2_EXTRA : DATA_P1_EXTRA, deck.extraDeck().size());
        this.entityData.set(p2 ? DATA_P2_REMAIN : DATA_P1_REMAIN, deck.mainDeck().size());
        this.entityData.set(p2 ? DATA_P2_GRAVE : DATA_P1_GRAVE, 0);
        this.entityData.set(p2 ? DATA_P2_BANISH : DATA_P1_BANISH, 0);
        // A side becomes ready as soon as a valid deck is placed on its half.
        this.entityData.set(p2 ? DATA_P2_READY : DATA_P1_READY, true);
    }

    public boolean isReadyToStart() {
        return this.entityData.get(DATA_P1_READY) && this.entityData.get(DATA_P2_READY);
    }

    /** 把某一侧的卡组卸掉（决胜后重开用：让"准备"状态和 AI 自动补卡组重新走一遍）。 */
    public void unbindDeck(String side) {
        boolean p2 = "p2".equals(side);
        if (p2) {
            this.entityData.set(DATA_P2_AI, "");
            this.entityData.set(DATA_P2_AI_NAME, "");
            this.entityData.set(DATA_P2_AI_DIALOG, "");
        }
        this.entityData.set(p2 ? DATA_P2_DECK : DATA_P1_DECK, false);
        this.entityData.set(p2 ? DATA_P2_NAME : DATA_P1_NAME, "");
        this.entityData.set(p2 ? DATA_P2_MAIN : DATA_P1_MAIN, 0);
        this.entityData.set(p2 ? DATA_P2_EXTRA : DATA_P1_EXTRA, 0);
        this.entityData.set(p2 ? DATA_P2_REMAIN : DATA_P1_REMAIN, 0);
        this.entityData.set(p2 ? DATA_P2_GRAVE : DATA_P1_GRAVE, 0);
        this.entityData.set(p2 ? DATA_P2_BANISH : DATA_P1_BANISH, 0);
        this.entityData.set(p2 ? DATA_P2_READY : DATA_P1_READY, false);
        // 卡组都撤了，这一侧的座位自然也失效（下次放卡组时重新占座）
        clearSeat(side);
    }

    // ------------------------------------------------------------ 人机

    /** 远半场放上「人机卡组」：记住是哪个人机，卡组本身照常绑定。 */
    public void bindAiDeck(String key, String name, String dialog, DeckData deck) {
        this.entityData.set(DATA_P2_AI, key == null ? "" : key);
        this.entityData.set(DATA_P2_AI_NAME, name == null ? "" : name);
        this.entityData.set(DATA_P2_AI_DIALOG, dialog == null ? "" : dialog);
        // 远半场人机上桌：这一侧就不再是真人的座位了
        clearSeat("p2");
        bindDeck("p2", deck);
    }

    /** 远半场的人机卡组键；空 = 还没放。 */
    public String aiKey() {
        return this.entityData.get(DATA_P2_AI);
    }

    public String aiName() {
        return this.entityData.get(DATA_P2_AI_NAME);
    }

    public String aiDialog() {
        return this.entityData.get(DATA_P2_AI_DIALOG);
    }

    public boolean isGameStarted() { return this.entityData.get(DATA_GAME_STARTED); }
    public void setGameStarted(boolean value) { this.entityData.set(DATA_GAME_STARTED, value); }

    /** 0 = 没有结果（进行中/未开局），1 = 本侧胜利，2 = 本侧失败。 */
    public int duelResult() { return this.entityData.get(DATA_RESULT); }
    public void setDuelResult(int value) { this.entityData.set(DATA_RESULT, value < 0 || value > 2 ? 0 : value); }
    public boolean hasResult() { return duelResult() != 0; }

    /** 当前时点开关（右下角菜单里能改）。 */
    public DuelTimingMode timingMode() {
        return DuelTimingMode.byOrdinal(this.entityData.get(DATA_TIMING_MODE));
    }

    public void setTimingMode(DuelTimingMode mode) {
        this.entityData.set(DATA_TIMING_MODE, (mode == null ? DuelTimingMode.DEFAULT : mode).ordinal());
    }

    /** 当前规则模式（右下角菜单里能改）。 */
    public DuelRuleMode ruleMode() {
        return DuelRuleMode.byOrdinal(this.entityData.get(DATA_RULE_MODE));
    }

    public void setRuleMode(DuelRuleMode mode) {
        this.entityData.set(DATA_RULE_MODE, DuelRuleMode.RUSH.ordinal());
    }
    public boolean isReady(String side) { return "p2".equals(side) ? this.entityData.get(DATA_P2_READY) : this.entityData.get(DATA_P1_READY); }
    /** 手动改某一侧的"已准备"标记（决胜后重开时把对面退回未准备，等绿色按钮补 AI 卡组）。 */
    public void setReady(String side, boolean value) {
        this.entityData.set("p2".equals(side) ? DATA_P2_READY : DATA_P1_READY, value);
    }
    public int lifePoints(String side) { return "p2".equals(side) ? this.entityData.get(DATA_P2_LP) : this.entityData.get(DATA_P1_LP); }
    public void setLifePoints(String side, int value) {
        this.entityData.set("p2".equals(side) ? DATA_P2_LP : DATA_P1_LP, Math.max(0, value));
    }
    public String currentPhase(String side) { return "p2".equals(side) ? this.entityData.get(DATA_P2_PHASE) : this.entityData.get(DATA_P1_PHASE); }
    public void setCurrentPhase(String side, String phase) {
        this.entityData.set("p2".equals(side) ? DATA_P2_PHASE : DATA_P1_PHASE, phase == null ? "DP" : phase);
    }
    public void setTurnSide(int side) { this.entityData.set(DATA_TURN_SIDE, side & 1); }

    /** 由服务端每 tick 从 Duel Host 推过来：谁在决策、允许哪些阶段转移。 */
    public void setPhasePrompt(int promptedSeat, boolean toBattle, boolean toEnd, boolean toMain2) {
        this.entityData.set(DATA_PROMPT_SIDE, promptedSeat < 0 ? -1 : (promptedSeat & 1));
        this.entityData.set(DATA_TO_BATTLE, toBattle);
        this.entityData.set(DATA_TO_END, toEnd);
        this.entityData.set(DATA_TO_MAIN2, toMain2);
    }

    /** Seat currently allowed to answer, or -1 when no player decision is pending. */
    public int promptSeat() {
        return this.entityData.get(DATA_PROMPT_SIDE);
    }

    public void setPlaceZones(int mask) {
        this.entityData.set(DATA_PLACE_ZONES, mask);
    }

    public int placeZones() {
        return this.entityData.get(DATA_PLACE_ZONES);
    }

    /** 开局发牌演出：这段时间控制条显示「游戏中」，两边阶段键都不可按。 */
    public void setOpening(boolean opening) { this.entityData.set(DATA_OPENING, opening); }
    public boolean isOpening() { return this.entityData.get(DATA_OPENING); }

    /**
     * 该侧现在能不能把阶段推进到 phase。BP/M2/EP 之外一律 false：
     * 抽卡、准备、主要阶段不是玩家能“跳”过去的。
     */
    public boolean canAdvance(String side, String phase) {
        int prompted = this.entityData.get(DATA_PROMPT_SIDE);
        if (prompted < 0) return false;
        if (!side.equals(prompted == 1 ? "p2" : "p1")) return false;
        return switch (phase == null ? "" : phase.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "BP" -> this.entityData.get(DATA_TO_BATTLE);
            case "M2" -> this.entityData.get(DATA_TO_MAIN2);
            case "EP" -> this.entityData.get(DATA_TO_END);
            default -> false;
        };
    }

    /** Returns a UI state name used by the renderer: disabled, available, or current. */
    public String controlState(DuelFieldLayout.Control control) {
        String side = control.owner();
        if ("lp_display".equals(control.kind())) return "display";
        int result = duelResult();
        if ("confirm".equals(control.kind())) {
            // 决胜之后：按钮变成结果灯——本侧赢=金色「胜利」，本侧输=灰色「失败」；
            // 对面那一侧按镜像显示（双方各看各的）。右键这张键会回到「准备」开新一局。
            if (result != 0) {
                boolean mine = "p1".equals(side) ? result == 1 : result == 2;
                return mine ? "victory" : "defeat";
            }
            if (isOpening()) return "started";
            // 开局之后这张键变成回合指示灯：本侧回合绿色，对方回合红色
            if (isGameStarted()) {
                return side.equals(playerSideForTurn()) ? "my_turn" : "opponent_turn";
            }
            if (isReadyToStart()) return "started";
            return isReady(side) ? "ready" : "disabled";
        }
        if (result != 0) return "disabled";
        if (!isGameStarted() || isOpening()) return "disabled";
        if (!side.equals(playerSideForTurn())) return "disabled";
        if (control.phase().equalsIgnoreCase(currentPhase(side))) return "current";
        return canAdvance(side, control.phase()) ? "available" : "disabled";
    }

    /** Current turn side is deliberately kept simple until the host supplies turn metadata. */
    public String playerSideForTurn() { return this.entityData.get(DATA_TURN_SIDE) == 1 ? "p2" : "p1"; }

    /**
     * 按下一个控件（确认键 / 阶段键）。右键交互和客户端发来的点击都走这里，
     * 所以"能不能按、按了干什么"只有一份逻辑。
     *
     * @return true = 这个控件被处理了（不管成功还是只提示了一句）
     */
    public boolean activateControl(ServerPlayer player, DuelFieldLayout.Control control) {
        if (player == null || control == null) return false;
        if (!ownsControl(control, player)) return false;
        if ("confirm".equals(control.kind())) {
            handleConfirmClick(player);
            return true;
        }
        if (!"phase".equals(control.kind())) return false;
        String state = controlState(control);
        if ("available".equals(state)) {
            // 真正的阶段切换由规则核心执行：这里只提交它允许的应答，
            // 新阶段要等核心的 MSG_NEW_PHASE 回来才点亮。
            try {
                MDProMod.sessions().submitPhase(this, player, control.phase());
                player.displayClientMessage(Component.literal("已请求进入" + control.label()), true);
            } catch (Exception ex) {
                player.displayClientMessage(Component.literal("切换失败：" + ex.getMessage()), true);
            }
        } else if ("current".equals(state)) {
            player.displayClientMessage(Component.literal("已经是" + control.label() + "阶段"), true);
        } else {
            player.displayClientMessage(Component.literal("现在还不能切换到" + control.label()), true);
        }
        return true;
    }

    /**
     * 这个控件是不是"玩家自己那侧"的。
     *
     * <p>看的是**归属**（这块盘是人类在打，p1 就是他那半场），不看玩家站在桌子哪一边：
     * 以前用"站哪半场算哪边"的判断（{@code playerSide}），绕着桌子走一圈、或者站到中线
     * 另一边去看对面的墓地，自己的按键就全失灵了——"第一回合好用、第二回合点不动"
     * 很可能就是这么来的。以后真有第二个真人坐 p2，这里再改成按座位判断。
     */
    public boolean ownsControl(DuelFieldLayout.Control control) {
        return ownsControl(control, null);
    }

    /**
     * 这个玩家能不能按这个控件：控件属于哪一侧，就归坐在哪一侧的人。
     *
     * <p>以前这里写死"只认 p1"，是因为当时 {@code playerSide} 按站位算，
     * 走到桌子对面自己的键就失灵了（"第一回合好用、第二回合点不动"）。
     * 现在 {@link #playerSide} 先查 {@linkplain #setSeat 座位表}，站位不再影响身份，
     * 所以可以按实际控制者来判——联机时坐在 p2 的真人也就拿回了他那侧的键。
     * 没有坐位信息时（旁观者）退回几何判定，行为和以前一致。
     */
    public boolean ownsControl(DuelFieldLayout.Control control, Player player) {
        if (control == null) return false;
        String owner = control.owner();
        if (!"p1".equals(owner) && !"p2".equals(owner)) return false;
        return owner.equals(playerSide(player));
    }

    /** Server-side entry point used by the field button. The session manager starts the host. */
    public void handleConfirmClick(ServerPlayer player) {
        // 上一局已经决胜：右键这张键 = 收掉场面、回到「准备」，然后可以重新装填卡组开新一局
        if (hasResult()) {
            MDProMod.sessions().resetForNewDuel(player, this);
            return;
        }
        if (isGameStarted()) {
            // 这块盘上已经没有对局在跑了（重启 / 崩溃留下的"进行中"假状态）：直接退回「准备」，
            // 不然玩家点什么都只会听到"现在是我的回合"
            if (!MDProMod.sessions().hasLiveDuel(this)) {
                MDProMod.sessions().resetForNewDuel(player, this);
                return;
            }
            player.displayClientMessage(Component.literal(playerSide(player).equals(playerSideForTurn())
                    ? "现在是我的回合" : "现在是对手的回合"), true);
            return;
        }
        String side = playerSide(player);
        if (!isReady(side)) {
            player.displayClientMessage(Component.literal("先把卡组放到自己这一侧，按钮才会变绿"), true);
            return;
        }
        if (!isReadyToStart()) {
            // 远半场留给人机卡组：没放就等着，不再自动补预设卡组
            player.displayClientMessage(Component.literal(hasDeck("p2")
                    ? "双方都放入卡组后才能开始决斗" : "等待对手选择卡组"), true);
            return;
        }
        try {
            MDProMod.sessions().startFromField(player, this);
            player.displayClientMessage(Component.literal("决斗开始！"), true);
        } catch (Exception ex) {
            player.displayClientMessage(Component.literal("开始决斗失败：" + ex.getMessage()), true);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer) {
            // 右键点场上卡：决斗盘是个大实体，射线常常打在它身上——先看准星对着哪张卡，
            // 命中就把这次右键转交给那张卡（弹详情大图）。
            DuelCardEntity aimedCard = aimedCard(player);
            if (aimedCard != null) {
                // 卡在卡堆里（墓地 / 除外 / 额外）时，右键要看"这一堆的全部卡"，
                // 而不是只弹中箭的那一张。射线通常先打到堆里的卡实体，所以这里必须先分流。
                String aimedZone = aimedCard.getZoneId();
                if (isPile(aimedZone)) {
                    MDProMod.sessions().openPile(serverPlayer, this,
                            pileSide(aimedZone), pileLocation(aimedZone));
                    return InteractionResult.CONSUME;
                }
                aimedCard.interact(player, hand);
                return InteractionResult.CONSUME;
            }
            // 右键卡堆：看全部卡（额外卡组 / 墓地 / 除外）
            DuelFieldLayout.Zone pileZone = findAnyZone(player);
            if (pileZone != null && isPile(pileZone.id())) {
                MDProMod.sessions().openPile(serverPlayer, this,
                        pileSide(pileZone.id()), pileLocation(pileZone.id()));
                return InteractionResult.CONSUME;
            }
            DuelFieldLayout.Control control = findClickedControl(player);
            if (control != null) {
                if (ownsControl(control, player)) activateControl(serverPlayer, control);
                else player.displayClientMessage(Component.literal("这是对面那半场的按键"), true);
                return InteractionResult.CONSUME;
            }
            // 空手蹲下右键 = 收回决斗盘。放在卡/卡堆/按键**之后**：蹲着调阶段、看卡的时候
            // 不能手一滑把整块盘收了——想收就对着场地空白处蹲下右键
            if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty()
                    && player.getOffhandItem().isEmpty()) {
                MDProMod.sessions().reclaimField(serverPlayer, this);
                return InteractionResult.CONSUME;
            }
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() && player instanceof ServerPlayer serverPlayer) {
            MDProMod.sessions().linkField(serverPlayer.server, this, serverPlayer);
            String side = playerSide(player);
            player.displayClientMessage(Component.literal(
                    "已绑定决斗盘（空手右键）。现在用卡盒右键这里放下卡组；本侧"
                            + (hasDeck(side) ? "已有卡组：" + deckName(side)
                            : "还没有卡组")), false);
            return InteractionResult.CONSUME;
        }
        if (stack.getItem() instanceof CardCaseItem) {
            String side = playerSide(player);
            ServerPlayer seatPlayer = player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            // 两侧都允许真人放自己的卡组：坐在 p2 的真人现在可以自己带卡组上桌。
            // 放在远半场时把那侧可能挂着的人机撤掉（人机和真人不能同时占 p2）。
            if ("p2".equals(side) && !aiKey().isEmpty()) {
                this.entityData.set(DATA_P2_AI, "");
                this.entityData.set(DATA_P2_AI_NAME, "");
                this.entityData.set(DATA_P2_AI_DIALOG, "");
                player.displayClientMessage(Component.literal("已换下的人机，改由你的卡组上场"), true);
            }
            if (seatPlayer != null) {
                MDProMod.sessions().linkField(seatPlayer.server, this, seatPlayer);
            }
            DeckData deck = CardCaseItem.getDeck(stack);
            List<String> problems = DeckRules.problems(deck, ruleMode());
            if (!problems.isEmpty()) {
                player.displayClientMessage(Component.literal(
                        "卡组还没法放上场：" + String.join("；", problems)), false);
                return InteractionResult.FAIL;
            }
            bindDeck(side, deck);
            // 谁把卡组放到哪一侧，谁就坐那一边（桌子两侧的按键归属由此确定，不再看站位）
            setSeat(side, player);
            MDProMod.sessions().setPendingDeck(this, side, deck, seatPlayer);
            player.displayClientMessage(Component.literal(
                    "已把卡组「" + deck.name() + "」放到" + ("p1".equals(side) ? "近半场" : "远半场")
                            + "：主 " + deck.mainDeck().size() + " 张 / 额外 " + deck.extraDeck().size() + " 张"
                            + (isReadyToStart() ? "" : "（点自己那一侧的绿色按钮开始决斗）")),
                    false);
            // 两侧都有卡组时按规则自动开局；单人流程由绿色按钮补上 AI 卡组。
            if (isReadyToStart() && !isGameStarted() && player instanceof ServerPlayer starter) {
                try {
                    MDProMod.sessions().startFromField(starter, this);
                    player.displayClientMessage(Component.literal("两侧都有卡组，决斗开始！"), false);
                } catch (Exception ex) {
                    player.displayClientMessage(Component.literal("自动开始决斗失败：" + ex.getMessage()), false);
                }
            }
            return InteractionResult.CONSUME;
        }
        if (stack.getItem() instanceof AiDeckItem) {
            String side = playerSide(player);
            if (!"p2".equals(side)) {
                player.displayClientMessage(Component.literal("人机卡组要放到对面那半场"), true);
                return InteractionResult.FAIL;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                MDProMod.sessions().linkField(serverPlayer.server, this, serverPlayer);
            }
            String key = AiDeckItem.getKey(stack);
            java.nio.file.Path root = DuelSessionManager.runtimeRoot();
            AiDeckCatalog.Entry entry = AiDeckCatalog.find(root, key);
            try {
                DeckData deck = AiDeckCatalog.deck(root, key);
                bindAiDeck(key, entry == null ? "MDProBot" : entry.name(),
                        entry == null ? "default" : entry.dialog(), deck);
                player.displayClientMessage(Component.literal(
                        "人机「" + AiDeckItem.getLabel(stack) + "」已上场：主 "
                                + deck.mainDeck().size() + " 张 / 额外 " + deck.extraDeck().size() + " 张"
                                + (isReadyToStart() ? "" : "（点自己那一侧的绿色按钮开始决斗）")), false);
            } catch (Exception error) {
                player.displayClientMessage(Component.literal(
                        "放人机卡组失败：" + error.getMessage()), false);
                return InteractionResult.FAIL;
            }
            if (isReadyToStart() && !isGameStarted() && player instanceof ServerPlayer starter) {
                try {
                    MDProMod.sessions().startFromField(starter, this);
                    player.displayClientMessage(Component.literal("两侧都就位，决斗开始！"), false);
                } catch (Exception ex) {
                    player.displayClientMessage(Component.literal("自动开始决斗失败：" + ex.getMessage()), false);
                }
            }
            return InteractionResult.CONSUME;
        }
        if (stack.getItem() instanceof DuelCardItem cardItem) {
            return placeCard(player, cardItem.getCardCode(stack), stack)
                    ? InteractionResult.CONSUME : InteractionResult.FAIL;
        }
        String p1 = this.entityData.get(DATA_P1_DECK)
                ? "近半场 " + this.entityData.get(DATA_P1_NAME)
                        + "（主 " + this.entityData.get(DATA_P1_MAIN) + "）"
                : "近半场空";
        String p2 = this.entityData.get(DATA_P2_DECK)
                ? "远半场 " + this.entityData.get(DATA_P2_NAME)
                        + "（主 " + this.entityData.get(DATA_P2_MAIN) + "）"
                : "远半场空";
        player.displayClientMessage(Component.literal(
                "决斗盘 " + DuelFieldLayout.zoneCount() + " 区：" + p1 + " / " + p2
                        + "（把卡盒右键上来放卡组）"), false);
        return InteractionResult.SUCCESS;
    }

    /** 左键点场地格子：核心正在问落点时，选中的格子就作为落点提交。 */
    public void onLeftClick(ServerPlayer player) {
        // 先看准星是不是对着场上的某张卡：是的话转交给那张卡（弹动作按钮/选卡）
        DuelCardEntity aimed = aimedCard(player);
        if (aimed != null) {
            // 卡在卡堆里（墓地 / 除外 / 额外）时，左键也是开"整堆清单"：
            // 界面里每张现在能用的卡都有一个按钮（发动 / 特殊召唤），点了才真的提交。
            String aimedZone = aimed.getZoneId();
            if (isPile(aimedZone)) {
                MDProMod.sessions().openPile(player, this, pileSide(aimedZone), pileLocation(aimedZone));
                return;
            }
            aimed.onLeftClick(player);
            return;
        }
        // 左键点卡堆：额外卡组 / 墓地 / 除外 → 打开交互界面（不管有没有在问落点）
        DuelFieldLayout.Zone pile = findAnyZone(player);
        if (pile != null && isPile(pile.id())) {
            MDProMod.sessions().openPile(player, this, pileSide(pile.id()), pileLocation(pile.id()));
            return;
        }
        if (!MDProMod.sessions().hasPlacePrompt()) return;
        DuelFieldLayout.Zone zone = findClickedZone(player);
        if (zone == null) return;
        int[] address = DuelFieldLayout.engineAddress(zone.id(),
                playerSide(player).equals("p2") ? 1 : 0, ruleMode());
        try {
            if (address != null
                    && MDProMod.sessions().submitPlaceFor(address[0], address[1], address[2])) {
                player.displayClientMessage(Component.literal("落点已选定"), true);
            } else {
                player.displayClientMessage(Component.literal("这个格子不是合法落点"), true);
            }
        } catch (Exception ex) {
            player.displayClientMessage(Component.literal("选区域失败：" + ex.getMessage()), true);
        }
    }

    /** 玩家准星对着的场上卡（4 格以内）；没有就返回 null。 */
    private static boolean isPile(String zoneId) {
        return zoneId.endsWith("_extra") || zoneId.endsWith("_graveyard") || zoneId.endsWith("_banished");
    }

    private static int pileLocation(String zoneId) {
        if (zoneId.endsWith("_extra")) return 64;
        return zoneId.endsWith("_graveyard") ? 16 : 32;
    }

    /** 卡堆属于哪一侧：0 = 近半场（自己），1 = 远半场。 */
    private static int pileSide(String zoneId) {
        return zoneId.startsWith("p2_") ? 1 : 0;
    }

    private DuelCardEntity aimedCard(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(4.0));
        DuelCardEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (DuelCardEntity card : this.level().getEntitiesOfClass(DuelCardEntity.class,
                this.getBoundingBox().inflate(20))) {
            var clip = card.getBoundingBox().inflate(0.15).clip(eye, end);
            if (clip.isEmpty()) continue;
            double distance = clip.get().distanceToSqr(eye);
            if (distance >= bestDistance) continue;
            bestDistance = distance;
            best = card;
        }
        return best;
    }

    private DuelFieldLayout.Control findClickedControl(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        float yaw = (float) Math.toRadians(this.getYRot());
        Vec3 localEye = eye.subtract(this.position()).yRot(yaw);
        Vec3 localLook = look.yRot(yaw);
        return DuelFieldControls.clickedControl(localEye, localLook, 48.0);
    }

    /**
     * 客户端也要用：准星对着哪个控件（和交互时的命中判定同一套算法）。
     * 右下角菜单靠它判断"这一下是不是点在确认键上"。
     */
    public DuelFieldLayout.Control controlUnderCrosshair(Player player) {
        return findClickedControl(player);
    }

    // ------------------------------------------------------------ 放卡

    public boolean placeCard(Player player, int code, ItemStack stack) {
        CardInfo info = CardRegistry.get(code).orElse(null);
        if (info == null) {
            player.displayClientMessage(Component.literal("Unknown card: " + code), false);
            return false;
        }
        DuelFieldLayout.Zone zone = findClickedZone(player);
        if (zone == null) {
            player.displayClientMessage(Component.literal(
                    "Point at a specific field zone and right-click again"), false);
            return false;
        }
        String side = playerSide(player);
        if (!isLegalZone(info, zone, side)) {
            player.displayClientMessage(Component.literal(
                    "That zone is not legal for " + info.name()
                            + " under the current placement rules"), false);
            return false;
        }
        if (isZoneOccupied(zone.id())) {
            player.displayClientMessage(Component.literal("That zone is already occupied"), false);
            return false;
        }
        DuelCardEntity card = ModEntities.DUEL_CARD.get().create(this.level());
        if (card == null) return false;
        Vec3 position = zoneWorldPosition(zone);
        card.setCardCode(code);
        card.setFaceUp(true);
        card.setDefense(false);
        card.setZoneId(zone.id());
        card.moveTo(position.x, position.y, position.z, this.getYRot(), 0.0F);
        this.level().addFreshEntity(card);
        if (!player.isCreative()) stack.shrink(1);
        player.displayClientMessage(Component.literal("Placed " + info.name() + " at " + zone.id()), false);
        return true;
    }

    private DuelFieldLayout.Zone findClickedZone(Player player) {
        return findZone(player, true);
    }

    /** 任意格子（包含卡组/额外/墓地/除外这些不能放卡的堆）。 */
    private DuelFieldLayout.Zone findAnyZone(Player player) {
        return findZone(player, false);
    }

    private DuelFieldLayout.Zone findZone(Player player, boolean placementOnly) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        double planeY = this.getY() + 0.02;
        if (Math.abs(look.y) < 1.0E-6) return null;
        double t = (planeY - eye.y) / look.y;
        if (t < 0.0 || t > 48.0) return null;
        Vec3 hit = eye.add(look.scale(t));
        Vec3 local = new Vec3(hit.x - this.getX(), 0.0, hit.z - this.getZ())
                .yRot((float) Math.toRadians(this.getYRot()));
        double modelX = local.x * 16.0;
        double modelZ = local.z * 16.0;
        DuelFieldLayout.Zone best = null;
        double bestDistance = Double.MAX_VALUE;
        for (DuelFieldLayout.Zone zone : DuelFieldLayout.zones()) {
            if (placementOnly && !isPlacementZone(zone.kind())) continue;
            double dx = zone.x() - modelX;
            double dz = zone.z() - modelZ;
            double distance = dx * dx + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = zone;
            }
        }
        return bestDistance <= 15.0 * 15.0 ? best : null;
    }

    /** 坐在某一侧的真人玩家 UUID；空串 = 这侧还没有真人占座（人机占的那边就是空串）。 */
    public String seatUuid(String side) {
        return "p2".equals(side) ? this.entityData.get(DATA_P2_UUID)
                : this.entityData.get(DATA_P1_UUID);
    }

    /** 这一侧坐的是人还是人机。服务端开局/AI 占位都看这个。 */
    public boolean seatTaken(String side) {
        return !seatUuid(side).isEmpty();
    }

    /**
     * 把某个真人登记到一侧。同一张盘上一个人只能占一边，
     * 占新一侧的时候自动腾出他原先占的那边（走过桌子换个座位想重来的情况）。
     */
    public void setSeat(String side, Player player) {
        if (player == null) return;
        boolean p2 = "p2".equals(side);
        EntityDataAccessor<String> target = p2 ? DATA_P2_UUID : DATA_P1_UUID;
        EntityDataAccessor<String> other = p2 ? DATA_P1_UUID : DATA_P2_UUID;
        String uuid = player.getUUID().toString();
        if (uuid.equals(this.entityData.get(other))) {
            this.entityData.set(other, "");
            MDProMod.sessions().forgetSeat(this, p2 ? "p1" : "p2");
        }
        if (uuid.equals(this.entityData.get(target))) return;
        this.entityData.set(target, uuid);
        if (player instanceof ServerPlayer serverPlayer) {
            MDProMod.sessions().rememberSeat(this, side, serverPlayer);
        }
    }

    /** 收摊：两侧座位清空（比如一局打完取卡组）。 */
    public void clearSeat(String side) {
        boolean p2 = "p2".equals(side);
        this.entityData.set(p2 ? DATA_P2_UUID : DATA_P1_UUID, "");
        MDProMod.sessions().forgetSeat(this, side);
    }

    /**
     * 这个玩家属于哪一侧。
     *
     * <p>优先按 {@link #setSeat} 登记的座位表判断，这样对联机才是稳定的身份：
     * 走到桌子对面、绕过去看一眼对方的墓地，都不会把自己的按键弄丢。
     * 没登记过座位的（单人重力档、纯旁观者）才退回原来的几何判定。
     */
    public String playerSide(Player player) {
        if (player != null) {
            String uuid = player.getUUID().toString();
            if (uuid.equals(this.entityData.get(DATA_P1_UUID))) return "p1";
            if (uuid.equals(this.entityData.get(DATA_P2_UUID))) return "p2";
            Vec3 local = new Vec3(
                    player.getX() - this.getX(), 0.0, player.getZ() - this.getZ())
                    .yRot((float) Math.toRadians(this.getYRot()));
            return local.z >= 0.0 ? "p1" : "p2";
        }
        // 没有玩家上下文（服务端兜底判定）：默认按近半场
        return "p1";
    }

    private boolean isPlacementZone(String kind) {
        return "monster".equals(kind)
                || "spell_trap".equals(kind)
                || "extra_monster".equals(kind)
                || "field".equals(kind);
    }

    private boolean isLegalZone(CardInfo info, DuelFieldLayout.Zone zone, String side) {
        String owner = zone.owner();
        String kind = zone.kind();
        if (info.isMonster()) {
            boolean pendulum = (info.type() & 0x1000000) != 0;
            boolean link = (info.type() & 0x4000000) != 0;
            if (link) {
                return "extra_monster".equals(kind) && "shared".equals(owner);
            }
            if (pendulum) {
                if ("monster".equals(kind) && side.equals(owner)) return true;
                if ("spell_trap".equals(kind) && side.equals(owner) && isPendulumZone(zone.id())) {
                    return true;
                }
            }
            if (info.isExtraDeck() && "extra_monster".equals(kind) && "shared".equals(owner)) {
                return true;
            }
            return "monster".equals(kind) && side.equals(owner);
        }
        if (info.isSpell()) {
            if ((info.type() & 0x80000) != 0) {
                return "field".equals(kind) && side.equals(owner);
            }
            return "spell_trap".equals(kind) && side.equals(owner);
        }
        if (info.isTrap()) {
            return "spell_trap".equals(kind) && side.equals(owner);
        }
        return false;
    }

    private boolean isPendulumZone(String zoneId) {
        return zoneId.endsWith("_0") || zoneId.endsWith("_4");
    }

    private boolean isZoneOccupied(String zoneId) {
        List<DuelCardEntity> cards = this.level().getEntitiesOfClass(
                DuelCardEntity.class, this.getBoundingBox().inflate(2.0));
        for (DuelCardEntity card : cards) {
            if (zoneId.equals(card.getZoneId())) return true;
        }
        return false;
    }

    /** 区域中心的实际世界坐标（含决斗盘朝向）。 */
    public Vec3 zoneWorldPosition(DuelFieldLayout.Zone zone) {
        Vec3 local = new Vec3(zone.x() / 16.0, 0.0, zone.z() / 16.0)
                .yRot(-(float) Math.toRadians(this.getYRot()));
        return new Vec3(this.getX() + local.x, this.getY() + 0.03, this.getZ() + local.z);
    }
}
