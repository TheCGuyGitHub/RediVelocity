package de.bypixeltv.redivelocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import de.bypixeltv.redivelocity.RediVelocity;
import de.bypixeltv.redivelocity.managers.RedisController;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class ProxyPingListener {

    private final RedisController redisController;

    @Inject
    public ProxyPingListener(RediVelocity rediVelocity) {
        this.redisController = rediVelocity.getRedisController();
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onProxyPing(ProxyPingEvent event) {
        String players = redisController.getString("rv-global-playercount");
        var ping = event.getPing().asBuilder();
        ping.onlinePlayers(players != null ? Integer.parseInt(players) : 0);
        event.setPing(ping.build());
    }
}