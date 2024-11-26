
package net.nekogrile.splitanchante.datagen.loot;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.nekogrile.splitanchante.block.ModBlocks;

import java.util.Set;


public class ModBlockLootTables extends BlockLootSubProvider {
    public ModBlockLootTables() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags()); // Utilise les feature flags actuels
    }

    @Override
    protected void generate() {
        // Configure ici les loot tables pour tes blocs

    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        // Retourne tous les blocs enregistrés pour lesquels tu veux des loot tables
        return ModBlocks.BLOCKS.getEntries().stream()
                .map(registryObject -> registryObject.get())
                .toList();
    }
}
