package com.ecommerce.order.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderAuditRepository extends JpaRepository<OrderAuditLog, Long> {

    List<OrderAuditLog> findByOrderIdOrderByOccurredAtAsc(Long orderId);
}
