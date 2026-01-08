package com.blur.chatservice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.corundumstudio.socketio.SocketIOServer;

@Configuration
public class SocketIOConfig {

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();

        config.setHostname("0.0.0.0");
        config.setPort(8099);

        // ✅ FIX: Properly configure CORS for cross-port communication
        config.setOrigin("http://localhost");
        config.setOrigin("*");

        // ✅ Increase timeouts to prevent premature disconnects
        config.setPingTimeout(120000); // 2 minutes
        config.setPingInterval(30000); // 30 seconds

        // ✅ Allow protocol upgrade
        config.setUpgradeTimeout(10000);

        // ✅ Increase connection pool
        config.setMaxFramePayloadLength(1024 * 1024); // 1MB

        return new SocketIOServer(config);
    }
}
