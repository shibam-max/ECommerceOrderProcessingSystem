package com.ecommerce.order.repository;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(OrderStatus status);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    long countByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o")
    BigDecimal sumTotalAmount();

    @Query("SELECT COALESCE(AVG(o.totalAmount), 0) FROM Order o")
    Double averageTotalAmount();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status")
    BigDecimal sumTotalAmountByStatus(@Param("status") OrderStatus status);

    /**
     * Bulk-update all PENDING orders to PROCESSING in a single SQL statement.
     * Returns the number of rows affected.
     */
    @Modifying
    @Query("UPDATE Order o SET o.status = 'PROCESSING', o.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE o.status = 'PENDING'")
    int bulkUpdatePendingToProcessing();

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    java.util.Optional<Order> findByIdWithItems(@Param("id") Long id);
}
