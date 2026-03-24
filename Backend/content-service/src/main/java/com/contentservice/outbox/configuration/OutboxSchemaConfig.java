package com.contentservice.outbox.configuration;

import org.neo4j.driver.Driver;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OutboxSchemaConfig {

  @Bean
  ApplicationRunner outboxSchemaInitializer(Driver driver) {
    return args -> {
      try (var session = driver.session()) {
        session.run(
            "CREATE INDEX outbox_event_published IF NOT EXISTS FOR (e:outbox_event) ON (e.published)")
            .consume();
        session.run(
            "CREATE INDEX outbox_event_createdAt IF NOT EXISTS FOR (e:outbox_event) ON (e.createdAt)")
            .consume();
        session.run(
            "CREATE INDEX outbox_event_retryCount IF NOT EXISTS FOR (e:outbox_event) ON (e.retryCount)")
            .consume();
        session.run(
            "CREATE INDEX outbox_event_publishedAt IF NOT EXISTS FOR (e:outbox_event) ON (e.publishedAt)")
            .consume();
      }
    };
  }
}
