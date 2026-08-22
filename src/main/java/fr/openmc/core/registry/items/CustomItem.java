package fr.openmc.core.registry.items;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import fr.openmc.core.OMCRegistry;
import fr.openmc.core.hooks.itemsadder.ItemsAdderHook;
import fr.openmc.core.utils.bukkit.ItemUtils;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public abstract class CustomItem {
    @Getter
    private final String id;

    /**
     * -- GETTER --
     *  Méthode à override afin d'ajouter des metas personnalisées
     */
    @Getter
    private CustomItemMeta meta;

    public CustomItem(String id) {
        this.id = id;
        this.meta = null;
    }

    public CustomItem(CustomItemMeta meta) {
        this.meta = meta;
        this.id = meta.getId();
    }

    public abstract @NotNull ItemStack getVanilla();

    public @Nullable ItemStack getItemsAdder() {
        CustomStack stack = getCustomStack();
        return stack != null ? stack.getItemStack() : null;
    }

    /**
     * @return l'item du fournisseur ItemsAdder, ou null s'il ne connait pas cet id
     */
    public @Nullable CustomStack getCustomStack() {
        if (!ItemsAdderHook.isEnable()) return null;
        return CustomStack.getInstance(getId());
    }

    /**
     * @return le bloc du fournisseur ItemsAdder, ou null si cet id n'est pas un bloc connu
     */
    public @Nullable CustomBlock getCustomBlock() {
        if (!ItemsAdderHook.isEnable()) return null;
        return CustomBlock.getInstance(getId());
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ItemStack anotherItem) {
            Optional<CustomItem> citem = OMCRegistry.CUSTOM_ITEMS.get(anotherItem);

            if (citem.isEmpty()) return false;
            return citem.get().getId().equals(this.getId());
        }

        if (object instanceof String otherObjectName) {
            return this.getId().equals(otherObjectName);
        }

        if (object instanceof CustomItem citem) {
            return citem.getId().equals(this.getId());
        }

        return false;
    }

    /**
     * Order:
     * 1. ItemsAdder
     * 2. Vanilla
     *
     * @return Best ItemStack to use for the server
     */
    public ItemStack getBest() {
        ItemStack item;
        if (!ItemsAdderHook.isEnable() || getItemsAdder() == null) {
            item = getVanilla();
        } else {
            item = getItemsAdder();
        }

        ItemUtils.setTag(item, CustomItemRegistry.CUSTOM_ITEM_KEY, this.getId());

        return item;
    }
}