package net.mdpromod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 选卡清单（MC 弹窗风格）：候选卡以卡图网格排列，左键点选，金色边框表示已选。
 * 卡组/额外卡组里的卡没有世界实体，靠这个界面选。
 */
public class SelectCardScreen extends Screen {
    private static final int COLS = 6;
    private static final int GAP = 4;

    private int[] codes;
    /** 每张候选卡在哪（和 codes 一一对应；可能为空数组）。 */
    private int[] addrs;
    private int min;
    private int max;
    private final boolean announce;
    private final List<Integer> chosen = new ArrayList<>();
    private String query = "";
    private EditBox searchBox;
    private int page;
    private int rows = 3;
    private int panelLeft;
    private int panelTop;
    private int panelW;
    private int panelH;
    private int gridLeft;
    private int gridTop;

    public SelectCardScreen(int[] codes, int min, int max) {
        this(codes, min, max, new int[0], false);
    }

    public SelectCardScreen(int[] codes, int min, int max, int[] addrs) {
        this(codes, min, max, addrs, false);
    }

    public SelectCardScreen(int[] codes, int min, int max, int[] addrs, boolean announce) {
        super(Component.literal(announce ? "宣言卡名" : "选择卡片"));
        this.codes = codes == null ? new int[0] : codes;
        this.addrs = addrs == null ? new int[0] : addrs;
        this.min = Math.max(1, min);
        this.max = Math.max(this.min, max);
        this.announce = announce;
    }

    boolean announceMode() {
        return announce;
    }

    /** 核心 RETRY 后重发同一提示时，只刷新候选；同列表会保留页码与已选项。 */
    void update(int[] newCodes, int newMin, int newMax, int[] newAddrs) {
        int[] nextCodes = newCodes == null ? new int[0] : newCodes;
        int[] nextAddrs = newAddrs == null ? new int[0] : newAddrs;
        if (!java.util.Arrays.equals(codes, nextCodes)) {
            codes = nextCodes;
            addrs = nextAddrs;
            chosen.clear();
            page = 0;
        } else if (!java.util.Arrays.equals(addrs, nextAddrs)) {
            addrs = nextAddrs;
        }
        min = Math.max(1, newMin);
        max = Math.max(min, newMax);
        page = Math.min(page, pages() - 1);
    }

    private int pages() {
        int perPage = COLS * Math.max(1, rows);
        return Math.max(1, (matchingIndices().length + perPage - 1) / perPage);
    }

