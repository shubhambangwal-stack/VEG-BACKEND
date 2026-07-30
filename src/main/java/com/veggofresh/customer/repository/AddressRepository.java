package com.veggofresh.customer.repository;

import com.veggofresh.customer.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findByUserId(UUID userId);
    Optional<Address> findByIdAndUserId(UUID id, UUID userId);
    long countByUserId(UUID userId);
}
