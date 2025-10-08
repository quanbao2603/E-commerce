package com.womtech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.womtech.entity.Voucher;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, String> {

}