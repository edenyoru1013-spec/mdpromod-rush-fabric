package net.mdpromod;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.EntityHitResult;

import java.util.function.Supplier;

/** Fabric-side projection layer for the MDProMod duel host. */
public final class MDProMod implements ModInitializer {
    public static final String MODID = "mdpromod";

    public static final RegistryObject<Block> DUEL_ANCHOR = registerBlock(
            "duel_anchor", () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(2.0F)));
    public static final RegistryObject<Item> DUEL_ANCHOR_ITEM = registerItem(
            "duel_anchor", () -> new BlockItem(DUEL_ANCHOR.get(), new Item.Properties()));
    public static final RegistryObject<CreativeModeTab> DUEL_TAB = registerTab();

    private static final DuelSessionManager DUEL_SESSIONS = new DuelSessionManager();

    static DuelSessionManager sessions() {
        return DUEL_SESSIONS;
    }

    @Override
    public void onInitialize() {
        ModItems.register();
        ModEntities.register();
        ModNetwork.registerServer();

        ModPaths.useGameConfigDir(FabricLoader.getInstance().getConfigDir());
        ModPaths.useGameDir(FabricLoader.getInstance().getGameDir());
        ModBundle.start(FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT,
                ModPaths.dataRoot());
        ModPaths.applyBundledHost();
        ModPaths.seedDeckDir();
        CardRegistry.load();
        DuelFieldLayout.load();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                new DuelCommands(DUEL_SESSIONS).register(dispatcher));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> DUEL_SESSIONS.stop());
        ServerLifecycleEvents.SERVER_STARTED.register(RecipeSelfCheck::check);
        ServerTickEvents.END_SERVER_TICK.register(server -> DUEL_SESSIONS.tickField());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                syncMusicSettings(handler.player));
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                handleAttack(player, world, hand, entity, hitResult));
    }

    private static InteractionResult handleAttack(
            Player player, Level world, InteractionHand hand,
            Entity target, EntityHitResult hitResult) {
        if (world.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (target instanceof HandCardEntity card) {
            card.onLeftClick(serverPlayer);
            return InteractionResult.FAIL;
        }
        if (target instanceof DuelCardEntity card) {
            card.onLeftClick(serverPlayer);
            return InteractionResult.FAIL;
        }
        if (target instanceof DuelFieldEntity field) {
            field.onLeftClick(serverPlayer);
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    private static void syncMusicSettings(ServerPlayer player) {
        ItemStack stack = MusicSettingsItem.findSettings(player);
        if (stack == null) return;
        ModNetwork.sendTo(player, ModNetwork.MusicSettingsOpen.sync(
                MusicSettingsItem.snapshot(stack)));
    }

    private static RegistryObject<Block> registerBlock(String name, Supplier<Block> factory) {
        ResourceLocation id = new ResourceLocation(MODID, name);
        Block block = factory.get();
        net.minecraft.core.Registry.register(BuiltInRegistries.BLOCK, id, block);
        return new RegistryObject<>(block);
    }

    private static RegistryObject<Item> registerItem(String name, Supplier<Item> factory) {
        ResourceLocation id = new ResourceLocation(MODID, name);
        Item item = factory.get();
        net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, id, item);
        return new RegistryObject<>(item);
    }

    private static RegistryObject<CreativeModeTab> registerTab() {
        CreativeModeTab tab = FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.mdpromod.duel"))
                .icon(() -> DuelCardItem.createStack(89631139, 1))
                .displayItems((parameters, output) -> {
                    output.accept(DUEL_ANCHOR_ITEM.get());
                    output.accept(ModItems.DUEL_FIELD_ITEM.get());
                    output.accept(ModItems.CARD_CASE_ITEM.get());
                    output.accept(ModItems.MUSIC_SETTINGS_ITEM.get());
                    output.accept(ModItems.AI_DECK_ITEM.get());
                    output.accept(DuelCardItem.createStack(89631139, 1));
                })
                .build();
        net.minecraft.core.Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                new ResourceLocation(MODID, "duel"), tab);
        return new RegistryObject<>(tab);
    }
}