    private int[] matchingIndices() {
        if (!announce || query.isBlank()) {
            int[] all = new int[codes.length];
            for (int i = 0; i < all.length; i++) all[i] = i;
            return all;
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<Integer> matches = new ArrayList<>();
        for (int i = 0; i < codes.length; i++) {
            CardInfo info = CardRegistry.get(codes[i]).orElse(null);
            String name = info == null ? "" : info.name().toLowerCase(Locale.ROOT);
            if (name.contains(needle) || Integer.toString(codes[i]).startsWith(needle)) {
                matches.add(i);
            }
        }
        return matches.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    protected void init() {
        // 每页行数按可用高度算，卡再多也不会顶出面板
        rows = Math.max(1, (this.height - 120) / (McPanel.CARD_H + GAP));
        panelW = Math.min(Math.max(this.width - 40, 300), COLS * (McPanel.CARD_W + GAP) + 10);
        int searchH = announce ? 24 : 0;
        panelH = Math.min(Math.max(this.height - 40, 200),
                rows * (McPanel.CARD_H + GAP) + 76 + searchH);
        panelLeft = (this.width - panelW) / 2;
        panelTop = (this.height - panelH) / 2;
        gridLeft = panelLeft + (panelW - COLS * (McPanel.CARD_W + GAP)) / 2 + GAP / 2;
        gridTop = panelTop + 26 + searchH;
        int bottom = panelTop + panelH - 24;
        if (announce) {
            searchBox = new EditBox(font, panelLeft + 8, panelTop + 22,
                    panelW - 16, 16, Component.literal("卡名或卡号"));
            searchBox.setHint(Component.literal("输入卡名或卡号筛选"));
            searchBox.setMaxLength(32);
            searchBox.setValue(query);
            searchBox.setResponder(value -> {
                query = value;
                page = 0;
            });
            addRenderableWidget(searchBox);
        }
        addRenderableWidget(Button.builder(Component.literal("上一页"), button -> {
            page = Math.max(0, page - 1);
        }).bounds(panelLeft + 8, bottom, 60, 18).build());
        addRenderableWidget(Button.builder(Component.literal("下一页"), button -> {
            page = Math.min(pages() - 1, page + 1);
        }).bounds(panelLeft + 72, bottom, 60, 18).build());
        addRenderableWidget(Button.builder(Component.literal("确认"), button -> confirm())
                .bounds(panelLeft + panelW - 78, bottom, 70, 18).build());
    }

    /** 滚轮翻页（卡图多时主要靠它）。 */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        page = Math.max(0, Math.min(pages() - 1, page - (int) Math.signum(delta)));
        return true;
    }

    private void toggle(int index) {
        ClientDuelAudio.playUi("SE_DUEL_SELECT");
        if (!chosen.remove(Integer.valueOf(index))) {
            if (chosen.size() >= max) return;
            chosen.add(index);
        }
        if (chosen.size() >= max) {
            confirm();
        }
    }

    private void confirm() {
        if (chosen.size() < min) return;
        ClientDuelAudio.playUi("SE_DUEL_DECIDE");
        int[] indices = chosen.stream().mapToInt(Integer::intValue).toArray();
        if (announce) {
            int index = indices.length == 0 ? -1 : indices[0];
            if (index < 0 || index >= codes.length) return;
            ModNetwork.sendToServer(new ModNetwork.AnnounceChoose(codes[index]));
        } else {
            ModNetwork.sendToServer(new ModNetwork.SelectChoose(indices));
        }
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int[] matches = matchingIndices();
        if (button == 0) {
            int perPage = COLS * Math.max(1, rows);
            int start = page * perPage;
            for (int i = 0; i < perPage && start + i < matches.length; i++) {
                int x = gridLeft + (i % COLS) * (McPanel.CARD_W + GAP);
                int y = gridTop + (i / COLS) * (McPanel.CARD_H + GAP);
                if (McPanel.inside(mouseX, mouseY, x, y, McPanel.CARD_W, McPanel.CARD_H)) {
                    toggle(matches[start + i]);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        McPanel.panel(graphics, panelLeft, panelTop, panelLeft + panelW, panelTop + panelH);
        String heading = announce ? "宣言卡名" : "选择卡片";
        graphics.drawCenteredString(font, heading + "　已选 " + chosen.size() + " / " + min + ".." + max
                + "　（第 " + (page + 1) + "/" + pages() + " 页）",
                panelLeft + panelW / 2, panelTop + 8, 0x404040);
        int[] matches = matchingIndices();
        int perPage = COLS * Math.max(1, rows);
        int start = page * perPage;
        for (int i = 0; i < perPage && start + i < matches.length; i++) {
            int original = matches[start + i];
            int x = gridLeft + (i % COLS) * (McPanel.CARD_W + GAP);
            int y = gridTop + (i / COLS) * (McPanel.CARD_H + GAP);
            boolean picked = chosen.contains(original);
            McPanel.cardInSlot(graphics, codes[original], x, y, picked);
            // 卡面标记：连锁中/被指定（和世界里的边框同一套颜色）
            if (original < addrs.length) {
                ClientCardMarkers.drawGuiMark(graphics, x, y, McPanel.CARD_W, McPanel.CARD_H,
                        ClientCardMarkers.markAt(addrs[original]));
            }
            if (picked) {
                graphics.drawString(font, "✔", x + McPanel.CARD_W - 9, y + 2, 0xFFFF00);
            }
        }
        // 悬停时显示卡名 + 它在哪，方便辨认（尤其是同名卡/墓地里的卡）
        for (int i = 0; i < perPage && start + i < matches.length; i++) {
            int x = gridLeft + (i % COLS) * (McPanel.CARD_W + GAP);
            int y = gridTop + (i / COLS) * (McPanel.CARD_H + GAP);
            if (McPanel.inside(mouseX, mouseY, x, y, McPanel.CARD_W, McPanel.CARD_H)) {
                int index = matches[start + i];
                int code = codes[index];
                String name = McPanel.displayName(code);
                if (index < addrs.length) {
                    List<net.minecraft.util.FormattedCharSequence> lines = new ArrayList<>(List.of(
                            Component.literal(name).getVisualOrderText(),
                            Component.literal(DuelLocation.tooltipLine(addrs[index], ClientDuelSeat.get()))
                                    .getVisualOrderText()));
                    String mark = DuelMarks.label(ClientCardMarkers.markAt(addrs[index]));
                    if (!mark.isEmpty()) {
                        lines.add(Component.literal("状态：" + mark).getVisualOrderText());
                    }
                    graphics.renderTooltip(font, lines, mouseX, mouseY);
                } else {
                    graphics.renderTooltip(font, Component.literal(name), mouseX, mouseY);
                }
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
