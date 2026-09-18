package com.flowforge.order.persistence;

import com.flowforge.order.domain.OutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEntity, UUID> {

    List<OutboxEntity> findTop20ByStatusOrderByCreatedAtAsc(OutboxEntity.PublishStatus status);
}
