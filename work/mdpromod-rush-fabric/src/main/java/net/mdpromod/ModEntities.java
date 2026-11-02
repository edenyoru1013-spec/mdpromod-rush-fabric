package net.mdpromod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final RegistryObject<EntityType<DuelFieldEntity>> DUEL_FIELD =
            register("duel_field", EntityType.Builder
                    .<DuelFieldEntity>of(DuelFieldEntity::new, MobCategory.MISC)
                    .sized(16.0F, 0.4F)
                    .clientTrackingRange(32)
                    .updateInterval(20));

    public static final RegistryObject<EntityType<CardCaseEntity>> CARD_CASE =
            register("card_case", EntityType.Builder
                    .<CardCaseEntity>of(CardCaseEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.8F)
                    .clientTrackingRange(16)
                    .updateInterval(20));

    public static final RegistryObject<EntityType<DuelCardEntity>> DUEL_CARD =
            register("duel_card", EntityType.Builder
                    .<DuelCardEntity>of(DuelCardEntity::new, MobCategory.MISC)
                    .sized(1.0F, 0.1F)
                    .clientTrackingRange(16)
                    .updateInterval(20));

    public static final RegistryObject<EntityType<HandCardEntity>> HAND_CARD =
            register("hand_card", EntityType.Builder
                    .<HandCardEntity>of(HandCardEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.85F)
                    .clientTrackingRange(16)
                    .updateInterval(10));

    private ModEntities() {
    }

    static void register() {
        // Registration happens in the field initializers when this class is initialized.
    }

    private static <T extends net.minecraft.world.entity.Entity> RegistryObject<EntityType<T>> register(
            String name, EntityType.Builder<T> builder) {
        ResourceLocation id = new ResourceLocation(MDProMod.MODID, name);
        EntityType<T> type = builder.build(id.toString());
        net.minecraft.core.Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type);
        return new RegistryObject<>(type);
    }
}
