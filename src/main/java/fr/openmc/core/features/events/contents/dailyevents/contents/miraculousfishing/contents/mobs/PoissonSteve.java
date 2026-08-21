package fr.openmc.core.features.events.contents.dailyevents.contents.miraculousfishing.contents.mobs;

import fr.openmc.core.OMCRegistry;
import fr.openmc.core.registry.loottable.loots.ItemLoot;
import fr.openmc.core.registry.loottable.loots.XpLoot;
import fr.openmc.core.registry.mobs.CustomMob;
import fr.openmc.core.utils.RandomUtils;
import fr.openmc.core.utils.text.messages.TranslationManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class PoissonSteve extends CustomMob<ArmorStand> {
    public PoissonSteve(String id) {
        super(id,
                TranslationManager.translation("feature.dailyevents.miraculousfishing.mob.poisson_steve"),
                ArmorStand.class,
                50,
                67,
                RandomUtils.randomBetween(0.1, 0.1),
                List.of(
                        new ItemLoot(Material.TROPICAL_FISH,
                                1, 10, 20),
                        new ItemLoot(OMCRegistry.CUSTOM_ITEMS.POISSON_STEVE_HEAD,
                                0.5, 1),
                        new XpLoot(20, 35, 1)
                )
        );
    }

    @Override
    public ArmorStand spawn(Location spawnLocation) {
        ArmorStand mannequin = this.getPreBuildMob(spawnLocation);
        mannequin.setArms(true);
        mannequin.setBasePlate(false);
        mannequin.setCanPickupItems(false);
        mannequin.setDisabledSlots(
                EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
                EquipmentSlot.FEET, EquipmentSlot.LEGS,
                EquipmentSlot.CHEST, EquipmentSlot.HEAD
        );
        mannequin.getEquipment().setHelmet(OMCRegistry.CUSTOM_ITEMS.POISSON_STEVE_HEAD.getBest());

        return mannequin;
    }
}
