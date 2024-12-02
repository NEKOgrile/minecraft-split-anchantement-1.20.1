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
    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // Contrôle des slots
            return switch (slot) {
                case 0 -> stack.is(Items.ENCHANTED_BOOK); // INPUT : accepte uniquement des livres enchantés
                case 1 -> false; // OUTPUT : aucun item ne peut être placé
                case 2 -> stack.is(Items.BOOK); // BOOK : accepte uniquement des livres normaux
                default -> false;
            };
        }
    };

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int BOOK_SLOT = 2;

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IItemHandler> hopperHandler = LazyOptional.empty();

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
                if (slot == INPUT_SLOT) {
                    ItemStack inputStack = itemHandler.getStackInSlot(INPUT_SLOT);
                    if (isSingleEnchantedBook(inputStack)) {
                        return itemHandler.extractItem(INPUT_SLOT, amount, simulate);
                    }
                    return ItemStack.EMPTY; // Bloque l'extraction si la condition n'est pas remplie
                }
                if (slot == OUTPUT_SLOT) {
                    return itemHandler.extractItem(slot, amount, simulate);
                }
                return ItemStack.EMPTY; // Bloque l'extraction pour les autres slots
            }

            @Override
            public int getSlots() {
                return 3;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return itemHandler.getStackInSlot(slot);
            }
        });
    }

    private boolean isSingleEnchantedBook(ItemStack stack) {
        if (!stack.is(Items.ENCHANTED_BOOK) || stack.getTag() == null) {
            return false; // Pas un livre enchanté ou pas de NBT
        }
        var enchantments = stack.getTag().getList("StoredEnchantments", 10);
        return enchantments.size() == 1; // Vérifie qu'il y a exactement un enchantement
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

        if (inputStack.is(Items.ENCHANTED_BOOK) && inputStack.hasTag() && bookStack.getCount() > 0) {
            // Récupère la liste des enchantements du livre d'entrée
            var enchantments = inputStack.getTag().getList("StoredEnchantments", 10);

            if (!enchantments.isEmpty()) {
                // Récupère le premier enchantement
                var firstEnchantment = enchantments.getCompound(0);

                // Crée un nouveau livre enchanté avec cet enchantement
                ItemStack newEnchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
                var newEnchantmentList = new net.minecraft.nbt.ListTag();
                newEnchantmentList.add(firstEnchantment);
                newEnchantedBook.addTagElement("StoredEnchantments", newEnchantmentList);

                // Place le nouveau livre enchanté dans le slot de sortie
                if (outputStack.isEmpty()) {
                    this.itemHandler.setStackInSlot(OUTPUT_SLOT, newEnchantedBook);
                } else if (outputStack.is(Items.ENCHANTED_BOOK)) {
                    // Combine avec le livre existant dans l'output (si déjà présent)
                    var existingEnchantments = outputStack.getTag().getList("StoredEnchantments", 10);
                    existingEnchantments.add(firstEnchantment);
                    outputStack.getTag().put("StoredEnchantments", existingEnchantments);
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

                // Consomme un livre normal dans le slot de livres
                bookStack.shrink(1);
                this.itemHandler.setStackInSlot(BOOK_SLOT, bookStack);
            }
        }
    }


    private boolean hasRecipe() {
        ItemStack inputStack = this.itemHandler.getStackInSlot(INPUT_SLOT);
        ItemStack bookStack = this.itemHandler.getStackInSlot(BOOK_SLOT);
        ItemStack outputStack = this.itemHandler.getStackInSlot(OUTPUT_SLOT);

        boolean hasEnchantedBookWithEnchantments = inputStack.is(Items.ENCHANTED_BOOK) &&
                inputStack.hasTag() &&
                inputStack.getTag().getList("StoredEnchantments", 10).size() > 0;

        boolean hasBooks = bookStack.is(Items.BOOK) && bookStack.getCount() > 0;
        boolean outputSlotEmptyOrMatching = outputStack.isEmpty() || outputStack.is(Items.ENCHANTED_BOOK);

        return hasEnchantedBookWithEnchantments && hasBooks && outputSlotEmptyOrMatching;
    }

    private boolean hasProgressFinished() {
        return progress >= maxProgress;
    }

    private void increaseCraftingProgress() {
        progress++;
    }
}
