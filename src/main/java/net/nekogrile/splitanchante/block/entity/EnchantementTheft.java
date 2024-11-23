package net.nekogrile.splitanchante.block.entity;

import net.nekogrile.splitanchante.screen.EnchantementTheftMenu;
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

public class EnchantementTheft extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(3); // Trois slots

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int BOOK_SLOT = 2;

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 78;

    public EnchantementTheft(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.ENCHANTEMED_BE.get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> EnchantementTheft.this.progress;
                    case 1 -> EnchantementTheft.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> EnchantementTheft.this.progress = pValue;
                    case 1 -> EnchantementTheft.this.maxProgress = pValue;
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
        return Component.translatable("block.splitanchante.anchnetement_theft");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new EnchantementTheftMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.putInt("anchnetement_theft.progress", progress);
        super.saveAdditional(pTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        progress = pTag.getInt("anchnetement_theft.progress");
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

        // Vérifie si l'item d'entrée est une armure ou un autre objet avec des enchantements
        if (inputStack.isEnchanted() && bookStack.getCount() > 0) {
            // Récupère tous les enchantements de l'armure
            var enchantments = inputStack.getEnchantmentTags();

            if (!enchantments.isEmpty()) {
                // Crée un livre enchanté contenant tous les enchantements
                ItemStack newEnchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
                newEnchantedBook.addTagElement("StoredEnchantments", enchantments.copy());

                // Place le livre enchanté dans le slot de sortie
                if (outputStack.isEmpty()) {
                    this.itemHandler.setStackInSlot(OUTPUT_SLOT, newEnchantedBook);
                } else {
                    // Si un livre existe déjà dans la sortie, fusionne les enchantements
                    var outputEnchantments = outputStack.getOrCreateTag().getList("StoredEnchantments", 10);
                    for (int i = 0; i < enchantments.size(); i++) {
                        outputEnchantments.add(enchantments.get(i));
                    }
                    outputStack.getTag().put("StoredEnchantments", outputEnchantments);
                    this.itemHandler.setStackInSlot(OUTPUT_SLOT, outputStack);
                }

                // Supprime tous les enchantements de l'objet d'entrée
                inputStack.removeTagKey("Enchantments");
                this.itemHandler.setStackInSlot(INPUT_SLOT, inputStack);

                // Consomme un livre normal du slot de livres
                bookStack.shrink(1);
                this.itemHandler.setStackInSlot(BOOK_SLOT, bookStack);
            }
        }
    }



    private boolean hasRecipe() {
        ItemStack inputStack = this.itemHandler.getStackInSlot(INPUT_SLOT);
        boolean hasItemWithEnchantments = inputStack.isEnchanted(); // Vérifie si l'item a des enchantements

        boolean hasEnoughBooks = this.itemHandler.getStackInSlot(BOOK_SLOT).getCount() >= 1; // Besoin d'un livre
        boolean isStandardBook = this.itemHandler.getStackInSlot(BOOK_SLOT).getItem() == Items.BOOK; // Vérifie si c'est un livre simple

        ItemStack result = new ItemStack(Items.ENCHANTED_BOOK);

        return hasItemWithEnchantments && hasEnoughBooks && isStandardBook &&
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
