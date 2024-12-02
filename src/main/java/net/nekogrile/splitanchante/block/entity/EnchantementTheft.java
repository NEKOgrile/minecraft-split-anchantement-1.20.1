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
    private LazyOptional<IItemHandler> hopperHandler = LazyOptional.empty();

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
            if (side == Direction.DOWN) {
                // Extraction uniquement pour le slot OUTPUT
                return hopperHandler.cast();
            } else if (side == Direction.NORTH || side == Direction.SOUTH || side == Direction.EAST || side == Direction.WEST) {
                // Hoppers sur les côtés interagissent avec le BOOK_SLOT
                return LazyOptional.of(() -> new ItemStackHandler(3) {
                    @Override
                    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                        if (slot == BOOK_SLOT && stack.is(Items.BOOK)) {
                            return itemHandler.insertItem(BOOK_SLOT, stack, simulate);
                        }
                        return stack; // Refuse tout autre item
                    }

                    @Override
                    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                        return ItemStack.EMPTY; // Empêche l'extraction par les hoppers sur les côtés
                    }

                    @Override
                    public int getSlots() {
                        return 3;
                    }

                    @Override
                    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                        return slot == BOOK_SLOT && stack.is(Items.BOOK); // Vérifie uniquement les livres normaux
                    }

                    @Override
                    public @NotNull ItemStack getStackInSlot(int slot) {
                        return itemHandler.getStackInSlot(slot);
                    }
                }).cast();
            }
            return lazyItemHandler.cast(); // Par défaut, retourne l'accès complet
        }
        return super.getCapability(cap, side);
    }


    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        hopperHandler = LazyOptional.of(() -> new ItemStackHandler(3) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return false; // Les hoppers ne doivent pas insérer d'items.
            }

            @Override
            @NotNull
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot == OUTPUT_SLOT) {
                    return itemHandler.extractItem(slot, amount, simulate);
                }
                if (slot == INPUT_SLOT) {
                    ItemStack inputStack = itemHandler.getStackInSlot(INPUT_SLOT);
                    if (inputStack.isEnchanted()) {
                        return ItemStack.EMPTY; // Empêche l'extraction si l'item est enchanté.
                    }
                    return itemHandler.extractItem(INPUT_SLOT, amount, simulate);
                }
                return ItemStack.EMPTY; // Bloque l'extraction pour les autres slots.
            }

            @Override
            public int getSlots() {
                return 3;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                if (slot == OUTPUT_SLOT) {
                    return itemHandler.getStackInSlot(slot);
                }
                if (slot == INPUT_SLOT) {
                    return itemHandler.getStackInSlot(INPUT_SLOT);
                }
                return ItemStack.EMPTY;
            }
        });
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        hopperHandler.invalidate();
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

        if (inputStack.isEnchanted() && bookStack.getCount() > 0) {
            var enchantments = inputStack.getEnchantmentTags();
            if (!enchantments.isEmpty()) {
                ItemStack newEnchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
                newEnchantedBook.addTagElement("StoredEnchantments", enchantments.copy());
                if (outputStack.isEmpty()) {
                    this.itemHandler.setStackInSlot(OUTPUT_SLOT, newEnchantedBook);
                }
                inputStack.removeTagKey("Enchantments");
                this.itemHandler.setStackInSlot(INPUT_SLOT, inputStack);
                bookStack.shrink(1);
                this.itemHandler.setStackInSlot(BOOK_SLOT, bookStack);
            }
        }
    }

    private boolean hasRecipe() {
        ItemStack inputStack = this.itemHandler.getStackInSlot(INPUT_SLOT);
        boolean hasItemWithEnchantments = inputStack.isEnchanted();
        boolean hasEnoughBooks = this.itemHandler.getStackInSlot(BOOK_SLOT).getCount() >= 1;
        boolean isStandardBook = this.itemHandler.getStackInSlot(BOOK_SLOT).getItem() == Items.BOOK;

        ItemStack result = new ItemStack(Items.ENCHANTED_BOOK);

        return hasItemWithEnchantments && hasEnoughBooks && isStandardBook &&
                canInsertAmountIntoOutputSlot(result.getCount()) &&
                canInsertItemIntoOutputSlot(result.getItem().getDefaultInstance());
    }

    private boolean canInsertItemIntoOutputSlot(ItemStack item) {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty() || this.itemHandler.getStackInSlot(OUTPUT_SLOT).is(item.getItem());
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
