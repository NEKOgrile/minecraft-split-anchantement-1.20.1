package net.nekogrile.splitanchante.block.entity;

import net.nekogrile.splitanchante.SplitAnchante;
import net.nekogrile.splitanchante.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SplitAnchante.MOD_ID);

    public static final RegistryObject<BlockEntityType<GemPolishingStationBlockEntity>> GEM_POLISHING_BE =
            BLOCK_ENTITIES.register("gem_polishing_be", () ->
                    BlockEntityType.Builder.of(GemPolishingStationBlockEntity::new,
                            ModBlocks.GEM_POLISHING_STATION.get()).build(null));


    public static final RegistryObject<BlockEntityType<EnchantementTheft>> ENCHANTEMED_BE =
            BLOCK_ENTITIES.register("anchnetement_theft_be", () ->
                    BlockEntityType.Builder.of(EnchantementTheft::new,
                            ModBlocks.ENCHANTEMENT_THEFT.get()).build(null));

    public static final RegistryObject<BlockEntityType<TheBibiliothequeEntity>> BIBILIOTHEQUE_BE =
            BLOCK_ENTITIES.register("bibiliotheque", () ->
                    BlockEntityType.Builder.of(TheBibiliothequeEntity::new,
                            ModBlocks.BIBILIOTHE.get()).build(null));


    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
