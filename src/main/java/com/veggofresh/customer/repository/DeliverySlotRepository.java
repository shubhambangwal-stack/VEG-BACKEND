package com.veggofresh.customer.repository;

import com.veggofresh.customer.entity.DeliverySlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DeliverySlotRepository extends JpaRepository<DeliverySlot, UUID> {
    List<DeliverySlot> findByDateOrderByStartTimeAsc(LocalDate date);
    List<DeliverySlot> findByDateInOrderByDateAscStartTimeAsc(List<LocalDate> dates);
}
