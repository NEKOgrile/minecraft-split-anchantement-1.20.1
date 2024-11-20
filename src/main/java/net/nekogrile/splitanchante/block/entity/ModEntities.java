package net.nekogrile.splitanchante.block.entity;

import net.nekogrile.splitanchante.SplitAnchante;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SplitAnchante.MOD_ID);



    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
