package net.nekogrile.splitanchante.datagen;

import net.nekogrile.splitanchante.SplitAnchante;
import net.nekogrile.splitanchante.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.*;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, SplitAnchante.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {


        simpleBlockWithItem(ModBlocks.GEM_POLISHING_STATION.get(),
                new ModelFile.UncheckedModelFile(modLoc("block/gem_polishing_station")));

        simpleBlockWithItem(ModBlocks.ENCHANTEMENT_THEFT.get(),
                new ModelFile.UncheckedModelFile(modLoc("block/gem_polishing_station")));
    }



    private void blockWithItem(RegistryObject<Block> blockRegistryObject) {
        simpleBlockWithItem(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));
    }
}
