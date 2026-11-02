package net.mdpromod;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Client hooks used by the Fabric callbacks and the mouse mixin. */
public final class ClientForgeEvents {
    private static final boolean HAND_HUD =
            Boolean.parseBoolean(System.getProperty("mdpromod.handHud", "false"));

    private ClientForgeEvents() {
    }

    static void tick(Minecraft minecraft) {
        ClientCardSelect.tick(minecraft);
        ClientPileHints.tick(minecraft);
        ClientCardMarkers.tick(minecraft);
        ClientFieldMenu.tick(minecraft);
        ClientSummonEffects.tick(minecraft);
        ClientCutinEffects.tick(minecraft);
        ClientDuelAudio.tick(minecraft);
        ClientPromptWatch.tick(minecraft);
        ClientBundleNotice.tick(minecraft);
    }

    static void renderHud(GuiGraphics graphics, float tickDelta) {
        if (HAND_HUD) HandHudOverlay.render(graphics);
    }

    static void renderWorld(WorldRenderContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        var poses = context.matrixStack();
        ClientCardMenu.render(minecraft, poses);
        ClientFieldMenu.render(minecraft, poses);
        ClientPileHints.render(minecraft, poses);
        ClientCardMarkers.render(minecraft, poses, context.tickDelta());
        ClientCardSelect.render(minecraft, poses);
        ClientChainState.render(minecraft, poses);
        ClientCloseups.render(minecraft, poses);
        ClientSummonEffects.render(minecraft, poses);
        ClientCutinEffects.render(minecraft, poses);
    }

    public static boolean onMouseButton(int button, int action) {
        if (action != 1) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null) return false;
        if (ClientFieldMenu.handleMouse(minecraft, button)) {
            return true;
        }
        if (button != 0) return false;
        if (ClientCardMenu.isOpen() && ClientCardMenu.mouseClicked(minecraft)) {
            return true;
        }
        if (!HAND_HUD) return false;
        double[] mouse = HandHudOverlay.mousePosition(minecraft);
        return HandHudOverlay.mouseClicked(mouse[0], mouse[1], button);
    }
}
