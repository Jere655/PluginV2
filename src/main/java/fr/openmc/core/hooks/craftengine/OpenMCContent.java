package fr.openmc.core.hooks.craftengine;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.CraftEngineFurniture;
import net.momirealms.craftengine.bukkit.api.CraftEngineImages;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.font.Image;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Public-CraftEngine boundary used by OpenMC gameplay code. */
public final class OpenMCContent {
    private OpenMCContent() { }

    public static @Nullable ItemStack createItem(String id) {
        var definition = CraftEngineItems.byId(id);
        return definition == null ? null : definition.buildBukkitItem();
    }

    public static @Nullable String itemId(ItemStack item) {
        Key key = CraftEngineItems.getCustomItemId(item);
        return key == null ? null : key.asString();
    }

    public static boolean isItem(ItemStack item, String id) {
        return id.equals(itemId(item));
    }

    public static @Nullable String blockId(Block block) {
        ImmutableBlockState state = CraftEngineBlocks.getCustomBlockState(block);
        if (state == null || state.isEmpty() || state.owner() == null || state.owner().value() == null) return null;
        return state.owner().value().id().asString();
    }

    public static boolean isBlock(Block block, String id) {
        return id.equals(blockId(block));
    }

    public static boolean placeBlock(Location location, String id) {
        return CraftEngineBlocks.place(location, Key.of(id), false);
    }

    public static boolean removeBlock(Block block) {
        return CraftEngineBlocks.remove(block);
    }

    public static @Nullable BukkitFurniture furniture(Entity entity) {
        return CraftEngineFurniture.getLoadedFurnitureByMetaEntity(entity);
    }

    public static @Nullable String furnitureId(BukkitFurniture furniture) {
        return furniture == null ? null : furniture.id().asString();
    }

    public static @Nullable String image(String id) {
        Image image = CraftEngineImages.byId(Key.of(id));
        return image == null ? null : image.miniMessageAt(0, 0);
    }
}
