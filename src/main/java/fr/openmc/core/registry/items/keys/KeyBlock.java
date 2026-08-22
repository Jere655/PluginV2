package fr.openmc.core.registry.items.keys;

import dev.lone.itemsadder.api.CustomBlock;
import fr.openmc.core.OMCRegistry;
import fr.openmc.core.hooks.itemsadder.ItemsAdderHook;
import fr.openmc.core.registry.items.CustomItem;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.block.BlockType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class KeyBlock {
    private static final Set<String> CUSTOM_IDS = ConcurrentHashMap.newKeySet();
    @Getter
    private final BlockType blockType;
    @Getter
    private final String namespacedID;
    private final String id;

    private KeyBlock(BlockType blockType, String namespacedID, String id) {
        this.blockType = blockType;
        this.namespacedID = namespacedID;
        this.id = id;
    }

    public static KeyBlock vanilla(BlockType type) {
        return new KeyBlock(type, null, "vanilla:" + type.getKey());
    }

    public static KeyBlock custom(CustomBlock block) {
        return custom(block.getNamespacedID());
    }

    public static KeyBlock custom(CustomItem item) {
        return custom(item.getId());
    }

    public static KeyBlock custom(String namespacedID) {
        CUSTOM_IDS.add(namespacedID);
        return new KeyBlock(null, namespacedID, "custom:" + namespacedID);
    }

    /**
     * @return les identifiants custom attendus comme blocs par les features déjà chargées
     */
    public static Set<String> getKnownCustomIDs() {
        return Collections.unmodifiableSet(CUSTOM_IDS);
    }

    public static KeyBlock fromBlock(Block block) {
        CustomBlock customBlock = getPlacedCustomBlock(block);
        if (customBlock != null) {
            return custom(customBlock);
        }
        return vanilla(block.getType().asBlockType());
    }

    private static @Nullable CustomBlock getPlacedCustomBlock(Block block) {
        if (!ItemsAdderHook.isEnable()) return null;
        return CustomBlock.byAlreadyPlaced(block);
    }

    /**
     * Le bloc custom n'est résolu que si le fournisseur ItemsAdder le connait,
     * il peut donc être absent selon le contenu chargé sur le serveur.
     */
    public @Nullable CustomBlock getCustomBlock() {
        if (isVanilla() || !ItemsAdderHook.isEnable()) return null;
        return CustomBlock.getInstance(namespacedID);
    }

    public @Nullable CustomItem getCustomItem() {
        if (isVanilla() || OMCRegistry.CUSTOM_ITEMS == null) return null;
        return OMCRegistry.CUSTOM_ITEMS.get(namespacedID).orElse(null);
    }

    public boolean isVanilla() {
        return blockType != null;
    }

    public boolean isCustom() {
        return namespacedID != null;
    }

    public Component name() {
        if (isVanilla()) return Component.translatable(blockType.translationKey());

        CustomItem item = getCustomItem();
        if (item == null) return Component.text(namespacedID);

        ItemStack itemStack = item.getBest();
        if (itemStack.getItemMeta().hasItemName()) {
            return itemStack.getItemMeta().itemName();
        }
        return itemStack.displayName();
    }

    public boolean matches(Block block) {
        if (block == null) return false;

        if (isCustom()) {
            CustomBlock placed = getPlacedCustomBlock(block);
            return placed != null && namespacedID.equals(placed.getNamespacedID());
        }

        return blockType == block.getType().asBlockType();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof KeyBlock other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "KeyBlock[" + id + "]";
    }
}
