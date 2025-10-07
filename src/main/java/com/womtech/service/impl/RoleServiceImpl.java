package com.womtech.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.womtech.entity.Role;
import com.womtech.repository.RoleRepository;
import com.womtech.service.RoleService;

@Service
public class RoleServiceImpl implements RoleService {
	@Autowired
	RoleRepository roleRepository;

	@Override
	public Optional<Role> findByRolename(String rolename) {
		return roleRepository.findByRolename(rolename);
	}

	@Override
	public <S extends Role> S save(S entity) {
		return roleRepository.save(entity);
	}

	@Override
	public Optional<Role> findById(String id) {
		return roleRepository.findById(id);
	}
}
