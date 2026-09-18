package com.flowforge.order.persistence;

import com.flowforge.order.domain.OrderTimelineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderTimelineRepository extends JpaRepository<OrderTimelineEntity, Long> {

    List<OrderTimelineEntity> findByOrderIdOrderByOccurredAtAscIdAsc(UUID orderId);
}
