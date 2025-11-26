package com.ecommerce.catalogservice.repository;

import com.ecommerce.catalogservice.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    @Query("SELECT i FROM Item i WHERE (:name IS NULL OR i.name LIKE %:name%) AND (:category IS NULL OR i.category = :category)")
    Page<Item> searchItems(@Param("name") String name, @Param("category") String category, Pageable pageable);
}