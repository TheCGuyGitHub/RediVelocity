/*
 * Copyright (c) 2025.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.bypixel.redivelocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.bypixel.redivelocity.RediVelocity;
import dev.bypixel.redivelocity.RediVelocityLogger;
import dev.bypixel.redivelocity.config.Config;
import dev.bypixel.redivelocity.jedisWrapper.RedisController;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class PostLoginListener {

    private final Config config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final RedisController redisController;
    private final String proxyId;
    private final RediVelocity rediVelocity;
    private final ExecutorService redisExecutor = Executors.newFixedThreadPool(5);
    private final RediVelocityLogger logger;
    private final ProxyServer proxy;

    @Inject
    public PostLoginListener(RediVelocity rediVelocity, Config config, RedisController redisController, RediVelocityLogger logger, ProxyServer proxy) {
        this.config = config;
        this.redisController = redisController;
        this.proxyId = rediVelocity.getProxyId();
        this.rediVelocity = rediVelocity;
        this.logger = logger;
        this.proxy = proxy;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        var player = event.getPlayer();

        if (Objects.equals(redisController.getString("rv-init-process"), "true")) {
            player.disconnect(MiniMessage.miniMessage().deserialize("<red>Proxy is booting up, please wait..."));
            return;
        }

        if (config.getVersionControl().isEnabled()) {
            int playerProtocolVersion = player.getProtocolVersion().getProtocol();
            List<Integer> requiredProtocolVersions = config.getVersionControl().getAllowedVersions();
            if (!requiredProtocolVersions.contains(playerProtocolVersion)) {
                if (!player.hasPermission("redivelocity.admin.version.bypass")) {
                    player.disconnect(miniMessage.deserialize(config.getVersionControl().getKickMessage()));
                    return;
                }
            }
        }

        redisExecutor.submit(() -> {
            try {
                redisController.setHashField("rv-players-proxy", player.getUniqueId().toString(), proxyId);

                var redisConfig = config.getRedis();
                redisController.sendPostLoginMessage(
                        "postLogin",
                        rediVelocity.getProxyId(),
                        player.getUsername(),
                        player.getUniqueId().toString(),
                        player.getRemoteAddress().toString().split(":")[0].substring(1),
                        redisConfig.getChannel()
                );

                redisController.setHashField("rv-proxy-players", proxyId, proxy.getAllPlayers().size() + "");

                redisController.setHashField("rv-players-name", player.getUniqueId().toString(), player.getUsername());
                redisController.setHashField("rv-players-ip", player.getUniqueId().toString(), player.getRemoteAddress().toString().split(":")[0].substring(1));

                Map<String, String> proxyPlayers = redisController.getHashValuesAsPair("rv-players-proxy");
                int values = proxyPlayers.values().stream()
                        .filter(value -> value.equals(proxyId))
                        .toList()
                        .size();
                redisController.setHashField("rv-proxy-players", proxyId, String.valueOf(values));

                Map<String, String> proxyPlayersMap = redisController.getHashValuesAsPair("rv-players-name");
                int sum = proxyPlayersMap.size();
                redisController.setString("rv-global-playercount", String.valueOf(sum));
            } catch (Exception ex) {
                logger.sendErrorLogs("Error while sending post login Redis message " + ex.getMessage());
            }
        });
    }
}