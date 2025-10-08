package com.womtech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.womtech.entity.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {

}