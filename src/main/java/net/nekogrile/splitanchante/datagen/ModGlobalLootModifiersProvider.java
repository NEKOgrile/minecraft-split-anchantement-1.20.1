package net.nekogrile.splitanchante.datagen;

import net.nekogrile.splitanchante.SplitAnchante;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.GlobalLootModifierProvider;

public class ModGlobalLootModifiersProvider extends GlobalLootModifierProvider {
    public ModGlobalLootModifiersProvider(PackOutput output) {
        super(output, SplitAnchante.MOD_ID);
    }

    @Override
    protected void start() {
;



    }
}
