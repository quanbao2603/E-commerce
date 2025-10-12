package com.womtech.service;

import com.womtech.entity.Inventory;
import com.womtech.entity.Product;
import com.womtech.entity.Location;

import java.util.List;
import java.util.Optional;

public interface InventoryService {

    List<Inventory> getAllInventory();

    Optional<Inventory> getInventoryByID(String id);

    Optional<Inventory> getInventoryByProductAndLocation(Product product, Location location);

    List<Inventory> getLowStockItems();

    List<Inventory> getOutOfStockItems();

    List<Inventory> getInventoryByStatus(Integer status);

    Inventory saveInventory(Inventory inventory);

    Inventory updateStock(String inventoryId, Integer quantity);

    Inventory restockInventory(String inventoryId, Integer quantity);

    void deleteInventory(String id);

    long getTotalCount();

    long getLowStockCount();

    long getOutOfStockCount();

    // Location methods
    List<Location> getAllLocations();

    List<Location> getActiveLocations();
}
