package fr.openmc.core.registry.items.options;

import net.momirealms.craftengine.bukkit.api.event.CustomBlockPlaceEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

public interface UsableBlock {
    default void onCustomBlockPlace(Player player, CustomBlockPlaceEvent event) {}
    default void onBlockPlace(Player player, BlockPlaceEvent event) {}
}
