package net.nekogrile.splitanchante.item;

import net.nekogrile.splitanchante.SplitAnchante;
import net.nekogrile.splitanchante.block.ModBlocks;
import net.nekogrile.splitanchante.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SplitAnchante.MOD_ID);

    public static final RegistryObject<CreativeModeTab> TUTORIAL_TAB = CREATIVE_MODE_TABS.register("tutorial_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModBlocks.GEM_POLISHING_STATION.get()))
                    .title(Component.translatable("creativetab.tutorial_tab"))
                    .displayItems((parameters, output) -> {
                        // Ajouter ici tous les items ou blocs que tu veux afficher dans l'onglet
                        output.accept(ModBlocks.GEM_POLISHING_STATION.get().asItem());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
