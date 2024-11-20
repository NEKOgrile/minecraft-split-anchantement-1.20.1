package net.nekogrile.splitanchante.item;

import net.nekogrile.splitanchante.SplitAnchante;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SplitAnchante.MOD_ID);



       public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
