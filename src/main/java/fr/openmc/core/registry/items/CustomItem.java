package fr.openmc.core.registry.items;

import fr.openmc.core.OMCRegistry;
import fr.openmc.core.hooks.craftengine.OpenMCContent;
import dev.lone.itemsadder.api.CustomBlock;
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

    /**
     * Transitional access for systems that still need an external custom-block
     * implementation. New item creation never depends on this API.
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
     * Builds the native OpenMC representation.  CraftEngine's generated
     * resource pack resolves the canonical item-model key while the PDC keeps
     * server-side identity independent of any content plugin.
     */
    public ItemStack getBest() {
        ItemStack item = OpenMCContent.createItem(getId());
        if (item == null) item = getVanilla();

        ItemUtils.setTag(item, CustomItemRegistry.CUSTOM_ITEM_KEY, this.getId());
        ItemUtils.setItemModel(item, this.getId());

        return item;
    }
}
