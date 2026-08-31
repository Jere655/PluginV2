package fr.openmc.core.features.events.contents.dailyevents.contents.goldenharvest.listeners;

import net.momirealms.craftengine.bukkit.api.event.CustomBlockBreakEvent;
import fr.openmc.core.OMCPlugin;
import fr.openmc.core.hooks.craftengine.OpenMCContent;
import fr.openmc.core.features.events.contents.dailyevents.DailyEventsManager;
import fr.openmc.core.features.events.contents.dailyevents.contents.goldenharvest.AbondanceArmorManager;
import fr.openmc.core.features.events.contents.dailyevents.contents.goldenharvest.GoldenHarvestEvent;
import fr.openmc.core.features.events.contents.dailyevents.contents.goldenharvest.GoldenHarvestManager;
import fr.openmc.core.features.events.contents.dailyevents.contents.goldenharvest.obesecrops.ObeseCropsRegistry;
import fr.openmc.core.registry.items.keys.KeyBlock;
import fr.openmc.core.registry.loottable.loots.CustomLoot;
import fr.openmc.core.registry.loottable.loots.ItemLoot;
import fr.openmc.core.utils.bukkit.ParticleUtils;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static fr.openmc.core.features.events.contents.dailyevents.contents.goldenharvest.AbondanceArmorManager.applyDoubleCropsChance;

/**
 * Listener qui prends en charge les loots donnée par les crops et par les obese crops
 */
public class GoldenCropsListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onCropBreak(BlockBreakEvent event) {
        if (!DailyEventsManager.isActiveDailyEvent()
                || !(DailyEventsManager.getActiveDailyEvent() instanceof GoldenHarvestEvent)) return;
        if (ThreadLocalRandom.current().nextDouble() > AbondanceArmorManager.getLuckGoldenCropsModifier(event.getPlayer())) return;

        BlockType blockType = event.getBlock().getType().asBlockType();
        KeyBlock keyBlock = KeyBlock.vanilla(blockType);
        ItemLoot itemLoot = GoldenHarvestManager.getGoldenCropsOnBreakMapping().get(keyBlock);
        if (itemLoot == null) return;

        List<CustomLoot> loots = itemLoot.run(event.getPlayer(), event.getBlock().getLocation()).loots();
        if (loots.isEmpty()) return;

        giveRewards(itemLoot, event.getPlayer(), event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onObeseCropBreak(CustomBlockBreakEvent event) {
        if (!DailyEventsManager.isActiveDailyEvent()
                || !(DailyEventsManager.getActiveDailyEvent() instanceof GoldenHarvestEvent)) return;
        if (ThreadLocalRandom.current().nextDouble() > GoldenHarvestManager.GOLDEN_CROP_ON_OBESE_CHANCE) return;
        if (!ObeseCropsRegistry.isObeseCrop(event.bukkitBlock().getLocation())) return;

        KeyBlock keyBlock = KeyBlock.custom(event.customBlock().id().asString());

        ItemLoot itemLoot = GoldenHarvestManager.getGoldenCropsOnBreakMapping().get(keyBlock);
        if (itemLoot == null) return;

        giveRewards(itemLoot, event.getPlayer(), event.bukkitBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onCropFullyGrowed(BlockGrowEvent event) {
        if (!DailyEventsManager.isActiveDailyEvent()
                || !(DailyEventsManager.getActiveDailyEvent() instanceof GoldenHarvestEvent)) return;
        BlockType blockType = event.getNewState().getType().asBlockType();
        KeyBlock keyBlock = KeyBlock.vanilla(blockType);
        KeyBlock keyBlockGolden = GoldenHarvestManager.getGoldenCropsOnGrowMapping().get(keyBlock);
        if (keyBlockGolden == null) return;

        if (ThreadLocalRandom.current().nextDouble() > GoldenHarvestManager.GOLDEN_CROP_ON_CROP_CHANCE) return;

        Bukkit.getScheduler().runTaskLater(OMCPlugin.getInstance(), () ->
                OpenMCContent.placeBlock(event.getBlock().getLocation(), keyBlockGolden.getNamespacedID()), 1L);

        ParticleUtils.spawnDispersingParticles(
                event.getBlock().getLocation().add(0.5, 0.5, 0.5),
                Particle.POOF,
                20,
                40,
                0.3,
                null);
        event.getBlock().getWorld().playSound(event.getBlock().getLocation(), Sound.ENTITY_CREAKING_SPAWN, 1, 0.3f);
    }

    private void giveRewards(ItemLoot itemLoot, Player player, Block block) {
        Collection<CustomLoot> loots = applyDoubleCropsChance(player, itemLoot.run(player, block.getLocation()).loots());
        if (loots.isEmpty()) return;

        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1, 0.3f);
        ParticleUtils.spawnDispersingParticles(
                block.getLocation().add(0.5, 0.5, 0.5),
                Particle.DRIPPING_HONEY,
                10,
                40,
                0.3,
                null);
    }
}
