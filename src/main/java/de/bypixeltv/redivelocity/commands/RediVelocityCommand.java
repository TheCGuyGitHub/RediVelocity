package de.bypixeltv.redivelocity.commands;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import de.bypixeltv.redivelocity.config.ConfigLoader;
import de.bypixeltv.redivelocity.managers.RedisController;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Singleton
public class RediVelocityCommand {

    private final String prefix;
    private final MiniMessage miniMessage;
    private final RedisController redisController;
    private final ProxyServer proxy;

    @Inject
    public RediVelocityCommand(RedisController redisController, ProxyServer proxy) {
        ConfigLoader configLoader = new ConfigLoader("plugins/redivelocity/config.yml");
        configLoader.load();
        this.prefix = configLoader.getConfig().getMessages().getPrefix();
        this.miniMessage = MiniMessage.miniMessage();
        this.redisController = redisController;
        this.proxy = proxy;
    }

    public void register() {
        new CommandAPICommand("redivelocity")
                .withAliases("rv", "rediv", "redisvelocity", "redisv")
                .withSubcommands(
                        new CommandAPICommand("player")
                                .withSubcommands(
                                        new CommandAPICommand("proxy")
                                                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(_ -> redisController.getAllHashValues("rv-players-name"))))
                                                .withPermission("redivelocity.admin.player.proxy")
                                                .executes((sender, args) -> {
                                                    String playerName = (String) args.get(0);
                                                    String playerKey = redisController.getHashKeyByValue("rv-players-name", playerName);
                                                    if (playerKey == null) {
                                                        sender.sendMessage(miniMessage.deserialize(prefix + " <gray>The player <aqua>" + playerName + "</aqua> does not exist.</gray>"));
                                                        return;
                                                    }
                                                    String playerProxy = redisController.getHashField("rv-players-proxy", playerKey);
                                                    if (playerProxy != null) {
                                                        sender.sendMessage(miniMessage.deserialize(prefix + " <gray>The player <aqua>" + playerName + "</aqua> is connected to proxy: <aqua>" + playerProxy + "</aqua></gray>"));
                                                    } else {
                                                        sender.sendMessage(miniMessage.deserialize(prefix + " <gray>The player <aqua>" + playerName + "</aqua> is <red>offline</red>.</gray>"));
                                                    }
                                                }),
                                        new CommandAPICommand("lastseen")
                                                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> redisController.getAllHashValues("rv-players-name"))))
                                                .withPermission("redivelocity.admin.player.lastseen")
                                                .executes((sender, args) -> {
                                                    String playerName = (String) args.get(0);
                                                    String playerKey = redisController.getHashKeyByValue("rv-players-name", playerName);
                                                    if (playerKey == null) {
                                                        sender.sendMessage(miniMessage.deserialize(prefix + " <gray>The player <aqua>" + playerName + "</aqua> does not exist.</gray>"));
                                                        return;
                                                    }
                                                    String lastSeen = redisController.getHashField("rv-players-lastseen", playerKey);
                                                    boolean isOnline = redisController.getHashField("rv-players-name", playerKey).contains(playerName);
                                                    if (!isOnline) {
                                                        if (lastSeen != null) {
                                                            sender.sendMessage(miniMessage.deserialize(prefix + " <gray>The player <aqua>" + playerName + "</aqua> was last seen <aqua>" + lastSeen + "</aqua>.</gray>"));
                                                        } else {
                                                            sender.sendMessage(miniMessage.deserialize(prefix + " <gray>The player <aqua>" + playerName + "</aqua> was never seen before.</gray>"));
                                                        }
                                                    } else {
                                                        sender.sendMessage(miniMessage.deserialize(prefix + " <gray>The player <aqua>" + playerName + "</aqua> is currently <green>online</green>.</gray>"));
                                                    }
                                                }),
                                        new CommandAPICommand("ip")
                                                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> redisController.getAllHashValues("rv-players-name"))))
                                                .withPermission("redivelocity.admin.player.ip")
                                                .executes((sender, args) -> {
                                                    String playerName = (String) args.get(0);
                                                    String playerKey = redisController.getHashKeyByValue("rv-players-name", playerName);
                                                    if (playerKey == null) {
                                                        sender.sendMessage(miniMessage.deserialize(prefix + " <gray>The player <aqua>" + playerName + "</aqua> does not exist.</gray>"));
                                                        return;
                                                    }
                                                    String playerIp = redisController.getHashField("rv-players-ip", playerKey);
                                                    if (playerIp != null) {
                                                        sender.sendMessage(miniMessage.deserialize(prefix + " <gray>The player <aqua>" + playerName + "</aqua> is connected with IP: <aqua><hover:show_text:'<aqua>Click to copy</aqua>'><click:copy_to_clipboard:" + playerIp + ">" + playerIp + "</click></hover></aqua></gray>"));
                                                    } else {
                                                        sender.sendMessage(miniMessage.deserialize(prefix + " <gray>The player <aqua>" + playerName + "</aqua> is <red>offline</red>.</gray>"));
                                                    }
                                                }),
                                        new CommandAPICommand("uuid")
                                                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> redisController.getAllHashValues("rv-players-name"))))
                                                .withPermission("redivelocity.admin.player.uuid")
                                                .executes((sender, args) -> {
                                                    String playerName = (String) args.get(0);
                                                    String playerUuid = redisController.getHashKeyByValue("rv-players-name", playerName);
                                                    if (playerUuid != null) {
                                                        sender.sendMessage(miniMessage.deserialize(prefix + " <gray>The player <aqua>" + playerName + "</aqua> has the UUID: <aqua><hover:show_text:'<aqua>Click to copy</aqua>'><click:copy_to_clipboard:" + playerUuid + ">" + playerUuid + "</click></hover></aqua></gray>"));
                                                    } else {
                                                        sender.sendMessage(miniMessage.deserialize(prefix + " <gray>The player <aqua>" + playerName + "</aqua> does not exist.</gray>"));
                                                    }
                                                }),
                                        new CommandAPICommand("server")
                                                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> redisController.getAllHashValues("rv-players-name"))))
                                                .withPermission("redivelocity.admin.player.uuid")
                                                .executes((sender, args) -> {
                                                    String playerName = (String) args.get(0);
                                                    String playerKey = redisController.getHashKeyByValue("rv-players-name", playerName);
                                                    if (playerKey == null) {
                                                        sender.sendMessage(miniMessage.deserialize(prefix + " <gray>The player <aqua>" + playerName + "</aqua> does not exist.</gray>"));
                                                        return;
                                                    }
                                                    String playerServer = redisController.getHashField("rv-players-server", playerKey);
                                                    if (playerServer != null) {
                                                        sender.sendMessage(miniMessage.deserialize(prefix + " <gray>The player <aqua>" + playerName + "</aqua> is currently on server: <aqua>" + playerServer + "</aqua></gray>"));
                                                    } else {
                                                        sender.sendMessage(miniMessage.deserialize(prefix + " <gray>The player <aqua>" + playerName + "</a> is <red>offline</red>.</gray>"));
                                                    }
                                                })
                                ),
                        new CommandAPICommand("proxy")
                                .withSubcommands(
                                        new CommandAPICommand("list")
                                                .withPermission("redivelocity.admin.proxy.list")
                                                .executes((sender, args) -> {
                                                    Set<String> proxies = redisController.getAllHashFields("rv-proxies");
                                                    List<String> proxiesPrettyNames = proxies.stream()
                                                            .map(proxyId -> prefix + " <aqua>" + proxyId + "</aqua> <dark_grey>(<grey>Players: </grey><aqua>" + redisController.getHashField("rv-proxy-players", proxyId) + "</aqua>)</dark_grey>")
                                                            .collect(Collectors.toList());
                                                    String proxiesPrettyString = String.join("<br>", proxiesPrettyNames);
                                                    if (proxies.isEmpty()) {
                                                        sender.sendMessage(miniMessage.deserialize(prefix + " <gray>There are currently no connected proxies.</gray>"));
                                                    } else {
                                                        sender.sendMessage(miniMessage.deserialize(prefix + " <gray>Currently connected proxies:<br>" + proxiesPrettyString + "</gray>"));
                                                    }
                                                }),
                                        new CommandAPICommand("players")
                                                .withOptionalArguments(new StringArgument("proxy").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> redisController.getAllHashFields("rv-proxies"))))
                                                .withPermission("redivelocity.admin.proxy.players")
                                                .executes((sender, args) -> {
                                                    String proxyId = (String) args.getOptional(0).orElse(null);
                                                    List<String> players = redisController.getAllHashValues("rv-players-name");
                                                    List<String> playersPrettyNames = players.stream()
                                                            .map(player -> {
                                                                String playerProxy = redisController.getHashField("rv-players-proxy", redisController.getHashKeyByValue("rv-players-name", player));
                                                                if (proxyId == null) {
                                                                    return prefix + " <aqua>" + player + "</aqua> <dark_gray>(<aqua>" + playerProxy + "</aqua>)</dark_gray>";
                                                                } else if (playerProxy.equals(proxyId)) {
                                                                    return prefix + " <aqua>" + player + "</aqua>";
                                                                }
                                                                return null;
                                                            })
                                                            .filter(Objects::nonNull)
                                                            .collect(Collectors.toList());
                                                    String playersPrettyString = String.join("<br>", playersPrettyNames);
                                                    if (playersPrettyNames.isEmpty()) {
                                                        if (proxyId == null) {
                                                            sender.sendMessage(miniMessage.deserialize(prefix + " <gray>There are currently no players online.</gray>"));
                                                        } else {
                                                            sender.sendMessage(miniMessage.deserialize(prefix + " <gray>There are currently no players online on proxy <aqua>" + proxyId + "</aqua>.</gray>"));
                                                        }
                                                    } else {
                                                        if (proxyId != null) {
                                                            sender.sendMessage(miniMessage.deserialize(prefix + " <gray>Currently online players on proxy <aqua>" + proxyId + "</aqua>:<br>" + playersPrettyString + "</gray>"));
                                                        } else {
                                                            sender.sendMessage(miniMessage.deserialize(prefix + " <gray>Currently online players:<br>" + playersPrettyString + "</gray>"));
                                                        }
                                                    }
                                                }),
                                        new CommandAPICommand("playercount")
                                                .withOptionalArguments(new StringArgument("proxy").replaceSuggestions(ArgumentSuggestions.stringCollection(info -> redisController.getAllHashFields("rv-proxies"))))
                                                .withPermission("redivelocity.admin.proxy.playercount")
                                                .executes((sender, args) -> {
                                                    String proxyId = (String) args.getOptional(0).orElse(null);
                                                    if (proxyId == null) {
                                                        String playerCount = redisController.getString("rv-global-playercount");
                                                        if (playerCount != null) {
                                                            sender.sendMessage(miniMessage.deserialize(prefix + " <gray>There are currently <aqua>" + playerCount + "</aqua> players online.</gray>"));
                                                        } else {
                                                            sender.sendMessage(miniMessage.deserialize(prefix + " <gray>There are currently no players online.</gray>"));
                                                        }
                                                    } else {
                                                        String playerCount = redisController.getHashField("rv-proxy-players", proxyId);
                                                        if (playerCount != null) {
                                                            sender.sendMessage(miniMessage.deserialize(prefix + " <gray>There are currently <aqua>" + playerCount + "</aqua> players online on proxy <aqua>" + proxyId + "</aqua>.</gray>"));
                                                        } else {
                                                            sender.sendMessage(miniMessage.deserialize(prefix + " <gray>There are currently no players online on proxy <aqua>" + proxyId + "</aqua>.</gray>"));
                                                        }
                                                    }
                                                }),
                                        new CommandAPICommand("servers")
                                                .withPermission("redivelocity.admin.proxy.servers")
                                                .executes((sender, args) -> {
                                                    List<CompletableFuture<String>> futures = proxy.getAllServers().stream()
                                                            .map(server -> CompletableFuture.supplyAsync(() -> {
                                                                try {
                                                                    ServerPing result = server.ping().get();
                                                                    return prefix + " <color:#0dbf00>●</color> <aqua>" + server.getServerInfo().getName() + "</aqua> <dark_gray>(<grey>Address: <aqua>" + server.getServerInfo().getAddress() + "</aqua>, Playercount: <aqua>" + server.getPlayersConnected().size() + "</aqua>, Version: <aqua>" + result.getVersion().getProtocol() + ", " + result.getVersion().getName() + "</aqua></grey>)</dark_gray>";
                                                                } catch (Exception e) {
                                                                    return prefix + " <color:#f00000>●</color> <aqua>" + server.getServerInfo().getName() + "</aqua> <dark_gray>(<grey>Address: <aqua>" + server.getServerInfo().getAddress() + "</aqua></grey>)</dark_gray>";
                                                                }
                                                            }))
                                                            .toList();
                                                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
                                                        List<String> proxyRegisteredServersPrettyNames = futures.stream()
                                                                .map(CompletableFuture::join)
                                                                .collect(Collectors.toList());
                                                        String proxyRegisteredServersPrettyString = String.join("<br>", proxyRegisteredServersPrettyNames);
                                                        if (!proxyRegisteredServersPrettyNames.isEmpty()) {
                                                            sender.sendMessage(miniMessage.deserialize(prefix + " <gray>Currently registered servers:<br>" + proxyRegisteredServersPrettyString + "</gray>"));
                                                        } else {
                                                            sender.sendMessage(miniMessage.deserialize(prefix + " <gray>There are currently no registered servers.</gray>"));
                                                        }
                                                    });
                                                })
                                )
                ).register();
    }
}