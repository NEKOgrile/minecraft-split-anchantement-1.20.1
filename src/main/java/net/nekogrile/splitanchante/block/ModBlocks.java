package net.nekogrile.splitanchante.block;

import net.nekogrile.splitanchante.SplitAnchante;
import net.nekogrile.splitanchante.block.custom.EnchantementTheft;
import net.nekogrile.splitanchante.block.custom.GemPolishingStationBlock;
import net.nekogrile.splitanchante.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, SplitAnchante.MOD_ID);


    public static final RegistryObject<Block> GEM_POLISHING_STATION = registerBlock("gem_polishing_station",
            () -> new GemPolishingStationBlock(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD).lightLevel(state -> 10).strength(2.0f, 6.0f)));


    public static final RegistryObject<Block> ENCHANTEMENT_THEFT = registerBlock("anchnetement_theft",
            () -> new EnchantementTheft(BlockBehaviour.Properties.copy(Blocks.OAK_WOOD) .lightLevel(state -> 10).strength(2.0f, 6.0f)));



    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
