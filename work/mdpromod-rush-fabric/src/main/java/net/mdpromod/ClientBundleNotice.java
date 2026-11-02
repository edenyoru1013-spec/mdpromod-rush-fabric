package net.mdpromod;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Reports the one-time runtime unpack without blocking the client thread. */
public final class ClientBundleNotice {
    private static boolean doneReported;
    private static boolean failureReported;

    private ClientBundleNotice() {
    }

    static void tick(Minecraft minecraft) {
        if (minecraft.player == null) return;
        ModBundle.State state = ModBundle.state();
        if (state == ModBundle.State.RUNNING) {
            minecraft.player.displayClientMessage(
                    Component.literal("MDProMod 正在解包资源：" + ModBundle.progressText()), true);
        } else if (state == ModBundle.State.DONE && !doneReported) {
            doneReported = true;
            if (ModBundle.bundled()) {
                minecraft.player.displayClientMessage(
                        Component.literal("MDProMod 资源准备完成（" + ModBundle.detail() + "）"), false);
            }
        } else if (state == ModBundle.State.FAILED && !failureReported) {
            failureReported = true;
            minecraft.player.displayClientMessage(
                    Component.literal("MDProMod 资源解包失败：" + ModBundle.detail()), false);
        }
    }
}
