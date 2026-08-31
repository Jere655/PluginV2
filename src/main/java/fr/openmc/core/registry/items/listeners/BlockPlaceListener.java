package fr.openmc.core.registry.items.listeners;

import net.momirealms.craftengine.bukkit.api.event.CustomBlockPlaceEvent;
import fr.openmc.core.OMCRegistry;
import fr.openmc.core.registry.items.CustomItem;
import fr.openmc.core.registry.items.options.LootboxBlock;
import fr.openmc.core.registry.items.options.UsableBlock;
import fr.openmc.core.utils.bukkit.ItemUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Optional;

public class BlockPlaceListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    void onCustomBlockPlace(CustomBlockPlaceEvent event) {
        Player player = event.player();

        Optional<CustomItem> item = OMCRegistry.CUSTOM_ITEMS.get(event.customBlock().id().asString());
        if (item.isEmpty()) return;
        if (item.get() instanceof UsableBlock usable) {
            usable.onCustomBlockPlace(player, event);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        Optional<CustomItem> item = OMCRegistry.CUSTOM_ITEMS.get(event.getItemInHand());
        if (item.isEmpty()) return;

        if (item.get() instanceof UsableBlock usable) {
            usable.onBlockPlace(player, event);
        }
    }

}
