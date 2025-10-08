package com.womtech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.womtech.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

}