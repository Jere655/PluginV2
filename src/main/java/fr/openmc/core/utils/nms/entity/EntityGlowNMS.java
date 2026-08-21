package fr.openmc.core.utils.nms.entity;

import fr.openmc.core.bootstrap.features.types.NotLoadInUnitTest;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.ChatFormatting;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EntityGlowNMS implements Listener, NotLoadInUnitTest {
    private static final Scoreboard NMS_SCOREBOARD = new Scoreboard();
    private static final Map<ChatFormatting, PlayerTeam> TEAM_MAP = new HashMap<>();
    private static final Map<UUID, ChatFormatting> entitiesGlowing = new ConcurrentHashMap<>();


    public EntityGlowNMS() {
        for (ChatFormatting color : ChatFormatting.values()) {
            PlayerTeam team = new PlayerTeam(NMS_SCOREBOARD, "omc_glow_" + color.getSerializedName());

            team.setColor(color);
            team.setSeeFriendlyInvisibles(false);
            team.setNameTagVisibility(PlayerTeam.Visibility.NEVER);

            TEAM_MAP.put(color, team);
        }
    }

    public static void setGlowingColor(Entity entity, ChatFormatting color) {
        ChatFormatting previous = entitiesGlowing.get(entity.getUniqueId());
        if (previous != null && previous != color) {
            PlayerTeam previousTeam = TEAM_MAP.get(previous);
            sendPacket(ClientboundSetPlayerTeamPacket.createPlayerPacket(
                    previousTeam, entity.getUniqueId().toString(), ClientboundSetPlayerTeamPacket.Action.REMOVE
            ));
        } else if (previous == color) {
            entity.setGlowing(true);
            return;
        }

        entity.setGlowing(true);

        PlayerTeam team = TEAM_MAP.get(color);
        String entry = entity.getUniqueId().toString();

        ClientboundSetPlayerTeamPacket addEntityPacket = ClientboundSetPlayerTeamPacket.createPlayerPacket(
                        team,
                        entry,
                        ClientboundSetPlayerTeamPacket.Action.ADD
        );

        sendPacket(addEntityPacket);
        entitiesGlowing.put(entity.getUniqueId(), color);
    }

    public static void removeGlowing(Entity entity) {
        entity.setGlowing(false);
        ChatFormatting current = entitiesGlowing.remove(entity.getUniqueId());

        if (current == null) return;

        PlayerTeam team = TEAM_MAP.get(current);
        String entry = entity.getUniqueId().toString();

        ClientboundSetPlayerTeamPacket removeEntityPacket = ClientboundSetPlayerTeamPacket.createPlayerPacket(
                team,
                entry,
                ClientboundSetPlayerTeamPacket.Action.REMOVE
        );

        sendPacket(removeEntityPacket);
    }

    private static void sendPacket(ClientboundSetPlayerTeamPacket packet) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ((CraftPlayer) player).getHandle().connection.send(packet);
        }
    }

    private static void sendPacketTo(Player player, ClientboundSetPlayerTeamPacket packet) {
        ((CraftPlayer) player).getHandle().connection.send(packet);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();
        for (ChatFormatting color : ChatFormatting.values()) {
            PlayerTeam team = TEAM_MAP.get(color);
            if (team == null) continue;

            sendPacketTo(joined, ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));
        }

        for (Map.Entry<UUID, ChatFormatting> entry : entitiesGlowing.entrySet()) {
            PlayerTeam team = TEAM_MAP.get(entry.getValue());
            if (team == null) continue;
            sendPacketTo(joined, ClientboundSetPlayerTeamPacket.createPlayerPacket(
                    team, entry.getKey().toString(), ClientboundSetPlayerTeamPacket.Action.ADD
            ));
        }
    }
}
