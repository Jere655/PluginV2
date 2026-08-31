package fr.openmc.core.hooks.itemsadder.behaviours;

import fr.openmc.core.hooks.craftengine.OpenMCContent;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockBreakEvent;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockPlaceEvent;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;

/**
 * Native CraftEngine implementation of the former ItemsAdder {@code up_block}
 * behavior used by obese crops. The source content is converted at bootstrap,
 * so this gameplay rule is intentionally owned by OpenMC rather than parsed
 * from an ItemsAdder runtime directory.
 */
public final class BehaviourUpBlock implements Listener {
    private static final Map<String, String> UP_BLOCKS = Map.of(
            "omc_daily_events:obese_potato", "omc_daily_events:obese_potato_stem",
            "omc_daily_events:obese_poisonous_potato", "omc_daily_events:obese_potato_stem",
            "omc_daily_events:obese_carrot", "omc_daily_events:obese_carrot_stem",
            "omc_daily_events:obese_beetroot", "omc_daily_events:obese_beetroot_stem",
            "omc_daily_events:obese_nether_wart", "omc_daily_events:obese_nether_wart_stem",
            "omc_daily_events:obese_golden_apple", "omc_daily_events:obese_golden_apple_stem"
    );

    public static void onPlace(Block block, String namespacedId) {
        String upId = UP_BLOCKS.get(namespacedId);
        if (upId == null) return;

        Block upBlock = block.getRelative(BlockFace.UP);
        if (upBlock.getType().isAir()) OpenMCContent.placeBlock(upBlock.getLocation(), upId);
    }

    @EventHandler
    public void onBelowBlockPlaced(CustomBlockPlaceEvent event) {
        onPlace(event.bukkitBlock(), event.customBlock().id().asString());
    }

    @EventHandler
    public void onBelowBlockBreak(CustomBlockBreakEvent event) {
        onBreak(event.bukkitBlock(), event.customBlock().id().asString());
    }

    public static void onBreak(Block block, String namespacedId) {
        String upId = UP_BLOCKS.get(namespacedId);
        if (upId == null) return;

        Block upBlock = block.getRelative(BlockFace.UP);
        if (OpenMCContent.isBlock(upBlock, upId)) OpenMCContent.removeBlock(upBlock);
    }
}
