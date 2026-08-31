package fr.openmc.core.features.animations.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import fr.openmc.core.OMCPlugin;
import fr.openmc.core.bootstrap.features.types.NotLoadInUnitTest;
import fr.openmc.core.features.animations.Animation;
import fr.openmc.core.features.animations.PlayerAnimationInfo;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.logging.Level;

public class EmoteListener implements Listener, NotLoadInUnitTest {
    public static final HashMap<Player, PlayerAnimationInfo> playingAnimations = new HashMap<>();

    public static void play(Player player, Animation animation) {
        stop(player);
        Location base = player.getLocation();

        if (animation.getSoundName() != null) {
            player.playSound(player, animation.getSoundName(), 1.0f, 1.0f);
        }

        PlayerAnimationInfo info = new PlayerAnimationInfo();
        info.setAnimation(animation);
        info.setOldInvulnerable(player.isInvulnerable());
        playingAnimations.put(player, info);

        EmoteListener.setupHead(player);

        ArmorStand stand = player.getWorld().spawn(base, ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setGravity(false);
            as.setMarker(true);
        });
        info.setArmorStand(stand);

        if (animation == Animation.JOIN_RIFT) {
            info.setOldWalkSpeed(player.getWalkSpeed());
            info.setOldFlySpeed(player.getFlySpeed());
            player.setWalkSpeed(0f);
            player.setFlySpeed(0f);
            player.setInvulnerable(true);
        }

        BukkitTask task = new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick > animation.getTotalTicks()) {
                    stop(player);
                    return;
                }

                if (animation == Animation.JOIN_RIFT && !player.isOnGround()) {
                    stop(player);
                    return;
                }

                if (animation.getCameraPositions().containsKey(tick) && animation.getCameraViews().containsKey(tick)) {
                    Vector pos = animation.getCameraPositions().get(tick);
                    Vector view = animation.getCameraViews().get(tick);

                    Location camLoc = base.clone().add(pos);

                    Vector lookTarget = camLoc.toVector().add(new Vector(view.getX(), 0, view.getZ()));

                    Vector diff = lookTarget.clone().subtract(camLoc.toVector());
                    if (diff.lengthSquared() < 1e-6) {
                        diff = new Vector(0, 0, 1);
                    }
                    Vector direction = diff.normalize();
                    camLoc.setDirection(direction);

                    stand.teleport(camLoc);
                    sendCamera(player, stand);
                }

                tick++;
            }
        }.runTaskTimer(OMCPlugin.getInstance(), 0L, 1L);

        info.setTask(task);
    }

    public static void stop(Player player) {
        PlayerAnimationInfo info = playingAnimations.remove(player);
        if (info == null) return;

        player.setInvulnerable(info.isOldInvulnerable());

        if (info.getAnimation() == Animation.JOIN_RIFT) {
            player.setWalkSpeed(info.getOldWalkSpeed());
            player.setFlySpeed(info.getOldFlySpeed());
        }

        sendCamera(player, player);
        if (info.getArmorStand() != null)
            info.getArmorStand().remove();

        if (info.getTask() != null)
            info.getTask().cancel();

        restoreHead(player, info);
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        PlayerAnimationInfo info = playingAnimations.get(event.getPlayer());
        if (info != null && info.getAnimation() == Animation.JOIN_RIFT) {
            stop(event.getPlayer());
        }
    }

    /** Keep the player anchored while the camera is detached; looking remains allowed. */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        PlayerAnimationInfo info = playingAnimations.get(event.getPlayer());
        if (info == null || info.getAnimation() != Animation.JOIN_RIFT || event.getTo() == null) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            to.setX(from.getX());
            to.setY(from.getY());
            to.setZ(from.getZ());
            event.setTo(to);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stop(event.getPlayer());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerAnimationInfo info = playingAnimations.get(player);
        if (info != null && info.getAnimation() == Animation.JOIN_RIFT) {
            stop(player);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        PlayerAnimationInfo info = playingAnimations.get(player);
        if (info != null && info.getAnimation() == Animation.JOIN_RIFT) {
            stop(player);
        }
    }

    /**
     * Sends a packet to the player to set the camera to the specified entity.
     *
     * @param player The player to send the packet to.
     * @param entity The entity to set the camera to.
     */
    public static void sendCamera(Player player, Entity entity) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.CAMERA);
        packet.getIntegers().write(0, entity.getEntityId());
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
    }

    public static void setupHead(Player player) {
        PlayerAnimationInfo info = playingAnimations.get(player);
        if (info == null) return;
        info.setOldRotations(new Float[]{player.getLocation().getYaw(), player.getLocation().getPitch()});

        Location loc = player.getLocation().clone();
        loc.setYaw(180f);
        loc.setPitch(0f);

        player.teleport(loc);
    }

    private static void restoreHead(Player player, PlayerAnimationInfo info) {
        Float[] rot = info.getOldRotations();
        if (rot == null) return;
        Location loc = player.getLocation().clone();
        loc.setYaw(rot[0]);
        loc.setPitch(rot[1]);
        player.teleport(loc);
    }
}
