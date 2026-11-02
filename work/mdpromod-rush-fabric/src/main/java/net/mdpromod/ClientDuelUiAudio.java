package net.mdpromod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

/** Private interface feedback; never broadcast card selection to the opponent. */
public final class ClientDuelUiAudio {
    private ClientDuelUiAudio() {
    }

    private static boolean duelPrompt(Screen screen) {
        return screen instanceof DecisionScreen || screen instanceof ChainChoiceScreen
                || screen instanceof SelectCardScreen || screen instanceof UnselectCardScreen
                || screen instanceof SortCardScreen || screen instanceof OptionScreen
                || screen instanceof PositionScreen || screen instanceof PileScreen;
    }

    private static boolean cardChoice(Screen screen) {
        return screen instanceof ChainChoiceScreen || screen instanceof SelectCardScreen
                || screen instanceof UnselectCardScreen || screen instanceof SortCardScreen
                || screen instanceof PileScreen;
    }

    static void screenChanged(Screen previous, Screen next) {
        if (next == previous) return;
        if (cardChoice(previous) && (next == null || next.getClass() != previous.getClass())) {
            ClientDuelAudio.playUi("SE_MENU_SLIDE_04");
        }
        if (cardChoice(next) && (previous == null || previous.getClass() != next.getClass())) {
            ClientDuelAudio.playUi("SE_MENU_SLIDE_03");
        }
        if (next instanceof DecisionScreen || next instanceof OptionScreen
                || next instanceof PositionScreen) {
            ClientDuelAudio.playUi("SE_SYS_VERIFY");
        }
    }

    static void keyPressed(Screen screen, int keyCode) {
        if (keyCode == 256 && duelPrompt(screen) && screen.shouldCloseOnEsc()) {
            ClientDuelAudio.playUi("SE_DUEL_CANCEL");
        }
    }

    public static boolean replaceButtonSound(AbstractWidget widget) {
        if (!Boolean.parseBoolean(System.getProperty("mdpromod.duelAudio", "true"))
                || !duelPrompt(Minecraft.getInstance().screen)) {
            return false;
        }
        String label = widget.getMessage().getString();
        boolean cancel = label.equals("取消") || label.equals("否") || label.equals("关闭");
        ClientDuelAudio.playUi(cancel ? "SE_DUEL_CANCEL" : "SE_DUEL_DECIDE");
        return true;
    }
}
