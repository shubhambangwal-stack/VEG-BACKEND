package com.veggofresh.delivery.repository;

import com.veggofresh.delivery.entity.DeliveryDocument;
import com.veggofresh.delivery.entity.DeliveryDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryDocumentRepository extends JpaRepository<DeliveryDocument, UUID> {
    List<DeliveryDocument> findByDeliveryPartnerUserId(UUID deliveryPartnerUserId);
    Optional<DeliveryDocument> findByDeliveryPartnerUserIdAndDocumentType(UUID deliveryPartnerUserId, DeliveryDocumentType documentType);
}
