package com.ecommerce.pricingservice.repository;

import com.ecommerce.pricingservice.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {

    Optional<PricingRule> findByItemIdAndStatus(String itemId, String status);

    List<PricingRule> findByItemId(String itemId);

    @Query("SELECT p FROM PricingRule p WHERE p.itemId = :itemId AND p.status = 'ACTIVE' " +
           "AND (p.validFrom IS NULL OR p.validFrom <= CURRENT_TIMESTAMP) " +
           "AND (p.validUntil IS NULL OR p.validUntil >= CURRENT_TIMESTAMP) " +
           "ORDER BY p.discountPercent DESC")
    List<PricingRule> findActivePricingRulesForItem(@Param("itemId") String itemId);

    @Query("SELECT p FROM PricingRule p WHERE p.itemId = :itemId AND p.status = 'ACTIVE' " +
           "AND (p.validFrom IS NULL OR p.validFrom <= CURRENT_TIMESTAMP) " +
           "AND (p.validUntil IS NULL OR p.validUntil >= CURRENT_TIMESTAMP) " +
           "AND (p.minQuantity IS NULL OR p.minQuantity <= :quantity) " +
           "ORDER BY p.discountPercent DESC")
    List<PricingRule> findBestPricingRuleForItem(@Param("itemId") String itemId, @Param("quantity") Integer quantity);

    List<PricingRule> findByStatus(String status);

    boolean existsByItemId(String itemId);
}

