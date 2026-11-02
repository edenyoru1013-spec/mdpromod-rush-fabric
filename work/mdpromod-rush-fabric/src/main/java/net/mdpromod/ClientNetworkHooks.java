package net.mdpromod;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

/** Keeps every client-only networking type out of dedicated-server class loading. */
final class ClientNetworkHooks {
    private ClientNetworkHooks() {
    }

    static void registerReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetwork.CHANNEL,
                (client, handler, buf, responseSender) -> {
                    String type = buf.readUtf(128);
                    byte[] data = new byte[buf.readableBytes()];
                    buf.readBytes(data);
                    client.execute(() -> ModNetwork.receive(type, data, null));
                });
    }

    static void send(FriendlyByteBuf buffer) {
        ClientPlayNetworking.send(ModNetwork.CHANNEL, buffer);
    }

    static void run(Runnable task) {
        Minecraft.getInstance().execute(task);
    }
}
