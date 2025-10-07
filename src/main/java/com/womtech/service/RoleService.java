package com.womtech.service;

import java.util.Optional;

import com.womtech.entity.Role;

public interface RoleService {

	Optional<Role> findById(String id);

	<S extends Role> S save(S entity);

	Optional<Role> findByRolenameIgnoreCase(String rolename);

}
