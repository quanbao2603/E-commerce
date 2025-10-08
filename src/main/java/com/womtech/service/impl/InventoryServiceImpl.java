package com.womtech.service.impl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import com.womtech.entity.Inventory;
import com.womtech.service.InventoryService;

@Service
public class InventoryServiceImpl extends BaseServiceImpl<Inventory, String> implements InventoryService {
	public InventoryServiceImpl(JpaRepository<Inventory, String> repo) {
		super(repo);
	}
}