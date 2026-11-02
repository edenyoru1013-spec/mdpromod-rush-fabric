package net.mdpromod;

/**
 * 把"卡在哪"说成人话：自己/对方的怪兽区 1、墓地、除外区、手牌……
 *
 * <p>打包格式和落点提示一致：{@code controller << 16 | location << 8 | sequence}。
 * 纯函数，可离线测试。
 *
 * <p>Rush 独立版：全局只有 Rush 一种规则，所以这里直接用<b>引擎序号本身</b>报列号，
 * 和 {@link DuelFieldLayout} 的恒等映射、以及 {@link DuelSessionManager#placeLabel} 完全对齐。
 * 以前这里写死 {@code sequence + 1}（那是大师规则的编号习惯），
 * 会把 Rush 最左那一列叫成「怪兽区2」，和绿色高亮的位置对不上。
 */
final class DuelLocation {

    private DuelLocation() {
    }

    /** {@code 0x000408} → 「自己墓地」这类。 */
    static String label(int packed) {
        return label(packed, 0);
    }

    static String label(int packed, int viewerSeat) {
        int controller = (packed >> 16) & 1;
        int location = (packed >> 8) & 0xFF;
        int sequence = packed & 0xFF;
        String who = controller == (viewerSeat & 1) ? "自己" : "对方";
        return switch (location) {
            case 1 -> who + "卡组";
            case 2 -> who + "手牌";
            // Rush 的三列就是引擎序号 1/2/3，不再是 sequence + 1
            case 4 -> DuelFieldLayout.isSelectableColumn(sequence, DuelRuleMode.RUSH)
                    ? who + "怪兽区" + sequence
                    : who + "怪兽区(第 " + sequence + " 格)";
            case 8 -> sequence >= 1 && sequence <= 3
                    ? who + "魔陷区" + sequence
                    : who + "魔陷区(第 " + sequence + " 格)";
            case 16 -> who + "墓地";
            case 32 -> who + "除外区";
            case 64 -> who + "额外卡组";
            default -> who + "区域 " + location + "-" + sequence;
        };
    }

    /** 选卡列表悬停时显示的两行：卡名 + 位置。 */
    static String tooltipLine(int packed) {
        return tooltipLine(packed, 0);
    }

    static String tooltipLine(int packed, int viewerSeat) {
        return "位置：" + label(packed, viewerSeat);
    }
}
