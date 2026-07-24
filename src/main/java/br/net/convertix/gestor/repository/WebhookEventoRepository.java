package br.net.convertix.gestor.repository;

import br.net.convertix.gestor.entity.WebhookEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookEventoRepository extends JpaRepository<WebhookEvento, Long> {

    Optional<WebhookEvento> findByEventId(String eventId);

    boolean existsByEventId(String eventId);
}
