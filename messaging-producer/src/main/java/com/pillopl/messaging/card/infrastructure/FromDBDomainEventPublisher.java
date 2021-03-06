package com.pillopl.messaging.card.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pillopl.messaging.card.DomainEventsPublisher;
import com.pillopl.messaging.card.model.DomainEvent;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.annotation.Scheduled;

import javax.transaction.Transactional;

//@Primary
//@Component
public class FromDBDomainEventPublisher implements DomainEventsPublisher {

    private final DomainEventStorage domainEventStorage;
    private final ObjectMapper objectMapper;
    private final Source source;

    public FromDBDomainEventPublisher(DomainEventStorage domainEventStorage, ObjectMapper objectMapper, Source source) {
        this.domainEventStorage = domainEventStorage;
        this.objectMapper = objectMapper;
        this.source = source;
    }

    @Override
    public void publish(DomainEvent domainEvent) {
        try {
            domainEventStorage.save(new StoredDomainEvent(objectMapper.writeValueAsString(domainEvent)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Scheduled(fixedRate = 2000)
    @Transactional
    public void publishExternally() {
        domainEventStorage
                .findAllBySentOrderByTimestampDesc(false)
                .forEach(event -> {
                            source.output().send(new GenericMessage<>(event.getContent()));
                            event.sent();
                        }

                );
    }
}
