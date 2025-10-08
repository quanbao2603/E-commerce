package com.womtech.service.impl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import com.womtech.entity.Specification;
import com.womtech.service.SpecificationService;

@Service
public class SpecificationServiceImpl extends BaseServiceImpl<Specification, String> implements SpecificationService {
	public SpecificationServiceImpl(JpaRepository<Specification, String> repo) {
		super(repo);
	}
}