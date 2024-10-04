package de.bypixeltv.redivelocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import de.bypixeltv.redivelocity.config.Config;
import de.bypixeltv.redivelocity.managers.RedisController;
import de.bypixeltv.redivelocity.RediVelocity;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.minimessage.MiniMessage;

@Singleton
public class PostLoginListener {

    private final RediVelocity rediVelocity;
    private final Config config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final RedisController redisController;
    private final String proxyId;

    @Inject
    public PostLoginListener(RediVelocity rediVelocity, Config config) {
        this.rediVelocity = rediVelocity;
        this.config = config;
        this.redisController = rediVelocity.getRedisController();
        this.proxyId = rediVelocity.getProxyId();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        var player = event.getPlayer();

        if (config.getVersionControl().isEnabled()) {
            int playerProtocolVersion = player.getProtocolVersion().getProtocol();
            int requiredProtocolVersion = config.getVersionControl().getProtocolVersion();
            if (playerProtocolVersion < requiredProtocolVersion) {
                if (!player.hasPermission("redivelocity.admin.version.bypass")) {
                    player.disconnect(miniMessage.deserialize(config.getVersionControl().getKickMessage()));
                }
            }
        }

        var redisConfig = config.getRedis();
        redisController.sendPostLoginMessage(
                "postLogin",
                rediVelocity.getProxyId(),
                player.getUsername(),
                player.getUniqueId().toString(),
                player.getRemoteAddress().toString().split(":")[0].substring(1),
                redisConfig.getChannel()
        );

        if (redisController.getHashField("rv-players-blacklist", player.getUniqueId().toString()) != null) {
            redisController.setHashField("rv-players-blacklist", player.getUniqueId().toString(), player.getRemoteAddress().toString().split(":")[0].substring(1));
            player.disconnect(miniMessage.deserialize(config.getMessages().getKickMessage()));
            return;
        }

        redisController.setHashField("rv-players-proxy", player.getUniqueId().toString(), proxyId);
        var players = redisController.getHashField("rv-proxy-players", proxyId);
        if (players != null) {
            int playerCount = Integer.parseInt(players);
            if (playerCount <= 0) {
                redisController.setHashField("rv-proxy-players", proxyId, "1");
            } else {
                redisController.setHashField("rv-proxy-players", proxyId, String.valueOf(playerCount + 1));
            }
        } else {
            redisController.setHashField("rv-proxy-players", proxyId, "0");
        }

        var gplayers = redisController.getString("rv-global-playercount");
        if (gplayers != null) {
            int globalPlayerCount = Integer.parseInt(gplayers);
            if (globalPlayerCount <= 0) {
                redisController.setString("rv-global-playercount", "1");
            } else {
                redisController.setString("rv-global-playercount", String.valueOf(globalPlayerCount + 1));
            }
        } else {
            redisController.setString("rv-global-playercount", "0");
        }

        redisController.setHashField("rv-players-name", player.getUniqueId().toString(), player.getUsername());
        redisController.setHashField("rv-players-ip", player.getUniqueId().toString(), player.getRemoteAddress().toString().split(":")[0].substring(1));
    }
}