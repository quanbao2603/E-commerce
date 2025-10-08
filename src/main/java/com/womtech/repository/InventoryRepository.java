package com.womtech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.womtech.entity.Inventory;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String> {

}