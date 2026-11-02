package net.mdpromod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public final class ModItems {
    public static final RegistryObject<Item> DUEL_FIELD_ITEM =
            register("duel_field", () -> new DuelFieldItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CARD_CASE_ITEM =
            register("card_case", () -> new CardCaseItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DUEL_CARD_ITEM =
            register("duel_card", () -> new DuelCardItem(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> MUSIC_SETTINGS_ITEM =
            register("music_settings", () -> new MusicSettingsItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> AI_DECK_ITEM =
            register("ai_deck", () -> new AiDeckItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    static void register() {
        // Registration happens in the field initializers when this class is initialized.
    }

    private static RegistryObject<Item> register(String name, Supplier<Item> factory) {
        ResourceLocation id = new ResourceLocation(MDProMod.MODID, name);
        Item item = factory.get();
        net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, id, item);
        return new RegistryObject<>(item);
    }
}
