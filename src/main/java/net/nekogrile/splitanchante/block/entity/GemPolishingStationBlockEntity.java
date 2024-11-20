package net.nekogrile.splitanchante.block.entity;

import net.nekogrile.splitanchante.screen.GemPolishingStationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GemPolishingStationBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(3); // Trois slots

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int BOOK_SLOT = 2;

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 78;

    public GemPolishingStationBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.GEM_POLISHING_BE.get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> GemPolishingStationBlockEntity.this.progress;
                    case 1 -> GemPolishingStationBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> GemPolishingStationBlockEntity.this.progress = pValue;
                    case 1 -> GemPolishingStationBlockEntity.this.maxProgress = pValue;
                }
            }

            @Override
            public int getCount() {
                return 3; // Nombre de slots : 3 (input, output, book)
            }
        };
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler); // Initialisation du lazyItemHandler
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.splitanchante.gem_polishing_station");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new GemPolishingStationMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.putInt("gem_polishing_station.progress", progress);
        super.saveAdditional(pTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        progress = pTag.getInt("gem_polishing_station.progress");
    }

    public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
        if (hasRecipe()) {
            increaseCraftingProgress();
            setChanged(pLevel, pPos, pState);

            if (hasProgressFinished()) {
                craftItem();
                resetProgress();
            }
        } else {
            resetProgress();
        }
    }

    private void resetProgress() {
        progress = 0;
    }

    private void craftItem() {
        ItemStack inputStack = this.itemHandler.getStackInSlot(INPUT_SLOT);
        ItemStack outputStack = this.itemHandler.getStackInSlot(OUTPUT_SLOT);
        ItemStack bookStack = this.itemHandler.getStackInSlot(BOOK_SLOT);

        if (inputStack.is(Items.ENCHANTED_BOOK) && inputStack.getTag() != null && bookStack.getCount() > 0) {
            // Récupère la liste des enchantements
            var enchantments = inputStack.getTag().getList("StoredEnchantments", 10);

            if (!enchantments.isEmpty()) {
                // Extrait le premier enchantement
                var firstEnchantment = enchantments.getCompound(0);

                // Crée un livre enchanté avec cet enchantement
                ItemStack newEnchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
                newEnchantedBook.addTagElement("StoredEnchantments", new net.minecraft.nbt.ListTag());
                newEnchantedBook.getTag().getList("StoredEnchantments", 10).add(firstEnchantment);

                // Place le livre enchanté dans le slot de sortie s'il est vide
                if (outputStack.isEmpty()) {
                    this.itemHandler.setStackInSlot(OUTPUT_SLOT, newEnchantedBook);
                } else {
                    outputStack.getTag().getList("StoredEnchantments", 10).add(firstEnchantment);
                    this.itemHandler.setStackInSlot(OUTPUT_SLOT, outputStack);
                }

                // Supprime le premier enchantement du livre d'entrée
                enchantments.remove(0);
                if (enchantments.isEmpty()) {
                    // Si plus d'enchantements, retire le livre
                    this.itemHandler.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY);
                } else {
                    // Met à jour le livre d'entrée avec les enchantements restants
                    inputStack.getTag().put("StoredEnchantments", enchantments);
                    this.itemHandler.setStackInSlot(INPUT_SLOT, inputStack);
                }

                // Consomme un livre normal du slot de livres (BOOK_SLOT)
                bookStack.shrink(1);
                this.itemHandler.setStackInSlot(BOOK_SLOT, bookStack);
            }
        }
    }


    private boolean hasRecipe() {
        ItemStack inputStack = this.itemHandler.getStackInSlot(INPUT_SLOT);
        boolean hasEnchantedBookWithEnchantments = inputStack.is(Items.ENCHANTED_BOOK) &&
                inputStack.getTag() != null &&
                inputStack.getTag().getList("StoredEnchantments", 10).size() > 0; // Vérifie qu'il y a au moins un enchantement

        boolean hasEnoughBooks = this.itemHandler.getStackInSlot(BOOK_SLOT).getCount() >= 2;
        boolean isStandardBook = this.itemHandler.getStackInSlot(BOOK_SLOT).getItem() == Items.BOOK; // Vérifie si c'est un livre simple

        ItemStack result = new ItemStack(Items.ENCHANTED_BOOK);

        return hasEnchantedBookWithEnchantments && hasEnoughBooks && isStandardBook &&
                canInsertAmountIntoOutputSlot(result.getCount()) &&
                canInsertItemIntoOutputSlot(result.getItem());
    }


    private boolean canInsertItemIntoOutputSlot(Item item) {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty() || this.itemHandler.getStackInSlot(OUTPUT_SLOT).is(item);
    }

    private boolean canInsertAmountIntoOutputSlot(int count) {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() + count <= this.itemHandler.getStackInSlot(OUTPUT_SLOT).getMaxStackSize();
    }

    private boolean hasProgressFinished() {
        return progress >= maxProgress;
    }

    private void increaseCraftingProgress() {
        progress++;
    }
}
