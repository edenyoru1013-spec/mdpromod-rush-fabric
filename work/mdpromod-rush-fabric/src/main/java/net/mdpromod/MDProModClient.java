package net.mdpromod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;

public final class MDProModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModNetwork.registerClient();
        EntityRendererRegistry.register(ModEntities.DUEL_FIELD.get(), DuelFieldRenderer::new);
        EntityRendererRegistry.register(ModEntities.CARD_CASE.get(), CardCaseRenderer::new);
        EntityRendererRegistry.register(ModEntities.DUEL_CARD.get(), DuelCardRenderer::new);
        EntityRendererRegistry.register(ModEntities.HAND_CARD.get(), HandCardRenderer::new);

        ClientTickEvents.END_CLIENT_TICK.register(ClientForgeEvents::tick);
        HudRenderCallback.EVENT.register(ClientForgeEvents::renderHud);
        WorldRenderEvents.LAST.register(ClientForgeEvents::renderWorld);
        WorldRenderEvents.LAST.register(ClientDuelAnimationRenderer::render);

        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) ->
                ClientDuelUiAudio.screenChanged(client.screen, screen));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientAnimBridge.clear();
            ClientCardMarkers.clear();
            ClientSummonEffects.stopField(Integer.MIN_VALUE);
            ClientCutinEffects.stop();
        });
    }
}
