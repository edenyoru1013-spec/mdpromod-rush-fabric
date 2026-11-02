package net.mdpromod;

import net.minecraft.client.Minecraft;

/** 客户端镜像：把人机卡组列表交给界面。 */
public final class ClientAiDeckBridge {

    private ClientAiDeckBridge() {
    }

    public static void open(ModNetwork.AiDeckOpen message) {
        Minecraft.getInstance().setScreen(new AiDeckScreen(message));
    }
}
