package fr.openmc.core.features.events.contents.dailyevents.contents.miraculousfishing.contents.mobs;

import fr.openmc.core.OMCPlugin;
import fr.openmc.core.OMCRegistry;
import fr.openmc.core.registry.loottable.loots.ItemLoot;
import fr.openmc.core.registry.loottable.loots.XpLoot;
import fr.openmc.core.registry.mobs.CustomMob;
import fr.openmc.core.utils.RandomUtils;
import fr.openmc.core.utils.text.messages.TranslationManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Dolphin;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class Leviathan extends CustomMob<Dolphin> {
    public Leviathan(String id) {
        super(id,
                TranslationManager.translation("feature.dailyevents.miraculousfishing.mob.leviathan"),
                Dolphin.class,
                40,
                20,
                RandomUtils.randomBetween(0.3, 0.5),
                List.of(
                        new ItemLoot(OMCRegistry.CUSTOM_ITEMS.LEVIATHAN_HEAD,
                                0.50, 1),
                        new ItemLoot(OMCRegistry.CUSTOM_ITEMS.FISHING_FURNITURE_BOX, 0.20, 1),
                        new XpLoot(30, 60, 1)
                )
        );
    }

    @Override
    public Dolphin spawn(Location spawnLocation) {
        Dolphin dolphin = this.getPreBuildMob(spawnLocation);

        spawnPassager(dolphin);

        startDashAi(dolphin);

        return dolphin;
    }

    /**
     * Lance le scheduler qui fait dash le dauphin, sur le joueur
     * @param dolphin le dauphin ciblé
     */
    private void startDashAi(Dolphin dolphin) {
        Bukkit.getScheduler().runTaskTimer(OMCPlugin.getInstance(), task -> {
            if (dolphin.isDead()) {
                task.cancel();
                return;
            }

            Optional<Player> target = dolphin.getLocation().getNearbyPlayers(16).stream()
                    .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(dolphin.getLocation())));

            target.ifPresent(t -> triggerDash(dolphin, t));
        }, 20L, 60L);
    }

    /**
     * Dash vers la cible. 1.21.7 n'a pas le dash Nautilus, on utilise la vélocité.
     * @param dolphin le dauphin ciblé
     * @param target la target du dash, le joueur le plus proche
     */
    private void triggerDash(Dolphin dolphin, LivingEntity target) {
        org.bukkit.util.Vector direction = target.getLocation().toVector().subtract(dolphin.getLocation().toVector());
        if (direction.lengthSquared() < 1.0E-4) {
            return;
        }
        dolphin.setVelocity(direction.normalize().multiply(1.8));
    }

    /**
     * Spawn le passager du dauphin, un drowned pouvant varier
     * @param dolphin le dauphin ciblé
     */
    private void spawnPassager(Dolphin dolphin) {
        Drowned drowned = dolphin.getWorld().spawn(dolphin.getLocation(), Drowned.class);
        if (ThreadLocalRandom.current().nextFloat() < 0.1f)
            drowned.setBaby();
        drowned.setShouldBurnInDay(false);
        drowned.setAggressive(true);
        drowned.getEquipment().setItemInMainHand(getDrownedTrident());
        drowned.getEquipment().setHelmet(OMCRegistry.CUSTOM_ITEMS.LEVIATHAN_HEAD.getBest());
        drowned.getEquipment().setHelmetDropChance(0f);

        AttributeInstance maxHealth = drowned.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null)
            maxHealth.setBaseValue(this.getHealth());

        drowned.setHealth(this.getHealth());

        AttributeInstance attackSpeed = drowned.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed != null)
            attackSpeed.setBaseValue(6);

        dolphin.addPassenger(drowned);
    }

    /**
     * Donne un trident ayant des echantement aléatoire
     * @return l'item trident
     */
    private ItemStack getDrownedTrident() {
        ItemStack trident = new ItemStack(Material.TRIDENT);

        if (ThreadLocalRandom.current().nextBoolean())
            trident.addEnchantment(Enchantment.LOYALTY, 1);
        if (ThreadLocalRandom.current().nextBoolean())
            trident.addEnchantment(Enchantment.CHANNELING, 1);

        return trident;
    }
}
