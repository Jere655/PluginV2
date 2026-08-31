package fr.openmc.core.features.animations;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitTask;

@Getter
@Setter
public class PlayerAnimationInfo {

    private BukkitTask task;
    private ArmorStand armorStand;
    private Float[] oldRotations;
    private Animation animation;
    private float oldWalkSpeed;
    private float oldFlySpeed;
    private boolean oldInvulnerable;
}
