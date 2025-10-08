package com.womtech.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.womtech.entity.Address;
import com.womtech.entity.User;

@Repository
public interface AddressRepository extends JpaRepository<Address, String> {
	Optional<Address> findByUserAndIsDefaultTrue(User user);
	
	List<Address> findByUser(User user);
}