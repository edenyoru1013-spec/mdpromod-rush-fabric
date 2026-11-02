package net.mdpromod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

public final class ClientPromptWatch {
    private static final long MIN_INTERVAL_MS = 1500L;
    private static long lastAskAt;

    private ClientPromptWatch() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return;
        if (minecraft.screen instanceof RequiredAnswer || minecraft.screen instanceof ChatScreen) return;
        DuelFieldEntity field = waitingForMe(minecraft);
        if (field == null) return;
        long now = System.currentTimeMillis();
        if (now - lastAskAt < MIN_INTERVAL_MS) return;
        lastAskAt = now;
        ModNetwork.sendToServer(new ModNetwork.PromptResend());
    }

    private static DuelFieldEntity waitingForMe(Minecraft minecraft) {
        for (DuelFieldEntity field : minecraft.level.getEntitiesOfClass(
                DuelFieldEntity.class, minecraft.player.getBoundingBox().inflate(48.0))) {
            if (!field.isGameStarted() || field.hasResult()) continue;
            int promptSide = field.promptSeat();
            if (promptSide < 0) continue;
            boolean secondPlayer = "p2".equals(field.playerSide(minecraft.player));
            if (promptSide == (secondPlayer ? 1 : 0)) return field;
        }
        return null;
    }
}
