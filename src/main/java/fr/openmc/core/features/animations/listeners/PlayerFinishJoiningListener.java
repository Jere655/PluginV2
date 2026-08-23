package fr.openmc.core.features.animations.listeners;

import fr.openmc.core.OMCPlugin;
import fr.openmc.core.bootstrap.features.types.NotLoadInUnitTest;
import fr.openmc.core.features.animations.Animation;
import fr.openmc.core.features.settings.PlayerSettingsManager;
import fr.openmc.core.features.settings.SettingType;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

public class PlayerFinishJoiningListener implements Listener, NotLoadInUnitTest {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        boolean onGround = player.getLocation().subtract(0, 1, 0).getBlock().getType().isSolid();
        if (!(boolean) PlayerSettingsManager.getPlayerSettings(player.getUniqueId()).getSetting(SettingType.JOIN_ANIMATION)) return;
        if (player.isFlying() || !onGround || player.getGameMode().equals(GameMode.SPECTATOR)) return;

        player.setInvulnerable(true);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                try {
                    EmoteListener.play(player, Animation.JOIN_RIFT);
                } catch (Exception e) {
                    EmoteListener.stop(player);
                    OMCPlugin.getInstance().getLogger().log(Level.WARNING, "Failed to play join_rift animation for " + player.getName(), e);
                }
            }
        }.runTaskLater(OMCPlugin.getInstance(), 11L);
    }
}
