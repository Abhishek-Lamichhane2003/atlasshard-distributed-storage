package com.atlasshard.storage.service;

import com.atlasshard.storage.model.AuditEvent;
import com.atlasshard.storage.repo.AuditEventRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.events.mode", havingValue = "kafka", matchIfMissing = true)
public class EventConsumer {
    private final AuditEventRepository repo;

    public EventConsumer(AuditEventRepository repo) {
        this.repo = repo;
    }

    @KafkaListener(topics = "storage-events", groupId = "atlasshard-workers")
    public void consume(String message) {
        String[] parts = message.split("\\|", 2);
        AuditEvent event = new AuditEvent();
        event.setType(parts[0]);
        event.setDetail(parts.length > 1 ? parts[1] : "");
        repo.save(event);
    }

    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void pruneOldEvents() {
        repo.deleteByCreatedAtBefore(Instant.now().minus(Duration.ofDays(30)));
    }
}
